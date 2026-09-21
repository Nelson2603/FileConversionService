package org.example.fileconversionservice.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Set;

@Component
public class ImageToPdfConverter implements FileConverter {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    @Override
    public boolean supports(String extension) {
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public byte[] convertToPdf(byte[] sourceData) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDImageXObject image = PDImageXObject.createFromByteArray(
                    document, sourceData, "image"
            );

            // Рассчитываем размер страницы под изображение с отступами
            float margin = 30; // отступы в пунктах
            float pageWidth = Math.max(image.getWidth() + margin * 2, PDRectangle.A4.getWidth());
            float pageHeight = Math.max(image.getHeight() + margin * 2, PDRectangle.A4.getHeight());

            PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
            document.addPage(page);

            // Масштабируем и центрируем
            float scale = Math.min(
                    (pageWidth - margin * 2) / image.getWidth(),
                    (pageHeight - margin * 2) / image.getHeight()
            );

            float scaledWidth = image.getWidth() * scale;
            float scaledHeight = image.getHeight() * scale;
            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page)) {
                contentStream.drawImage(image, x, y, scaledWidth, scaledHeight);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}