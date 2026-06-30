package org.example.fileconversionservice.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class ZipToPdfConverter implements FileConverter {

    private final List<FileConverter> converters;

    public ZipToPdfConverter(List<FileConverter> converters) {
        this.converters = converters;
        log.info("✅ ZipToPdfConverter зарегистрирован! Поддерживает: zip");
    }

    @Override
    public boolean supports(String extension) {
        return "zip".equalsIgnoreCase(extension);
    }

    @Override
    public byte[] convertToPdf(byte[] sourceData) throws Exception {
        log.info("🔄 Начало конвертации ZIP архива...");

        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
        merger.setDestinationStream(mergedOutput);

        boolean hasConvertedEntries = false;
        int convertedCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(sourceData))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    log.debug("📁 Пропускаем папку: {}", entry.getName());
                    continue;
                }

                String fileName = entry.getName();
                String extension = getFileExtension(fileName);
                log.debug("📄 Файл в ZIP: {}, расширение: {}", fileName, extension);

                // Пропускаем файлы без расширения
                if (extension.isEmpty()) {
                    log.warn("⚠️ Пропускаем файл без расширения: {}", fileName);
                    continue;
                }

                // Проверяем вложенный ZIP
                if ("zip".equalsIgnoreCase(extension)) {
                    log.warn("⚠️ Вложенный ZIP архив не поддерживается: {}", fileName);
                    continue; // Или можно выбросить исключение
                }

                byte[] fileData = zis.readAllBytes();

                // Ищем конвертер для этого файла
                FileConverter converter = converters.stream()
                        .filter(c -> c.supports(extension))
                        .findFirst()
                        .orElse(null);

                if (converter == null) {
                    log.warn("⚠️ Нет конвертера для: {}", extension);
                    continue;
                }

                log.info("🔍 Конвертируем: {} через {}", fileName, converter.getClass().getSimpleName());

                // Конвертируем в PDF
                byte[] pdfData = converter.convertToPdf(fileData);

                // Добавляем в объединенный PDF
                merger.addSource(RandomAccessReadBuffer.createBufferFromStream(
                        new ByteArrayInputStream(pdfData)
                ));

                hasConvertedEntries = true;
                convertedCount++;
                log.info("✅ {} сконвертирован", fileName);
            }
        }

        if (!hasConvertedEntries) {
            throw new IllegalArgumentException(
                    "ZIP архив не содержит поддерживаемых файлов"
            );
        }

        log.info("🔄 Объединение {} PDF файлов...", convertedCount);
        merger.mergeDocuments(null);

        byte[] result = mergedOutput.toByteArray();
        log.info("✅ ZIP конвертирован в PDF! Размер: {} байт", result.length);

        return result;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}