UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'Claude messages/system/media/documents/custom tools/tool_choice/thinking/output_config/metadata/service_tier/speed -> OpenAI Chat Completions messages/tools/tool_choice/reasoning_effort/response_format/user/service_tier; text-only content folds to a string, tool history is normalized, hosted web_search tools are dropped',
    response_mapping_json = 'OpenAI Chat Completions choices/message/tool_calls/reasoning_content/usage/finish_reason/stream chunks -> Claude Messages content/tool_use/thinking/usage/stop_reason/SSE events',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'OPENAI_CHAT_COMPLETIONS';

UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'OpenAI Chat Completions messages/tools/tool_choice/reasoning_effort/response_format/max_completion_tokens -> Claude Messages system/messages/tools/tool_choice/thinking/max_tokens',
    response_mapping_json = 'Claude Messages content/tool_use/thinking/usage/stop_reason -> OpenAI Chat Completions choices/message/tool_calls/reasoning_content/usage/finish_reason',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_CHAT_COMPLETIONS'
  AND target_protocol = 'CLAUDE_MESSAGES';
