ALTER TABLE provider_channels
    ADD COLUMN IF NOT EXISTS route_priority INTEGER NOT NULL DEFAULT 0;

ALTER TABLE provider_channels
    ALTER COLUMN key_ref TYPE TEXT;

ALTER TABLE channel_model_supports
    ADD COLUMN IF NOT EXISTS preferred BOOLEAN NOT NULL DEFAULT FALSE;
