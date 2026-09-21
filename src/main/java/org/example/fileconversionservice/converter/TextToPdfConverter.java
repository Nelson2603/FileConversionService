package org.example.fileconversionservice.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextToPdfConverter implements FileConverter {

    private static final float MARGIN = 50f;
    private static final float FONT_SIZE = 12f;
    private static final float LINE_HEIGHT = 14f;

    // Шрифт создаем один раз (кэшируем) для производительности
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    @Override
    public boolean supports(String extension) {
        return "txt".equalsIgnoreCase(extension);
    }

    @Override
    public byte[] convertToPdf(byte[] sourceData) throws Exception {
        String text = new String(sourceData, StandardCharsets.UTF_8);
        String[] lines = text.split("\\R"); // \\R - универсальный перенос строк

        try (PDDocument document = new PDDocument()) {
            PDPage page = createPage(document);
            PDPageContentStream contentStream = createContentStream(document, page);

            float y = page.getMediaBox().getHeight() - MARGIN;
            float maxWidth = page.getMediaBox().getWidth() - (2 * MARGIN);

            for (String line : lines) {
                // Разбиваем длинные строки на части
                List<String> wrappedLines = wrapLine(line, maxWidth);

                for (String wrappedLine : wrappedLines) {
                    // Если место закончилось - создаем новую страницу
                    if (y <= MARGIN) {
                        contentStream.close();
                        page = createPage(document);
                        contentStream = createContentStream(document, page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                    }

                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN, y);
                    contentStream.showText(wrappedLine);
                    contentStream.endText();
                    y -= LINE_HEIGHT;
                }
            }

            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private PDPage createPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    private PDPageContentStream createContentStream(PDDocument document, PDPage page) throws Exception {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(FONT, FONT_SIZE);
        return contentStream;
    }

    private List<String> wrapLine(String line, float maxWidth) throws Exception {
        if (line.isEmpty()) {
            return List.of("");
        }

        List<String> result = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : line.split(" ")) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;

            // Проверяем, влезает ли строка
            float width = FONT.getStringWidth(testLine) / 1000 * FONT_SIZE;

            if (width > maxWidth && !currentLine.isEmpty()) {
                // Если не влезает - сохраняем текущую строку и начинаем новую
                result.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // Добавляем последнюю строку
        if (!currentLine.isEmpty()) {
            result.add(currentLine.toString());
        }

        return result;
    }
}