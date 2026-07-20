-- AWS Bedrock is exposed only through the Converse protocol.
DELETE FROM protocol_conversion_definitions
WHERE source_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES'
   OR target_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES';

DELETE FROM channel_model_supports legacy
WHERE legacy.upstream_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES'
  AND EXISTS (
      SELECT 1
      FROM channel_model_supports converse
      WHERE converse.provider_channel_id = legacy.provider_channel_id
        AND converse.requested_model = legacy.requested_model
        AND converse.upstream_protocol = 'AWS_BEDROCK_CONVERSE'
  );

UPDATE channel_model_supports
SET upstream_protocol = 'AWS_BEDROCK_CONVERSE',
    updated_at = NOW()
WHERE upstream_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES';

DELETE FROM provider_channel_protocol_mappings
WHERE request_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES';

UPDATE provider_channel_protocol_mappings
SET upstream_protocol = 'AWS_BEDROCK_CONVERSE',
    updated_at = NOW()
WHERE upstream_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES';

UPDATE provider_channels
SET supported_protocols = replace(
        supported_protocols,
        'AWS_BEDROCK_CLAUDE_MESSAGES',
        'AWS_BEDROCK_CONVERSE'
    ),
    updated_at = NOW()
WHERE supported_protocols LIKE '%AWS_BEDROCK_CLAUDE_MESSAGES%';

UPDATE usage_records
SET request_protocol = CASE
        WHEN request_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES' THEN 'AWS_BEDROCK_CONVERSE'
        ELSE request_protocol
    END,
    upstream_protocol = CASE
        WHEN upstream_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES' THEN 'AWS_BEDROCK_CONVERSE'
        ELSE upstream_protocol
    END,
    updated_at = NOW()
WHERE request_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES'
   OR upstream_protocol = 'AWS_BEDROCK_CLAUDE_MESSAGES';
