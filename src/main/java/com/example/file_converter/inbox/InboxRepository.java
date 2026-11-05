package com.example.file_converter.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InboxRepository {

    private final JdbcTemplate jdbc;

    public boolean exists(UUID eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM inbox WHERE event_id = ?", Integer.class, eventId);
        return count != null && count > 0;
    }

    public boolean tryInsert(UUID eventId) {
        int rows = jdbc.update(
                "INSERT INTO inbox (event_id) VALUES (?) ON CONFLICT (event_id) DO NOTHING",
                eventId);
        return rows == 1;
    }
}