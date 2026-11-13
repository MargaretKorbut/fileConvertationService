package com.example.file_converter.converter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import com.example.file_converter.exception.UnsupportedFileTypeException;

public enum FileType {
    TXT("txt"),
    PNG("png"),
    JPG("jpg", "jpeg"),
    ZIP("zip");

    private final Set<String> extensions;

    FileType(String... extensions) {
        this.extensions = Set.of(extensions);
    }

    public static FileType fromFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new UnsupportedFileTypeException("У файла нет расширения: " + fileName);
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.extensions.contains(ext))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(
                        "Неподдерживаемый формат: ." + ext));
    }
}