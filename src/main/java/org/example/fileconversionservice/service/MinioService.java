package org.example.fileconversionservice.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.fileconversionservice.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

import java.io.InputStream;


@Service
@RequiredArgsConstructor
@Slf4j
public class  MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final String OUTPUT_FOLDER = "output/";

    /**
     * Скачивает файл из MinIO и возвращает его содержимое
     */
    public byte[] downloadFile(String filePath) throws StorageException {
        log.info("📥 Скачивание файла из MinIO: {}/{}", bucketName, filePath);

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filePath)
                        .build())) {

            byte[] content = stream.readAllBytes();
            log.info("✅ Файл скачан: {} ({} байт)", filePath, content.length);
            return content;

        } catch (Exception e) {
            log.error("❌ Ошибка скачивания файла: {}", filePath, e);
            throw new StorageException("Не удалось скачать файл: " + filePath, e);
        }
    }

    /**
     * Загружает файл в MinIO с автоматическим формированием пути
     * @return путь к загруженному файлу
     */
    public String uploadFile(byte[] data, String originalFileName, String contentType) throws StorageException {
        String outputPath = buildOutputPath(originalFileName);
        return uploadFileToPath(data, outputPath, contentType);
    }

    /**
     * Загружает файл в MinIO по указанному пути
     *
     * @return путь к загруженному файлу
     */
    public String uploadFileToPath(byte[] data, String objectPath, String contentType) throws StorageException {
        log.info("📤 Загрузка файла в MinIO: {}/{}", bucketName, objectPath);

        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("✅ Файл загружен: {} ({} байт)", objectPath, data.length);
            return objectPath;

        } catch (Exception e) {
            log.error("❌ Ошибка загрузки файла: {}", objectPath, e);
            throw new StorageException("Не удалось загрузить файл: " + objectPath, e);
        }
    }

    /**
     * Формирует путь для сохранения PDF (в папку output/)
     */
    private String buildOutputPath(String originalFileName) {
        String fileNameWithoutExt = getFileNameWithoutExtension(originalFileName);
        return OUTPUT_FOLDER + fileNameWithoutExt + ".pdf";
    }

    private String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}