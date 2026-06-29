INSERT INTO user_accounts (id, username, display_name, role, status, created_at, updated_at, deleted)
VALUES
    (1, 'admin', 'Admin', 'ADMIN', 'ACTIVE', NOW(), NOW(), FALSE),
    (2, 'user', 'User', 'USER', 'ACTIVE', NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO provider_channels (id, name, host, key_ref, supported_protocols, status, created_at, updated_at, deleted)
VALUES
    (1, 'Demo Provider', 'https://api.openai.com', 'demo-provider-key', 'OPENAI_CHAT_COMPLETIONS,OPENAI_RESPONSES', 'ENABLED', NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO channel_model_supports (id, provider_channel_id, requested_model, upstream_model, upstream_protocol, priority, source, status, created_at, updated_at)
VALUES
    (1, 1, 'gpt-4o-mini', 'gpt-4o-mini', 'OPENAI_CHAT_COMPLETIONS', 100, 'MANUAL', 'ENABLED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO protocol_conversion_definitions (
    id,
    source_protocol,
    target_protocol,
    kind,
    status,
    implementation_status,
    supports_streaming,
    supports_tool_calling,
    supports_reasoning,
    supports_usage_mapping,
    supports_cache_token_mapping,
    request_mapping_json,
    response_mapping_json,
    created_at,
    updated_at
)
VALUES
    (1, 'CLAUDE_MESSAGES', 'CLAUDE_MESSAGES', 'PASSTHROUGH', 'ENABLED', 'IMPLEMENTED', TRUE, TRUE, TRUE, TRUE, TRUE, 'Request passthrough', 'Response passthrough', NOW(), NOW()),
    (2, 'OPENAI_RESPONSES', 'OPENAI_RESPONSES', 'PASSTHROUGH', 'ENABLED', 'IMPLEMENTED', TRUE, TRUE, TRUE, TRUE, TRUE, 'Request passthrough', 'Response passthrough', NOW(), NOW()),
    (3, 'OPENAI_CHAT_COMPLETIONS', 'OPENAI_CHAT_COMPLETIONS', 'PASSTHROUGH', 'ENABLED', 'IMPLEMENTED', TRUE, TRUE, FALSE, TRUE, FALSE, 'Request passthrough', 'Response passthrough', NOW(), NOW()),
    (4, 'CLAUDE_MESSAGES', 'OPENAI_RESPONSES', 'TRANSFORM', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', TRUE, TRUE, TRUE, TRUE, TRUE, 'Request mapping pending', 'Response mapping pending', NOW(), NOW()),
    (5, 'CLAUDE_MESSAGES', 'OPENAI_CHAT_COMPLETIONS', 'TRANSFORM', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', TRUE, TRUE, FALSE, TRUE, FALSE, 'Request mapping pending', 'Response mapping pending', NOW(), NOW()),
    (6, 'OPENAI_RESPONSES', 'CLAUDE_MESSAGES', 'TRANSFORM', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', TRUE, TRUE, TRUE, TRUE, TRUE, 'Request mapping pending', 'Response mapping pending', NOW(), NOW()),
    (7, 'OPENAI_RESPONSES', 'OPENAI_CHAT_COMPLETIONS', 'TRANSFORM', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', TRUE, TRUE, FALSE, TRUE, FALSE, 'Request mapping pending', 'Response mapping pending', NOW(), NOW()),
    (8, 'OPENAI_CHAT_COMPLETIONS', 'CLAUDE_MESSAGES', 'TRANSFORM', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', TRUE, TRUE, FALSE, TRUE, FALSE, 'Request mapping pending', 'Response mapping pending', NOW(), NOW()),
    (9, 'OPENAI_CHAT_COMPLETIONS', 'OPENAI_RESPONSES', 'TRANSFORM', 'NOT_IMPLEMENTED', 'NOT_IMPLEMENTED', TRUE, TRUE, FALSE, TRUE, FALSE, 'Request mapping pending', 'Response mapping pending', NOW(), NOW())
ON CONFLICT (source_protocol, target_protocol) DO NOTHING;
