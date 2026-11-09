-- Images kept in PostgreSQL (for hosts without a persistent disk, e.g. Render free plan).
CREATE TABLE stored_image (
    id            UUID PRIMARY KEY,
    content_type  VARCHAR(40) NOT NULL,
    data          BYTEA NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
