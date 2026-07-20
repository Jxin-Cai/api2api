package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class OpenAIChatCompletionsUsageExtractor implements UnifiedUsageExtractor {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.OPENAI_CHAT_COMPLETIONS;
    }

    @Override
    public UnifiedTokenUsage extract(JsonNode payload) {
        JsonNode usage = payload.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return UnifiedTokenUsage.unknown();
        }
        JsonNode details = usage.path("prompt_tokens_details");
        long cacheReadTokens = details.path("cached_tokens").asLong(0);
        long cacheWriteTokens = details.path("cache_creation_tokens").asLong(0)
                + details.path("cache_write_tokens").asLong(0);
        long promptTokens = usage.path("prompt_tokens").asLong(0);
        long inputTokens = Math.max(0, promptTokens - cacheReadTokens - cacheWriteTokens);
        long outputTokens = usage.path("completion_tokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens);
    }
}
