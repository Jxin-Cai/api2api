ALTER TABLE api_credentials
    ALTER COLUMN model_whitelist SET DEFAULT '[]';

UPDATE api_credentials
SET model_whitelist = '[]'
WHERE model_whitelist IS NULL OR btrim(model_whitelist) = '';

ALTER TABLE usage_records
    ADD COLUMN IF NOT EXISTS usage_known BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS duration_millis BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS route_failures_json TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE usage_records
SET created_at = started_at
WHERE created_at IS NULL;

UPDATE usage_records
SET updated_at = COALESCE(ended_at, started_at)
WHERE updated_at IS NULL;

UPDATE usage_records
SET duration_millis = GREATEST(
    0,
    CAST(EXTRACT(EPOCH FROM (COALESCE(ended_at, started_at) - started_at)) * 1000 AS BIGINT)
)
WHERE duration_millis = 0;

ALTER TABLE usage_records
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_usage_records_started_at ON usage_records(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_records_protocol_time ON usage_records(request_protocol, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_records_model_time ON usage_records(requested_model, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_records_provider_time ON usage_records(provider_channel_id, started_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_channel_model_supports_combination
    ON channel_model_supports(provider_channel_id, requested_model, upstream_protocol);
