package org.example.fileconversionservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fileconversionservice.converter.ConversionManager;
import org.example.fileconversionservice.dto.FileConversionResponse;
import org.example.fileconversionservice.dto.FileUploadedEvent;
import org.example.fileconversionservice.exception.ConversionException;
import org.example.fileconversionservice.exception.StorageException;
import org.example.fileconversionservice.service.IdempotencyService;
import org.example.fileconversionservice.service.MinioService;
import org.example.fileconversionservice.service.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class FileConversionConsumerFromFlowManager {

    private final IdempotencyService idempotencyService;
    private final MinioService minioService;
    private final ConversionManager conversionManager;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.output-topic}")      // топик для результата, например "file-converted"
    private String outputTopic;

    /**
     * Слушаем топик "to-convert" от NewFlowManagerService
     */
    @KafkaListener(topics = "to-convert", groupId = "flow-manager-converter")
    @Transactional
    public void listen(String message) {
        log.info("📩 Получено событие из топика to-convert: {}", message);

        FileUploadedEvent event;
        try {
            event = objectMapper.readValue(message, FileUploadedEvent.class);
        } catch (Exception e) {
            log.error(" Ошибка парсинга JSON: {}", message, e);
            return; // битый JSON – пропускаем, чтобы не зациклить
        }

        String fileId = event.fileId();

        // 1. ИДЕМПОТЕНТНОСТЬ по fileId (используем как уникальный ключ)
        if (!idempotencyService.tryToAcquireLock(fileId)) {
            log.warn(" Дублирующее сообщение для fileId = {}. Пропускаем.", fileId);
            return;
        }

        try {
            // 2. Скачиваем файл из MinIO
            String minioPath = event.minioPath();
            log.info(" Скачивание файла: {}", minioPath);
            byte[] sourceData = minioService.downloadFile(minioPath);

            // 3. Конвертация (используем оригинальное имя для определения типа)
            String originalFileName = event.originalFileName();
            log.info(" Конвертация файла: {}", originalFileName);
            byte[] pdfData = conversionManager.convert(originalFileName, sourceData);

            // 4. Загружаем результат в MinIO (например, в тот же бакет с префиксом "converted/")
            String convertedPath = "converted/" + minioPath;
            log.info(" Загрузка сконвертированного файла: {}", convertedPath);
            minioService.uploadFile(pdfData, convertedPath, "application/pdf");

            // 5. Формируем ответное событие и сохраняем в Outbox
            FileConversionResponse response = new FileConversionResponse(
                    fileId,          // сохраняем fileId как идентификатор
                    convertedPath
            );
            String jsonResponse = objectMapper.writeValueAsString(response);
            outboxService.saveMessage(fileId, jsonResponse, outputTopic);

            log.info("✅ Конвертация успешно завершена для fileId = {}", fileId);

        } catch (StorageException e) {
            log.error(" Ошибка хранилища для fileId {}: {}", fileId, e.getMessage());
            // Здесь можно пометить как ошибку и не бросать исключение,
            // чтобы не повторять бесконечно. Можно сохранить в отдельную таблицу ошибок.
        } catch (ConversionException e) {
            log.error(" Ошибка конвертации для fileId {}: {}", fileId, e.getMessage());
            // Аналогично – логируем, но не выбрасываем, чтобы не зациклить.
        } catch (Exception e) {
            log.error(" Непредвиденная ошибка для fileId {}: {}", fileId, e.getMessage(), e);
            // Для непредвиденных ошибок лучше бросить исключение, чтобы Kafka повторила попытку.
            // Но с учётом идемпотентности, при повторной попытке lock уже будет занят, поэтому сообщение не будет обработано дважды.
            throw new ConversionException("Conversion failed for file " + fileId, e);
        }
    }
}