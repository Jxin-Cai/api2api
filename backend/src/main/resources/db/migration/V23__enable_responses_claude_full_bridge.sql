UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'OpenAI Responses instructions/input items/function calls/tool outputs/reasoning state/compaction/media/tools/tool_choice/parallel_tool_calls/reasoning effort/text.format/metadata/service_tier -> Claude Messages system/messages/tool_use/tool_result/thinking/compaction/tools/mcp_servers/tool_choice/thinking budget/output_config/metadata/service_tier; bridged Claude thinking state in reasoning.encrypted_content is restored to native signed thinking blocks',
    response_mapping_json = 'OpenAI Responses output/messages/function calls/reasoning/compaction/program/provider-hosted items/usage (cache read and write)/status/stream events/errors -> Claude Messages content/tool_use/thinking/server_tool_use/usage/stop_reason/SSE events/errors',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_RESPONSES'
  AND target_protocol = 'CLAUDE_MESSAGES';

UPDATE protocol_conversion_definitions
SET response_mapping_json = 'Claude Messages content/text/tool_use/thinking/redacted_thinking/compaction/usage (cache read and write)/stop_reason/stream events (signature_delta, response.incomplete) -> OpenAI Responses output/message/function_call/reasoning/compaction/usage/status/SSE events; native thinking signatures and redacted state are tunneled through reasoning.encrypted_content for round-tripping',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'OPENAI_RESPONSES';
