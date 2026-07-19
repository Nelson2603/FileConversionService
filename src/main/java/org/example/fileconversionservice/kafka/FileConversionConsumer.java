package org.example.fileconversionservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fileconversionservice.converter.ConversionManager;

import org.example.fileconversionservice.dto.FileConversionRequest;
import org.example.fileconversionservice.dto.FileConversionResponse;
import org.example.fileconversionservice.exception.ConversionException;
import org.example.fileconversionservice.exception.StorageException;
import org.example.fileconversionservice.service.IdempotencyService;
import org.example.fileconversionservice.service.MinioService;
import org.example.fileconversionservice.service.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileConversionConsumer {

    private final IdempotencyService idempotencyService;
    private final MinioService minioService;
    private final ConversionManager conversionManager;

    private final OutboxService outboxService;


    private final ObjectMapper objectMapper; // Для парсинга JSON

    @Value("${app.kafka.output-topic}")
    private String outputTopic;

    /**
     * Слушаем входящий топик Kafka
     */
    @KafkaListener(topics = "${app.kafka.input-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional

    public void listen(String message) {
        log.info("Received message from Kafka: {}", message);

        FileConversionRequest request;
        try {
            // Превращаем JSON строку в Java объект
            request = objectMapper.readValue(message, FileConversionRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON message: {}", message, e);
            return; // Если JSON битый, просто игнорируем, чтобы не зациклить ошибку
        }

        // 1. ПРОВЕРКА ИДЕМПОТЕНТНОСТИ (Transactional Inbox)
        // Если такое сообщение уже было обработано, tryToAcquireLock вернет false
        if (!idempotencyService.tryToAcquireLock(request.messageId())) {
            log.warn("Duplicate message detected. Skipping processing for ID: {}", request.messageId());
            return;
        }

        // 2. БИЗНЕС-ЛОГИКА
        try {
            // Получаем имя файла из пути (например, из "input/photo.png" берем "photo.png")
            String fileName = Paths.get(request.filePath()).getFileName().toString();

            // Скачиваем файл из MinIO
            log.info("Downloading file: {}", request.filePath());
            byte[] sourceData = minioService.downloadFile(request.filePath());

            // Конвертируем в PDF (тут сработает нужный Strategy: Image, Text или Zip)
            log.info("Converting file: {}", fileName);
            byte[] pdfData = conversionManager.convert(fileName, sourceData);

            // Загружаем PDF обратно в MinIO
            log.info("Uploading converted PDF to MinIO");
            String newPath = minioService.uploadFile(pdfData, fileName, "application/pdf");

            // 3. СОХРАНЕНИЕ РЕЗУЛЬТАТА (АУТБОКС)
            FileConversionResponse response = new FileConversionResponse(
                    request.messageId(), // Сохраняем тот же ID для связи запроса и ответа
                    newPath
            );

            String jsonResponse = objectMapper.writeValueAsString(response);
            outboxService.saveMessage(
                    request.messageId(),
                    jsonResponse,
                    outputTopic
            );

            log.info("Successfully processed and sent result for message ID: {}", request.messageId());

        } catch (StorageException e) {
            log.error("Storage exception for message ID {}: {}", request.messageId(), e.getMessage());


        } catch (ConversionException e) {

            log.error("Conversion exception for message ID {}: {}", request.messageId(), e.getMessage());
        }


        catch(Exception e){
            // 4. ОБРАБОТКА ОШИБОК И ОТКАТ
            log.error("Error during file conversion for message ID: {}", request.messageId(), e);


            // Пробрасываем исключение. Spring Kafka не отправит ACK,
            // и сообщение останется в топике для повторного получения.
            throw new ConversionException("Conversion failed for message " + request.messageId(), e);
        }
    }
}