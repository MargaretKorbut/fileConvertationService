package com.example.file_converter.converter;

import org.springframework.stereotype.Service;
import com.example.file_converter.exception.ConversionException;
import com.example.file_converter.exception.UnsupportedFileTypeException;
import java.util.List;

@Service
public class ConversionService {

    // Spring сам соберёт сюда ВСЕ классы, помеченные @Component и реализующие Converter
    private final List<Converter> converters;

    public ConversionService(List<Converter> converters) {
        this.converters = converters;
    }

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