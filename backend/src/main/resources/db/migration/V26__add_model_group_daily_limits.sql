-- Per-model daily token caps shared by every API credential bound to the group.
-- Stored as a JSON object: {"<model name>": <daily actual-token limit>}.
ALTER TABLE model_groups
    ADD COLUMN IF NOT EXISTS model_daily_limits TEXT NOT NULL DEFAULT '{}';

-- Daily-limit enforcement aggregates today's usage per (credential, model) on every request.
CREATE INDEX IF NOT EXISTS idx_usage_records_credential_model_started
    ON usage_records(api_credential_id, requested_model, started_at)
    WHERE deleted = FALSE;
