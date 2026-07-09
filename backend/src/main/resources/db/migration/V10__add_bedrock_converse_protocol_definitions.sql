-- Add AWS Bedrock Converse protocol conversion definitions
-- Bedrock is upstream-only (never client-facing), so we need 6 definitions:
-- 3 client protocols -> BEDROCK (request direction)
-- BEDROCK -> 3 client protocols (response direction, handled by same definition pair)

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
    (10, 'CLAUDE_MESSAGES', 'AWS_BEDROCK_CONVERSE', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED', FALSE, FALSE, FALSE, TRUE, TRUE,
     'Claude messages/system/model/max_tokens -> Bedrock messages/system/inferenceConfig',
     'Bedrock output/usage/stopReason -> Claude messages content/usage/stop_reason',
     NOW(), NOW()),
    (11, 'AWS_BEDROCK_CONVERSE', 'CLAUDE_MESSAGES', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED', FALSE, FALSE, FALSE, TRUE, TRUE,
     'Bedrock output/usage/stopReason -> Claude messages content/usage/stop_reason',
     'Claude messages/system/model/max_tokens -> Bedrock messages/system/inferenceConfig',
     NOW(), NOW()),
    (12, 'OPENAI_CHAT_COMPLETIONS', 'AWS_BEDROCK_CONVERSE', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED', FALSE, FALSE, FALSE, TRUE, TRUE,
     'OpenAI chat messages/model/max_tokens -> Bedrock messages/system/inferenceConfig',
     'Bedrock output/usage/stopReason -> OpenAI chat choices/usage/finish_reason',
     NOW(), NOW()),
    (13, 'AWS_BEDROCK_CONVERSE', 'OPENAI_CHAT_COMPLETIONS', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED', FALSE, FALSE, FALSE, TRUE, TRUE,
     'Bedrock output/usage/stopReason -> OpenAI chat choices/usage/finish_reason',
     'OpenAI chat messages/model/max_tokens -> Bedrock messages/system/inferenceConfig',
     NOW(), NOW()),
    (14, 'OPENAI_RESPONSES', 'AWS_BEDROCK_CONVERSE', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED', FALSE, FALSE, FALSE, TRUE, TRUE,
     'OpenAI responses input/instructions/model/max_output_tokens -> Bedrock messages/system/inferenceConfig',
     'Bedrock output/usage/stopReason -> OpenAI responses output/usage',
     NOW(), NOW()),
    (15, 'AWS_BEDROCK_CONVERSE', 'OPENAI_RESPONSES', 'TRANSFORM', 'ENABLED', 'IMPLEMENTED', FALSE, FALSE, FALSE, TRUE, TRUE,
     'Bedrock output/usage/stopReason -> OpenAI responses output/usage',
     'OpenAI responses input/instructions/model/max_output_tokens -> Bedrock messages/system/inferenceConfig',
     NOW(), NOW())
ON CONFLICT (source_protocol, target_protocol) DO NOTHING;
