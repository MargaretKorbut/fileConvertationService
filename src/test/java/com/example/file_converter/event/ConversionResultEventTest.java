package com.example.file_converter.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionResultEventTest {

    private final ConversionRequestedEvent request =
            new ConversionRequestedEvent(UUID.randomUUID(), "files", "a.txt");

    @Test
    void successEventCarriesResultKey() {
        ConversionResultEvent event = ConversionResultEvent.success(request, "converted/x.pdf");

        assertThat(event.status()).isEqualTo(ConversionStatus.SUCCESS);
        assertThat(event.resultKey()).isEqualTo("converted/x.pdf");
        assertThat(event.errorMessage()).isNull();
        assertThat(event.sourceEventId()).isEqualTo(request.eventId());
        assertThat(event.eventId()).isNotEqualTo(request.eventId());
    }

    @Test
    void failedEventCarriesErrorMessage() {
        ConversionResultEvent event = ConversionResultEvent.failed(request, "ошибка");

        assertThat(event.status()).isEqualTo(ConversionStatus.FAILED);
        assertThat(event.errorMessage()).isEqualTo("ошибка");
        assertThat(event.resultKey()).isNull();
    }
}