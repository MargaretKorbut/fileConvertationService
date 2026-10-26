package com.example.file_converter.outbox;

import java.util.UUID;

public record OutboxMessage(UUID id, String key, String payload) {}