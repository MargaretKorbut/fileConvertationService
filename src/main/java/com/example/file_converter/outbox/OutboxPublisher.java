package com.example.file_converter.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String resultsTopic;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           @Value("${app.topics.results}") String resultsTopic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.resultsTopic = resultsTopic;
    }

    @Scheduled(fixedDelay = 2000)
    public void publish() {
        List<OutboxMessage> batch = outboxRepository.findUnpublished(100);
        for (OutboxMessage message : batch) {
            try {
                // ждём подтверждения от Kafka
                kafkaTemplate.send(resultsTopic, message.key(), message.payload())
                        .get(10, TimeUnit.SECONDS);
                outboxRepository.markPublished(message.id());
                log.info("Опубликовано событие {}", message.id());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Kafka недоступна: остановимся, повторим при следующем запуске
                log.warn("Не удалось опубликовать {}: {}", message.id(), e.getMessage());
                return;
            }
        }
    }
}
