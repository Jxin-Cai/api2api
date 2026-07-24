-- Native Anthropic Claude Messages over Bedrock InvokeModel. This route keeps
-- Claude tool, reasoning, cache, context-management, and compaction semantics intact.
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
    (20, 'CLAUDE_MESSAGES', 'AWS_BEDROCK_CLAUDE_MESSAGES', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED',
     TRUE, TRUE, TRUE, TRUE, TRUE,
     'Claude Messages body preserved; model/stream removed; anthropic_version injected',
     'InvokeModel response is native Claude Messages format',
     NOW(), NOW()),
    (21, 'AWS_BEDROCK_CLAUDE_MESSAGES', 'CLAUDE_MESSAGES', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED',
     TRUE, TRUE, TRUE, TRUE, TRUE,
     'InvokeModel response is native Claude Messages format',
     'Identity conversion',
     NOW(), NOW())
ON CONFLICT (source_protocol, target_protocol) DO UPDATE
SET status = EXCLUDED.status,
    implementation_status = EXCLUDED.implementation_status,
    supports_streaming = EXCLUDED.supports_streaming,
    supports_tool_calling = EXCLUDED.supports_tool_calling,
    supports_reasoning = EXCLUDED.supports_reasoning,
    supports_usage_mapping = EXCLUDED.supports_usage_mapping,
    supports_cache_token_mapping = EXCLUDED.supports_cache_token_mapping,
    request_mapping_json = EXCLUDED.request_mapping_json,
    response_mapping_json = EXCLUDED.response_mapping_json,
    updated_at = NOW();
