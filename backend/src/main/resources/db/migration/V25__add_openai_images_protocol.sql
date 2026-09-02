-- OpenAI Images (/v1/images/generations) inbound protocol. Images requests are
-- never converted to another protocol; the same-protocol passthrough definition
-- makes routing native-only for gpt-image models.
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
    (22, 'OPENAI_IMAGES', 'OPENAI_IMAGES', 'PASSTHROUGH', 'ENABLED', 'IMPLEMENTED',
     TRUE, FALSE, FALSE, TRUE, FALSE,
     'Request passthrough',
     'Response passthrough',
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
