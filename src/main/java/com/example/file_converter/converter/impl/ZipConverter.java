package com.example.file_converter.converter.impl;

import com.example.file_converter.converter.ConversionService;
import com.example.file_converter.converter.Converter;
import com.example.file_converter.converter.FileType;
import com.example.file_converter.exception.ConversionException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
public class ZipConverter implements Converter {

    private static final int MAX_ENTRIES = 100;   // защита от «zip-бомбы»

    private final ObjectProvider<ConversionService> conversionServiceProvider;

    @Override
    public boolean supports(FileType type) {
        return type == FileType.ZIP;
    }

    @Override
    public byte[] toPdf(String fileName, byte[] content) throws IOException {
        try {
            return convertZip(content, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | ZipException e) {
            return convertZip(content, Charset.forName("Cp866"));
        }
    }

    private byte[] convertZip(byte[] content, Charset charset) throws IOException {
        ConversionService conversionService = conversionServiceProvider.getObject();
        List<byte[]> pdfs = new ArrayList<>();
        int count = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), charset)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || isJunk(entry.getName())) {
                    continue;
                }
                if (++count > MAX_ENTRIES) {
                    throw new ConversionException("В архиве слишком много файлов");
                }
                byte[] data = zip.readAllBytes();
                pdfs.add(conversionService.convert(entry.getName(), data));
            }
        }

        if (pdfs.isEmpty()) {
            throw new ConversionException("В архиве нет файлов для конвертации");
        }
        return pdfs.size() == 1 ? pdfs.get(0) : merge(pdfs);
    }

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