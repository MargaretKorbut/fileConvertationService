package com.example.file_converter;

import com.example.file_converter.event.ConversionRequestedEvent;
import com.example.file_converter.event.ConversionResultEvent;
import com.example.file_converter.event.ConversionStatus;
import com.example.file_converter.handler.ConversionResultRecorder;
import com.example.file_converter.inbox.InboxRepository;
import com.example.file_converter.outbox.OutboxMessage;
import com.example.file_converter.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "app.outbox.publisher.enabled=false"
})
@Import(PostgresTestConfig.class)
class DatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private InboxRepository inboxRepository;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private ConversionResultRecorder recorder;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanTables() {
        jdbc.update("DELETE FROM outbox");
        jdbc.update("DELETE FROM inbox");
    }

    @Test
    void liquibaseCreatesTables() {
        Integer tables = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN ('inbox', 'outbox')",
                Integer.class);
        Integer changeSets = jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class);

        assertThat(tables).isEqualTo(2);
        assertThat(changeSets).isEqualTo(2);
    }

    @Test
    void inboxRemembersProcessedEvents() {
        UUID id = UUID.randomUUID();

        assertThat(inboxRepository.exists(id)).isFalse();
        assertThat(inboxRepository.tryInsert(id)).isTrue();
        assertThat(inboxRepository.tryInsert(id)).isFalse();
        assertThat(inboxRepository.exists(id)).isTrue();
    }

    @Test
    void outboxReturnsOnlyUnpublishedMessages() {
        UUID id = UUID.randomUUID();
        outboxRepository.save(id, "key-1", "{\"a\":1}");

        assertThat(outboxRepository.findUnpublished(10)).extracting(OutboxMessage::id).containsExactly(id);

        outboxRepository.markPublished(id);

        assertThat(outboxRepository.findUnpublished(10)).isEmpty();
    }

    @Test
    void outboxRespectsLimit() {
        for (int i = 0; i < 3; i++) {
            outboxRepository.save(UUID.randomUUID(), "key-" + i, "{}");
        }

        assertThat(outboxRepository.findUnpublished(2)).hasSize(2);
    }

    @Test
    void skipLockedMakesParallelInstancesTakeDifferentRows() {
        outboxRepository.save(UUID.randomUUID(), "key-1", "{}");
        outboxRepository.save(UUID.randomUUID(), "key-2", "{}");

        TransactionTemplate firstInstance = new TransactionTemplate(transactionManager);
        TransactionTemplate secondInstance = new TransactionTemplate(transactionManager);
        // вторая транзакция идёт параллельно первой, как будто это другой инстанс сервиса
        secondInstance.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        firstInstance.executeWithoutResult(status -> {
            List<OutboxMessage> taken = outboxRepository.findUnpublished(10);
            List<OutboxMessage> takenByOther = secondInstance.execute(s -> outboxRepository.findUnpublished(10));

            assertThat(taken).hasSize(2);
            // строки заблокированы первой транзакцией, второй инстанс их пропускает
            assertThat(takenByOther).isEmpty();
        });
    }

    @Test
    void recorderWritesInboxAndOutboxOnlyOnce() {
        UUID sourceId = UUID.randomUUID();
        ConversionRequestedEvent request = new ConversionRequestedEvent(sourceId, "files", "a.txt");

        recorder.record(sourceId, ConversionResultEvent.success(request, "converted/x.pdf"));
        // повторная доставка того же события (у результата уже другой eventId)
        recorder.record(sourceId, ConversionResultEvent.success(request, "converted/x.pdf"));

        assertThat(count("inbox")).isEqualTo(1);
        assertThat(count("outbox")).isEqualTo(1);
    }

    @Test
    void recorderRollsBackInboxWhenOutboxWriteFails() {
        UUID sourceId = UUID.randomUUID();
        // eventId = null нарушит ограничение NOT NULL в outbox
        ConversionResultEvent broken = new ConversionResultEvent(
                null, sourceId, ConversionStatus.SUCCESS, "files", "k", null);

        assertThatThrownBy(() -> recorder.record(sourceId, broken)).isInstanceOf(DataAccessException.class);

        // запись в inbox откатилась вместе с outbox
        assertThat(inboxRepository.exists(sourceId)).isFalse();
        assertThat(count("outbox")).isZero();
    }

    private int count(String table) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}