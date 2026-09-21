package org.example.fileconversionservice.dto;

public record FileUploadedEvent(
        String fileId,
        String fileName,
        String originalFileName,
        String minioPath,
        long fileSize,
        String contentType,
        String timestamp
) {}