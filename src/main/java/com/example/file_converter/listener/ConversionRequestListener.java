package com.example.file_converter.listener;

import com.example.file_converter.event.ConversionRequestedEvent;
import com.example.file_converter.exception.InvalidMessageException;
import com.example.file_converter.handler.ConversionHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ConversionRequestListener {

    private final ConversionHandler handler;
    private final JsonMapper jsonMapper;

    public ConversionRequestListener(ConversionHandler handler, JsonMapper jsonMapper) {
        this.handler = handler;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "${app.topics.requests}")
    public void onMessage(String message, Acknowledgment ack) {
        ConversionRequestedEvent event = parse(message);
        handler.handle(event);
        // offset подтверждаем только ПОСЛЕ того, как всё записано в БД
        ack.acknowledge();
    }

    private ConversionRequestedEvent parse(String message) {
        ConversionRequestedEvent event;
        try {
            event = jsonMapper.readValue(message, ConversionRequestedEvent.class);
        } catch (JacksonException e) {
            throw new InvalidMessageException("Невалидный JSON: " + message, e);
        }
        if (event == null || event.eventId() == null
                || event.bucket() == null || event.objectKey() == null) {
            throw new InvalidMessageException("В сообщении не хватает полей: " + message);
        }
        return event;
    }
}
