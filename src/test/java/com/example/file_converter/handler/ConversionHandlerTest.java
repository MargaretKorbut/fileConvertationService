package com.example.file_converter.handler;

import com.example.file_converter.converter.ConversionService;
import com.example.file_converter.event.ConversionRequestedEvent;
import com.example.file_converter.event.ConversionResultEvent;
import com.example.file_converter.event.ConversionStatus;
import com.example.file_converter.exception.SourceNotFoundException;
import com.example.file_converter.exception.StorageException;
import com.example.file_converter.exception.UnsupportedFileTypeException;
import com.example.file_converter.inbox.InboxRepository;
import com.example.file_converter.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionHandlerTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private StorageService storageService;
    @Mock
    private ConversionService conversionService;
    @Mock
    private InboxRepository inboxRepository;
    @Mock
    private ConversionResultRecorder recorder;

    @InjectMocks
    private ConversionHandler handler;

    private final ConversionRequestedEvent event =
            new ConversionRequestedEvent(EVENT_ID, "files", "docs/test.txt");

    @Test
    void skipsEventThatWasAlreadyProcessed() {
        when(inboxRepository.exists(EVENT_ID)).thenReturn(true);

        handler.handle(event);

        verifyNoInteractions(storageService, conversionService, recorder);
    }

    @Test
    void convertsFileAndRecordsSuccess() {
        byte[] source = {1};
        byte[] pdf = {2};
        when(storageService.download("files", "docs/test.txt")).thenReturn(source);
        // в конвертер уходит только имя файла без папок
        when(conversionService.convert("test.txt", source)).thenReturn(pdf);

        handler.handle(event);

        String resultKey = "converted/" + EVENT_ID + ".pdf";
        verify(storageService).upload("files", resultKey, pdf, "application/pdf");
        ConversionResultEvent result = capturedResult();
        assertThat(result.status()).isEqualTo(ConversionStatus.SUCCESS);
        assertThat(result.resultKey()).isEqualTo(resultKey);
    }

    @Test
    void recordsFailureWhenFileTypeIsUnsupported() {
        when(storageService.download(any(), any())).thenReturn(new byte[]{1});
        when(conversionService.convert(any(), any()))
                .thenThrow(new UnsupportedFileTypeException("Неподдерживаемый формат: .docx"));

        handler.handle(event);

        verify(storageService, never()).upload(any(), any(), any(), any());
        ConversionResultEvent result = capturedResult();
        assertThat(result.status()).isEqualTo(ConversionStatus.FAILED);
        assertThat(result.errorMessage()).contains("docx");
    }

    @Test
    void recordsFailureWhenSourceFileIsMissing() {
        when(storageService.download(any(), any()))
                .thenThrow(new SourceNotFoundException("Файл не найден"));

        handler.handle(event);

        ConversionResultEvent result = capturedResult();
        assertThat(result.status()).isEqualTo(ConversionStatus.FAILED);
        assertThat(result.errorMessage()).contains("не найден");
    }

    @Test
    void technicalErrorIsNotRecordedAndGoesToRetry() {
        when(storageService.download(any(), any()))
                .thenThrow(new StorageException("MinIO недоступен", new RuntimeException()));

        assertThatThrownBy(() -> handler.handle(event)).isInstanceOf(StorageException.class);

        verifyNoInteractions(recorder);
    }

    private ConversionResultEvent capturedResult() {
        ArgumentCaptor<ConversionResultEvent> captor = ArgumentCaptor.forClass(ConversionResultEvent.class);
        verify(recorder).record(eq(EVENT_ID), captor.capture());
        return captor.getValue();
    }
}