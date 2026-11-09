--liquibase formatted sql

--changeset converter:001-create-inbox
CREATE TABLE IF NOT EXISTS inbox (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
--rollback DROP TABLE inbox;