package com.example.file_converter.handler;

import com.example.file_converter.event.ConversionResultEvent;
import com.example.file_converter.inbox.InboxRepository;
import com.example.file_converter.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class ConversionResultRecorder {

    private static final Logger log = LoggerFactory.getLogger(ConversionResultRecorder.class);

    private final InboxRepository inboxRepository;
    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public ConversionResultRecorder(InboxRepository inboxRepository,
                                    OutboxRepository outboxRepository,
                                    JsonMapper jsonMapper) {
        this.inboxRepository = inboxRepository;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void record(UUID sourceEventId, ConversionResultEvent result) {
        // 1. Отмечаем событие как обработанное. false = уже было.
        if (!inboxRepository.tryInsert(sourceEventId)) {
            log.info("Событие {} уже обработано, пропускаем", sourceEventId);
            return;
        }
        // 2. В ТОЙ ЖЕ транзакции кладём результат в outbox
        String payload = jsonMapper.writeValueAsString(result);
        outboxRepository.save(result.eventId(), sourceEventId.toString(), payload);
    }
}
