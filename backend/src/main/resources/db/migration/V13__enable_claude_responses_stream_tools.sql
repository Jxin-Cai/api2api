UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'Claude messages/system/tools/tool_choice/thinking -> OpenAI responses input/instructions/tools/tool_choice/reasoning',
    response_mapping_json = 'OpenAI responses output/usage/stream events -> Claude messages content/tool_use/usage/SSE events',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'OPENAI_RESPONSES';
