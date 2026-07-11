UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_tool_calling = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'Claude messages/system/mid_conv_system/media/documents/tools/tool_choice/thinking/output_config/cache_control/metadata/service_tier/speed -> Bedrock Converse messages/system/toolConfig/additionalModelRequestFields/outputConfig/requestMetadata/serviceTier/performanceConfig; model is mapped to the Converse URI',
    response_mapping_json = 'Bedrock Converse output/toolUse/toolResult/reasoningContent/citations/searchResult/usage/stopReason/stop_sequence/stream events/errors -> Claude Messages content/usage/stop_reason/stop_sequence/SSE events/errors',
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
    request_mapping_json = 'Claude messages/system/media/documents/custom tools/web search/code execution/MCP/tool_choice/thinking/output_config/context_management/metadata/service_tier/speed -> OpenAI Responses input/tools/tool_choice/reasoning/text/context_management/metadata/service_tier; opaque provider-hosted state is round-tripped in signed thinking blocks',
    response_mapping_json = 'OpenAI Responses output/messages/function calls/reasoning/compaction/provider-hosted items/usage/status/stream events/errors -> Claude Messages content/tool_use/thinking/compaction/usage/stop_reason/SSE events/errors',
    updated_at = NOW()
WHERE source_protocol = 'CLAUDE_MESSAGES'
  AND target_protocol = 'OPENAI_RESPONSES';
