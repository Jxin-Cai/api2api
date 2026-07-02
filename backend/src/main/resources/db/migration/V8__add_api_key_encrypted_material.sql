ALTER TABLE api_credentials
    ADD COLUMN encrypted_key_material TEXT,
    ADD COLUMN key_material_nonce VARCHAR(64),
    ADD COLUMN key_material_version INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_usage_records_api_credential_time
    ON usage_records(api_credential_id, started_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_usage_records_requested_model_time
    ON usage_records(requested_model, started_at DESC)
    WHERE deleted = FALSE;
