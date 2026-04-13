CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    data JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    data JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    data JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    endpoint TEXT NOT NULL,
    language TEXT NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_metrics_endpoint_language ON metrics(endpoint, language);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON metrics(timestamp);
