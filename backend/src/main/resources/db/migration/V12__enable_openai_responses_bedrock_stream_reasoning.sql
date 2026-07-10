UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'OpenAI responses input/instructions/reasoning -> Bedrock messages/system/inferenceConfig/additionalModelRequestFields; streaming handled by ConverseStream endpoint',
    response_mapping_json = 'Bedrock output/usage/reasoningContent/stream events -> OpenAI responses output/usage/SSE events',
    updated_at = NOW()
WHERE source_protocol = 'OPENAI_RESPONSES'
  AND target_protocol = 'AWS_BEDROCK_CONVERSE';

UPDATE protocol_conversion_definitions
SET supports_streaming = TRUE,
    supports_reasoning = TRUE,
    supports_usage_mapping = TRUE,
    supports_cache_token_mapping = TRUE,
    implementation_status = 'IMPLEMENTED',
    status = 'ENABLED',
    request_mapping_json = 'Bedrock output/usage/reasoningContent/stream events -> OpenAI responses output/usage/SSE events',
    response_mapping_json = 'OpenAI responses input/instructions/reasoning -> Bedrock messages/system/inferenceConfig/additionalModelRequestFields; streaming handled by ConverseStream endpoint',
    updated_at = NOW()
WHERE source_protocol = 'AWS_BEDROCK_CONVERSE'
  AND target_protocol = 'OPENAI_RESPONSES';
