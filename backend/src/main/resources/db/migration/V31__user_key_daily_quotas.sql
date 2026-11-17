ALTER TABLE token_quota_rules
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'ADMIN' CHECK (source IN ('ADMIN', 'USER'));

ALTER TABLE token_quota_rules
    DROP CONSTRAINT token_quota_rules_scope_target_id_model_period_key,
    ADD CONSTRAINT uk_token_quota_source UNIQUE (scope, target_id, model, period, source),
    ADD CONSTRAINT ck_user_key_daily_quota CHECK (
        source <> 'USER' OR (scope = 'API_KEY' AND period = 'DAY' AND length(trim(model)) > 0)
    );
