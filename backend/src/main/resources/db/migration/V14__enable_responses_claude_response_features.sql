UPDATE protocol_conversion_definitions
SET supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    response_mapping_json = 'OpenAI responses output/usage/function_call -> Claude messages content/usage/tool_use/stop_reason',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_RESPONSES'
  AND target_protocol = 'CLAUDE_MESSAGES';
