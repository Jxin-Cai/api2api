UPDATE usage_records
SET ended_at = started_at
WHERE ended_at IS NULL;

UPDATE usage_records
SET ended_at = started_at
WHERE ended_at < started_at;

UPDATE usage_records
SET duration_millis = GREATEST(
    0,
    FLOOR(EXTRACT(EPOCH FROM (ended_at - started_at)) * 1000)::BIGINT
)
WHERE duration_millis <> GREATEST(
    0,
    FLOOR(EXTRACT(EPOCH FROM (ended_at - started_at)) * 1000)::BIGINT
);
