package org.example.fileconversionservice.converter;

public interface FileConverter {
    //проверка поддерживает ли этот конвертер расширения файла
    boolean supports(String extension);

    // Сама логика конвертации: принимает байты файла, возвращает байты PDF
    byte[]convertToPdf(byte[]sourceData) throws Exception;
}
