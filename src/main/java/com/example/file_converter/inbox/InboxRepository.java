package com.example.file_converter.inbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class InboxRepository {

    private final JdbcTemplate jdbc;

    public InboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Уже обрабатывали такое событие? */
    public boolean exists(UUID eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM inbox WHERE event_id = ?", Integer.class, eventId);
        return count != null && count > 0;
    }

    /** true: id записан впервые. false: такой id уже был (дубль). */
    public boolean tryInsert(UUID eventId) {
        int rows = jdbc.update(
                "INSERT INTO inbox (event_id) VALUES (?) ON CONFLICT (event_id) DO NOTHING",
                eventId);
        return rows == 1;
    }
}
