package com.example.file_converter.exception;

public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String message) { super(message); }
    public InvalidMessageException(String message, Throwable cause) { super(message, cause); }
}
