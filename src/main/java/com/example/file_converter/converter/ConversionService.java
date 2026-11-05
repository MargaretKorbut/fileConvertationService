package com.example.file_converter.converter;

import com.example.file_converter.exception.ConversionException;
import com.example.file_converter.exception.UnsupportedFileTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversionService {

    private final List<Converter> converters;

    public byte[] convert(String fileName, byte[] content) {
        FileType type = FileType.fromFileName(fileName);

        Converter converter = converters.stream()
                .filter(c -> c.supports(type))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(
                        "Нет конвертера для типа " + type));

        try {
            return converter.toPdf(fileName, content);
        } catch (ConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new ConversionException(
                    "Не удалось сконвертировать файл " + fileName + ": " + e.getMessage(), e);
        }
    }
}