package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class OpenAIResponsesUsageExtractor implements UnifiedUsageExtractor {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.OPENAI_RESPONSES;
    }

    @Override
    public UnifiedTokenUsage extract(JsonNode payload) {
        JsonNode usage = payload.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return UnifiedTokenUsage.unknown();
        }
        return extractUsage(usage);
    }

    static UnifiedTokenUsage extractUsage(JsonNode usage) {
        long cacheReadTokens = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
        long cacheWriteTokens = firstPresentLong(
                usage.path("input_tokens_details").get("cache_creation_input_tokens"),
                usage.path("input_tokens_details").get("cache_creation_tokens"),
                usage.path("input_tokens_details").get("cache_write_tokens"),
                usage.get("cache_creation_input_tokens")
        );
        long rawInputTokens = usage.path("input_tokens").asLong(0);
        long inputTokens = Math.max(0, rawInputTokens - cacheReadTokens - cacheWriteTokens);
        long outputTokens = usage.path("output_tokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens);
    }

    private static long firstPresentLong(JsonNode... values) {
        for (JsonNode value : values) {
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                return Math.max(0, value.asLong(0));
            }
        }
        return 0;
    }
}
