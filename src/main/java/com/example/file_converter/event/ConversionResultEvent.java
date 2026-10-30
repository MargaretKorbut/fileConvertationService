package com.example.file_converter.event;

import java.util.UUID;

public record ConversionResultEvent(
        UUID eventId,
        UUID sourceEventId,
        String status,
        String bucket,
        String resultKey,
        String errorMessage) {

    public static ConversionResultEvent success(ConversionRequestedEvent src, String resultKey) {
        return new ConversionResultEvent(UUID.randomUUID(), src.eventId(),
                "SUCCESS", src.bucket(), resultKey, null);
    }

    public static ConversionResultEvent failed(ConversionRequestedEvent src, String error) {
        return new ConversionResultEvent(UUID.randomUUID(), src.eventId(),
                "FAILED", src.bucket(), null, error);
    }
}
