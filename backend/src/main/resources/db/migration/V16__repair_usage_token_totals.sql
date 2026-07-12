UPDATE usage_records
SET total_tokens = input_tokens
        + output_tokens
        + cache_creation_input_tokens
        + cache_read_input_tokens,
    updated_at = CURRENT_TIMESTAMP
WHERE total_tokens <> input_tokens
        + output_tokens
        + cache_creation_input_tokens
        + cache_read_input_tokens;
