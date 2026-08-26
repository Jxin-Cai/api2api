-- Channel evaluation (测评) runs executed against the external probe service.
CREATE TABLE channel_evaluations (
    id BIGINT PRIMARY KEY,
    provider_channel_id BIGINT NOT NULL,
    requested_model VARCHAR(256) NOT NULL,
    upstream_format VARCHAR(32) NOT NULL,
    provider_run_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    -- Already rebased onto 0-100 at ingestion so ranking and averaging stay comparable across runs.
    score NUMERIC(5, 2),
    detected_family VARCHAR(128),
    detected_model VARCHAR(256),
    detected_confidence NUMERIC(5, 4),
    family_mismatch BOOLEAN,
    channel_signature VARCHAR(128),
    report_url VARCHAR(512),
    passed_probe_count INTEGER,
    warning_probe_count INTEGER,
    failed_probe_count INTEGER,
    total_input_tokens BIGINT,
    total_output_tokens BIGINT,
    error_message TEXT,
    report_summary TEXT,
    requested_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_channel_evaluations_provider_channel
        FOREIGN KEY (provider_channel_id) REFERENCES provider_channels (id) ON DELETE CASCADE
);

-- History listing is always scoped to a channel and ordered by time.
CREATE INDEX idx_channel_evaluations_channel_requested_at
    ON channel_evaluations (provider_channel_id, requested_at DESC);

-- Score ranking and windowed averages only consider runs that produced a score.
CREATE INDEX idx_channel_evaluations_channel_score
    ON channel_evaluations (provider_channel_id, score DESC)
    WHERE score IS NOT NULL;

-- The async poller repeatedly scans unfinished runs.
CREATE INDEX idx_channel_evaluations_unfinished
    ON channel_evaluations (requested_at)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE UNIQUE INDEX uk_channel_evaluations_provider_run_id
    ON channel_evaluations (provider_run_id)
    WHERE provider_run_id IS NOT NULL;

-- One optional recurring evaluation schedule per provider channel.
CREATE TABLE channel_evaluation_schedules (
    id BIGINT PRIMARY KEY,
    provider_channel_id BIGINT NOT NULL,
    cron_expression VARCHAR(128) NOT NULL,
    zone_id VARCHAR(64) NOT NULL,
    -- JSON array of requested model names, same encoding as model_groups.model_whitelist.
    models TEXT NOT NULL DEFAULT '[]',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMPTZ,
    next_trigger_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_channel_evaluation_schedules_provider_channel
        FOREIGN KEY (provider_channel_id) REFERENCES provider_channels (id) ON DELETE CASCADE,
    CONSTRAINT uk_channel_evaluation_schedules_provider_channel UNIQUE (provider_channel_id)
);

CREATE INDEX idx_channel_evaluation_schedules_due
    ON channel_evaluation_schedules (next_trigger_at)
    WHERE enabled = TRUE;
