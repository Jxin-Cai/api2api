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
        long cacheReadTokens = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
        long cacheWriteTokens = usage.path("input_tokens_details").path("cache_write_tokens").asLong(0);
        long rawInputTokens = usage.path("input_tokens").asLong(0);
        long inputTokens = Math.max(0, rawInputTokens - cacheReadTokens - cacheWriteTokens);
        long outputTokens = usage.path("output_tokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens);
    }
}
