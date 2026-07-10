UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'Claude messages/system/model/max_tokens/tools/tool_choice/thinking -> Bedrock messages/system/inferenceConfig/toolConfig/additionalModelRequestFields',
    response_mapping_json = 'Bedrock output/usage/stopReason/toolUse/reasoningContent/stream events -> Claude messages content/usage/stop_reason/SSE events',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'AWS_BEDROCK_CONVERSE';

UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'Bedrock output/usage/stopReason/toolUse/reasoningContent/stream events -> Claude messages content/usage/stop_reason/SSE events',
    response_mapping_json = 'Claude messages/system/model/max_tokens/tools/tool_choice/thinking -> Bedrock messages/system/inferenceConfig/toolConfig/additionalModelRequestFields',
    updated_at = NOW()
WHERE source_protocol = 'AWS_BEDROCK_CONVERSE'
  AND target_protocol = 'CLAUDE_MESSAGES';
