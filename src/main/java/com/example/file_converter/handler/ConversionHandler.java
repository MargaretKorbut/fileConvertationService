package com.example.file_converter.handler;

import com.example.file_converter.converter.ConversionService;
import com.example.file_converter.event.ConversionRequestedEvent;
import com.example.file_converter.event.ConversionResultEvent;
import com.example.file_converter.exception.ConversionException;
import com.example.file_converter.inbox.InboxRepository;
import com.example.file_converter.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConversionHandler {

    private static final Logger log = LoggerFactory.getLogger(ConversionHandler.class);

    private final StorageService storageService;
    private final ConversionService conversionService;
    private final InboxRepository inboxRepository;
    private final ConversionResultRecorder recorder;

    public ConversionHandler(StorageService storageService,
                             ConversionService conversionService,
                             InboxRepository inboxRepository,
                             ConversionResultRecorder recorder) {
        this.storageService = storageService;
        this.conversionService = conversionService;
        this.inboxRepository = inboxRepository;
        this.recorder = recorder;
    }

    public void handle(ConversionRequestedEvent event) {
        // Быстрая проверка: если уже обработано, не тратим время на конвертацию
        if (inboxRepository.exists(event.eventId())) {
            log.info("Событие {} уже обработано, пропускаем", event.eventId());
            return;
        }

        ConversionResultEvent result;
        try {
            byte[] source = storageService.download(event.bucket(), event.objectKey());
            byte[] pdf = conversionService.convert(fileNameOf(event.objectKey()), source);

            // Ключ зависит от id события: при повторе PDF просто перезапишется
            String resultKey = "converted/" + event.eventId() + ".pdf";
            storageService.upload(event.bucket(), resultKey, pdf, "application/pdf");

            result = ConversionResultEvent.success(event, resultKey);
            log.info("Файл {} сконвертирован в {}", event.objectKey(), resultKey);
        } catch (ConversionException e) {
            // Бизнес-ошибка (не тот формат, файл не найден, битый файл): повторять бесполезно.
            // Сообщаем о ней событием FAILED.
            log.warn("Не удалось сконвертировать {}: {}", event.objectKey(), e.getMessage());
            result = ConversionResultEvent.failed(event, e.getMessage());
        }
        // Технические ошибки (StorageException и др.) не ловим: они уйдут в ретраи и DLQ

        recorder.record(event.eventId(), result);
    }

    private String fileNameOf(String objectKey) {
        return objectKey.substring(objectKey.lastIndexOf('/') + 1);
    }
}
