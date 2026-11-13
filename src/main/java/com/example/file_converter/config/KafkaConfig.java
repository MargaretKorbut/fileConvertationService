package com.example.file_converter.config;

import com.example.file_converter.exception.InvalidMessageException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic requestsTopic(@Value("${app.topics.requests}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic resultsTopic(@Value("${app.topics.results}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    // У DLQ столько же партиций, сколько у основного топика
    @Bean
    public NewTopic requestsDeadLetterTopic(@Value("${app.topics.requests}") String name) {
        return TopicBuilder.name(name + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        // После исчерпания попыток сообщение уходит в топик "<исходный>.DLT"
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

        // 3 повтора с паузой в 1 секунду, затем в DLQ
        DefaultErrorHandler handler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        // «Битые» сообщения повторять бесполезно: сразу в DLQ
        handler.addNotRetryableExceptions(InvalidMessageException.class);

        // После отправки в DLQ подтвердить offset, чтобы не получить то же сообщение при перезапуске
        handler.setCommitRecovered(true);
        return handler;
    }
}
