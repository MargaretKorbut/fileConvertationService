package com.example.file_converter.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageConverter implements Converter {

    @Override
    public boolean supports(FileType type) {
        return type == FileType.PNG || type == FileType.JPG;
    }

    @Override
    public byte[] toPdf(String fileName, byte[] content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, content, fileName);

            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            doc.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}