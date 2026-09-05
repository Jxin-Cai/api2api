ALTER TABLE usage_records
    ADD COLUMN IF NOT EXISTS first_token_millis BIGINT,
    ADD COLUMN IF NOT EXISTS client_ip VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_usage_records_client_ip ON usage_records(client_ip);
