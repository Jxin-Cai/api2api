ALTER TABLE model_groups ADD COLUMN allowed_channel_ids BIGINT[] NOT NULL DEFAULT '{}';

-- Snapshot existing access once. Channels created after this migration require explicit permission.
UPDATE model_groups
SET allowed_channel_ids = ARRAY(SELECT id FROM provider_channels WHERE NOT deleted ORDER BY id)
WHERE NOT deleted;
