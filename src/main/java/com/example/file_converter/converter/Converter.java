package com.example.file_converter.converter;

import java.io.IOException;

public interface Converter {
    boolean supports(FileType type);
    byte[] toPdf(String fileName, byte[] content) throws IOException;
}