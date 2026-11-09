package com.example.file_converter.handler;

import com.example.file_converter.event.ConversionRequestedEvent;
import com.example.file_converter.event.ConversionResultEvent;
import com.example.file_converter.inbox.InboxRepository;
import com.example.file_converter.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionResultRecorderTest {

    @Mock
    private InboxRepository inboxRepository;
    @Mock
    private OutboxRepository outboxRepository;

    private ConversionResultRecorder recorder;

    private final UUID sourceId = UUID.randomUUID();
    private final ConversionResultEvent result = ConversionResultEvent.success(
            new ConversionRequestedEvent(sourceId, "files", "a.txt"), "converted/x.pdf");

    @BeforeEach
    void setUp() {
        recorder = new ConversionResultRecorder(
                inboxRepository, outboxRepository, JsonMapper.builder().build());
    }

    @Test
    void savesResultToOutboxWhenEventIsNew() {
        when(inboxRepository.tryInsert(sourceId)).thenReturn(true);

        recorder.record(sourceId, result);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).save(eq(result.eventId()), eq(sourceId.toString()), payload.capture());
        assertThat(payload.getValue()).contains("SUCCESS").contains("converted/x.pdf");
    }

    @Test
    void doesNothingWhenEventWasAlreadyProcessed() {
        when(inboxRepository.tryInsert(sourceId)).thenReturn(false);

        recorder.record(sourceId, result);

        verifyNoInteractions(outboxRepository);
    }
}