CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGINT PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS api_credentials (
    id BIGINT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES user_accounts(id),
    name VARCHAR(128) NOT NULL,
    key_hash VARCHAR(128) NOT NULL UNIQUE,
    key_preview VARCHAR(64) NOT NULL,
    model_whitelist TEXT NOT NULL DEFAULT '',
    token_limit BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS provider_channels (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    host VARCHAR(512) NOT NULL,
    key_ref VARCHAR(128) NOT NULL,
    supported_protocols TEXT NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS channel_model_supports (
    id BIGINT PRIMARY KEY,
    provider_channel_id BIGINT NOT NULL REFERENCES provider_channels(id),
    requested_model VARCHAR(256) NOT NULL,
    upstream_model VARCHAR(256) NOT NULL,
    upstream_protocol VARCHAR(64) NOT NULL,
    priority INTEGER NOT NULL,
    source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS protocol_conversion_definitions (
    id BIGINT PRIMARY KEY,
    source_protocol VARCHAR(64) NOT NULL,
    target_protocol VARCHAR(64) NOT NULL,
    kind VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    implementation_status VARCHAR(32) NOT NULL,
    supports_streaming BOOLEAN NOT NULL DEFAULT FALSE,
    supports_tool_calling BOOLEAN NOT NULL DEFAULT FALSE,
    supports_reasoning BOOLEAN NOT NULL DEFAULT FALSE,
    supports_usage_mapping BOOLEAN NOT NULL DEFAULT FALSE,
    supports_cache_token_mapping BOOLEAN NOT NULL DEFAULT FALSE,
    request_mapping_json TEXT NOT NULL DEFAULT '{}',
    response_mapping_json TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (source_protocol, target_protocol)
);

CREATE TABLE IF NOT EXISTS usage_records (
    id BIGINT PRIMARY KEY,
    request_id VARCHAR(128) NOT NULL UNIQUE,
    user_account_id BIGINT NOT NULL REFERENCES user_accounts(id),
    api_credential_id BIGINT NOT NULL REFERENCES api_credentials(id),
    requested_model VARCHAR(256) NOT NULL,
    upstream_model VARCHAR(256),
    request_protocol VARCHAR(64) NOT NULL,
    upstream_protocol VARCHAR(64),
    provider_channel_id BIGINT REFERENCES provider_channels(id),
    status VARCHAR(32) NOT NULL,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    cache_creation_input_tokens BIGINT NOT NULL DEFAULT 0,
    cache_read_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    streaming BOOLEAN NOT NULL DEFAULT FALSE,
    error_type VARCHAR(128),
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_usage_records_user_time ON usage_records(user_account_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_records_api_credential ON usage_records(api_credential_id);
CREATE INDEX IF NOT EXISTS idx_usage_records_provider_channel ON usage_records(provider_channel_id);
