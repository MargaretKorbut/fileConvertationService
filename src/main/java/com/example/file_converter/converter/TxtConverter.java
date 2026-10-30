package com.example.file_converter.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TxtConverter implements Converter {

    private static final float FONT_SIZE = 12f;
    private static final float LEADING = 16f;   // расстояние между строками
    private static final float MARGIN = 50f;    // поля страницы

    @Override
    public boolean supports(FileType type) {
        return type == FileType.TXT;
    }

    @Override
    public byte[] toPdf(String fileName, byte[] content) throws IOException {
        String text = new String(content, StandardCharsets.UTF_8);

        try (PDDocument doc = new PDDocument()) {
            PDFont font;
            try (InputStream is = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
                font = PDType0Font.load(doc, is);
            }

            PDRectangle pageSize = PDRectangle.A4;
            float maxWidth = pageSize.getWidth() - 2 * MARGIN;
            List<String> lines = wrap(text, font, maxWidth);

            PDPageContentStream stream = null;
            float y = 0;
            for (String line : lines) {
                // нужна новая страница: первая строка или место кончилось
                if (stream == null || y < MARGIN) {
                    if (stream != null) {
                        stream.endText();
                        stream.close();
                    }
                    PDPage page = new PDPage(pageSize);
                    doc.addPage(page);
                    stream = new PDPageContentStream(doc, page);
                    stream.beginText();
                    stream.setFont(font, FONT_SIZE);
                    stream.setLeading(LEADING);
                    y = pageSize.getHeight() - MARGIN;
                    stream.newLineAtOffset(MARGIN, y);
                }
                if (!line.isEmpty()) {
                    stream.showText(line);
                }
                stream.newLine();
                y -= LEADING;
            }

            if (stream != null) {
                stream.endText();
                stream.close();
            } else {
                doc.addPage(new PDPage(pageSize));   // пустой файл: одна пустая страница
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    // Разбивает текст на строки, которые помещаются по ширине страницы
    private List<String> wrap(String text, PDFont font, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").replace("\t", "    ");

        for (String paragraph : normalized.split("\n", -1)) {
            String current = "";
            for (String word : paragraph.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(candidate, font) <= maxWidth) {
                    current = candidate;
                } else {
                    if (!current.isEmpty()) {
                        result.add(current);
                    }
                    current = word;
                }
            }
            result.add(current);
        }
        return result;
    }

    private float width(String text, PDFont font) throws IOException {
        return font.getStringWidth(text) / 1000f * FONT_SIZE;
    }
}