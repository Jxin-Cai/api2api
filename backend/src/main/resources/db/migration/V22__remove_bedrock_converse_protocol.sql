-- Remove the retired AWS Bedrock Converse protocol from active conversion and channel data.
DELETE FROM protocol_conversion_definitions
WHERE source_protocol = 'AWS_BEDROCK_CONVERSE'
   OR target_protocol = 'AWS_BEDROCK_CONVERSE';

DELETE FROM channel_model_supports
WHERE upstream_protocol = 'AWS_BEDROCK_CONVERSE';

DELETE FROM provider_channel_protocol_mappings
WHERE request_protocol = 'AWS_BEDROCK_CONVERSE'
   OR upstream_protocol = 'AWS_BEDROCK_CONVERSE';

UPDATE provider_channels channel
SET supported_protocols = COALESCE(
        (
            SELECT string_agg(btrim(protocol.value), ',' ORDER BY protocol.ordinality)
            FROM unnest(string_to_array(channel.supported_protocols, ','))
                 WITH ORDINALITY AS protocol(value, ordinality)
            WHERE btrim(protocol.value) <> 'AWS_BEDROCK_CONVERSE'
              AND btrim(protocol.value) <> ''
        ),
        ''
    ),
    updated_at = NOW()
WHERE supported_protocols LIKE '%AWS_BEDROCK_CONVERSE%';

-- Historical usage remains queryable after the enum value is removed.
UPDATE usage_records
SET request_protocol = CASE
        WHEN request_protocol = 'AWS_BEDROCK_CONVERSE' THEN 'AWS_BEDROCK_CLAUDE_MESSAGES'
        ELSE request_protocol
    END,
    upstream_protocol = CASE
        WHEN upstream_protocol = 'AWS_BEDROCK_CONVERSE' THEN 'AWS_BEDROCK_CLAUDE_MESSAGES'
        ELSE upstream_protocol
    END,
    updated_at = NOW()
WHERE request_protocol = 'AWS_BEDROCK_CONVERSE'
   OR upstream_protocol = 'AWS_BEDROCK_CONVERSE';
