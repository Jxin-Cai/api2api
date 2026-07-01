ALTER TABLE provider_channels
    ADD COLUMN IF NOT EXISTS models_path TEXT NOT NULL DEFAULT '/v1/models';

CREATE TABLE IF NOT EXISTS provider_channel_protocol_mappings (
    provider_channel_id BIGINT NOT NULL REFERENCES provider_channels(id) ON DELETE CASCADE,
    request_protocol VARCHAR(64) NOT NULL,
    upstream_protocol VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (provider_channel_id, request_protocol)
);

INSERT INTO provider_channel_protocol_mappings (provider_channel_id, request_protocol, upstream_protocol, created_at, updated_at)
SELECT channel.id,
       btrim(protocol.value) AS request_protocol,
       btrim(protocol.value) AS upstream_protocol,
       channel.created_at,
       channel.updated_at
FROM provider_channels channel
CROSS JOIN LATERAL regexp_split_to_table(channel.supported_protocols, ',') AS protocol(value)
WHERE btrim(protocol.value) <> ''
ON CONFLICT (provider_channel_id, request_protocol) DO NOTHING;
