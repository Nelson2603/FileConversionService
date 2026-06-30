package org.example.fileconversionservice.converter;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversionManager {

    private final List<FileConverter> converters;

    public ConversionManager(List<FileConverter> converters) {
        this.converters = converters;
    }

    public byte[] convert(String fileName, byte[] data) throws Exception {
        String extension = getFileExtension(fileName);

        return converters.stream()
                .filter(converter -> converter.supports(extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported file format: " + extension))
                .convertToPdf(data);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}