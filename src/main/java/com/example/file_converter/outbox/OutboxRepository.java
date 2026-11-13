package com.example.file_converter.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {

    private final JdbcTemplate jdbc;

    public void save(UUID id, String key, String payload) {
        jdbc.update("INSERT INTO outbox (id, event_key, payload) VALUES (?, ?, ?)",
                id, key, payload);
    }

    public List<OutboxMessage> findUnpublished(int limit) {
        return jdbc.query(
                "SELECT id, event_key, payload FROM outbox " +
                        "WHERE published_at IS NULL ORDER BY created_at LIMIT ? " +
                        "FOR UPDATE SKIP LOCKED",
                (rs, rowNum) -> new OutboxMessage(
                        rs.getObject("id", UUID.class),
                        rs.getString("event_key"),
                        rs.getString("payload")),
                limit);
    }

    public void markPublished(UUID id) {
        jdbc.update("UPDATE outbox SET published_at = now() WHERE id = ?", id);
    }
}