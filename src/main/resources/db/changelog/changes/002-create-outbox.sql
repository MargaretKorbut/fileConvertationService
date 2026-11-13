--liquibase formatted sql

--changeset converter:002-create-outbox
CREATE TABLE IF NOT EXISTS outbox (
    id           UUID PRIMARY KEY,
    event_key    VARCHAR(100) NOT NULL,
    payload      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox (created_at) WHERE published_at IS NULL;
--rollback DROP TABLE outbox;