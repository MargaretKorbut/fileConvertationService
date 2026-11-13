package com.example.file_converter.event;

import java.util.UUID;

public record ConversionRequestedEvent(UUID eventId, String bucket, String objectKey) {}