CREATE TABLE IF NOT EXISTS inbox (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS outbox (
    id           UUID PRIMARY KEY,
    event_key    VARCHAR(100) NOT NULL,
    payload      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox (created_at) WHERE published_at IS NULL;