package com.example.file_converter.exception;

// техническая ошибка хранилища, НЕ наследует ConversionException
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
