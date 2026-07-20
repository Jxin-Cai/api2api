ALTER TABLE channel_model_supports
    ADD COLUMN IF NOT EXISTS rate_limited_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rate_limit_reset_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_channel_model_supports_rate_limit_reset
    ON channel_model_supports(rate_limit_reset_at)
    WHERE status = 'RATE_LIMITED';

-- Previous versions represented any model 429 as a degraded channel. Removing that
-- behavior must also recover channels left isolated by the old implementation.
UPDATE provider_channels
SET status = 'ENABLED',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'DEGRADED'
  AND deleted = FALSE;
