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
        return extractUsage(usage);
    }

    static UnifiedTokenUsage extractUsage(JsonNode usageNode) {
        JsonNode details = usageNode.path("prompt_tokens_details");
        long cacheReadTokens = details.path("cached_tokens").asLong(0);
        // cache_write_tokens 与 cache_creation_tokens 是不同上游对同一语义的两种命名，取其一，禁止相加
        long cacheWriteTokens = details.path("cache_write_tokens").asLong(0);
        if (cacheWriteTokens <= 0) {
            cacheWriteTokens = details.path("cache_creation_tokens").asLong(0);
        }
        long promptTokens = usageNode.path("prompt_tokens").asLong(0);
        long inputTokens = Math.max(0, promptTokens - cacheReadTokens - cacheWriteTokens);
        long outputTokens = usageNode.path("completion_tokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens);
    }
}
