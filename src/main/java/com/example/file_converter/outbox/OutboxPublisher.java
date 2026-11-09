package com.example.file_converter.outbox;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.topics.results}")
    private String resultsTopic;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publish() {
        List<OutboxMessage> batch = outboxRepository.findUnpublished(BATCH_SIZE);
        for (OutboxMessage message : batch) {
            try {
                kafkaTemplate.send(resultsTopic, message.key(), message.payload())
                        .get(10, TimeUnit.SECONDS);
                outboxRepository.markPublished(message.id());
                log.info("Опубликовано событие {}", message.id());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Не удалось опубликовать {}: {}", message.id(), e.getMessage());
                return;
            }
        }
    }
}