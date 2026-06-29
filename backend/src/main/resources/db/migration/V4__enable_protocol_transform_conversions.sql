UPDATE protocol_conversion_definitions
SET status = 'ENABLED',
    implementation_status = 'IMPLEMENTED',
    supports_streaming = FALSE,
    supports_tool_calling = FALSE,
    supports_reasoning = FALSE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    request_mapping_json = 'Claude messages/model/max_tokens/system -> OpenAI responses input/model/max_output_tokens/instructions',
    response_mapping_json = 'OpenAI responses output/usage -> Claude messages content/usage',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'OPENAI_RESPONSES';

UPDATE protocol_conversion_definitions
SET status = 'ENABLED',
    implementation_status = 'IMPLEMENTED',
    supports_streaming = FALSE,
    supports_tool_calling = FALSE,
    supports_reasoning = FALSE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    request_mapping_json = 'Claude messages/model/max_tokens/system -> OpenAI chat messages/model/max_tokens/system message',
    response_mapping_json = 'OpenAI chat choices/usage -> Claude messages content/usage',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'OPENAI_CHAT_COMPLETIONS';

UPDATE protocol_conversion_definitions
SET status = 'ENABLED',
    implementation_status = 'IMPLEMENTED',
    supports_streaming = FALSE,
    supports_tool_calling = FALSE,
    supports_reasoning = FALSE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    request_mapping_json = 'OpenAI responses input/instructions/model/max_output_tokens -> Claude messages/system/model/max_tokens',
    response_mapping_json = 'Claude messages content/usage -> OpenAI responses output/usage',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_RESPONSES'
  AND target_protocol = 'CLAUDE_MESSAGES';

UPDATE protocol_conversion_definitions
SET status = 'ENABLED',
    implementation_status = 'IMPLEMENTED',
    supports_streaming = FALSE,
    supports_tool_calling = FALSE,
    supports_reasoning = FALSE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    request_mapping_json = 'OpenAI responses input/instructions/model/max_output_tokens -> OpenAI chat messages/model/max_tokens',
    response_mapping_json = 'OpenAI chat choices/usage -> OpenAI responses output/usage',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_RESPONSES'
  AND target_protocol = 'OPENAI_CHAT_COMPLETIONS';

UPDATE protocol_conversion_definitions
SET status = 'ENABLED',
    implementation_status = 'IMPLEMENTED',
    supports_streaming = FALSE,
    supports_tool_calling = FALSE,
    supports_reasoning = FALSE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    request_mapping_json = 'OpenAI chat messages/model/max_tokens -> Claude messages/system/model/max_tokens',
    response_mapping_json = 'Claude messages content/usage -> OpenAI chat choices/usage',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_CHAT_COMPLETIONS'
  AND target_protocol = 'CLAUDE_MESSAGES';

UPDATE protocol_conversion_definitions
SET status = 'ENABLED',
    implementation_status = 'IMPLEMENTED',
    supports_streaming = FALSE,
    supports_tool_calling = FALSE,
    supports_reasoning = FALSE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    request_mapping_json = 'OpenAI chat messages/model/max_tokens -> OpenAI responses input/instructions/model/max_output_tokens',
    response_mapping_json = 'OpenAI responses output/usage -> OpenAI chat choices/usage',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_CHAT_COMPLETIONS'
  AND target_protocol = 'OPENAI_RESPONSES';
