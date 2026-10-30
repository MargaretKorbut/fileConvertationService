package com.example.file_converter.converter;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.example.file_converter.exception.ConversionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipConverter implements Converter {

    private static final int MAX_ENTRIES = 100;   // защита от «zip-бомбы»

    private final ObjectProvider<ConversionService> conversionServiceProvider;

    public ZipConverter(ObjectProvider<ConversionService> conversionServiceProvider) {
        this.conversionServiceProvider = conversionServiceProvider;
    }

    @Override
    public boolean supports(FileType type) {
        return type == FileType.ZIP;
    }

    @Override
    public byte[] toPdf(String fileName, byte[] content) throws IOException {
        ConversionService conversionService = conversionServiceProvider.getObject();
        List<byte[]> pdfs = new ArrayList<>();
        int count = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || isJunk(entry.getName())) {
                    continue;
                }
                if (++count > MAX_ENTRIES) {
                    throw new ConversionException("В архиве слишком много файлов");
                }
                byte[] data = zip.readAllBytes();
                // та же самая «Стратегия»: если внутри ещё один zip, сработает рекурсия
                pdfs.add(conversionService.convert(entry.getName(), data));
            }
        }

        if (pdfs.isEmpty()) {
            throw new ConversionException("В архиве нет файлов для конвертации");
        }
        return pdfs.size() == 1 ? pdfs.get(0) : merge(pdfs);
    }

    // служебные файлы, которые создают macOS и Windows
    private boolean isJunk(String name) {
        String base = name.substring(name.lastIndexOf('/') + 1);
        return name.startsWith("__MACOSX/") || base.startsWith(".") || base.equalsIgnoreCase("Thumbs.db");
    }

    private byte[] merge(List<byte[]> pdfs) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        merger.setDestinationStream(out);
        for (byte[] pdf : pdfs) {
            merger.addSource(new RandomAccessReadBuffer(pdf));
        }
        merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
        return out.toByteArray();
    }
}