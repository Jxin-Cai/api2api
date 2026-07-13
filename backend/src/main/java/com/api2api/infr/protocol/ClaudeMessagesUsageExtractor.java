package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class ClaudeMessagesUsageExtractor implements UnifiedUsageExtractor {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.CLAUDE_MESSAGES;
    }

    @Override
    public UnifiedTokenUsage extract(JsonNode payload) {
        JsonNode usage = payload.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return UnifiedTokenUsage.unknown();
        }
        long inputTokens = firstPositiveLong(usage.get("input_tokens"), usage.get("prompt_tokens"));
        long outputTokens = firstPositiveLong(usage.get("output_tokens"), usage.get("completion_tokens"));
        long cacheCreationInputTokens = firstPositiveLong(usage.get("cache_creation_input_tokens"));
        if (cacheCreationInputTokens == 0) {
            cacheCreationInputTokens = sumCacheCreationDetails(usage.path("cache_creation"));
        }
        long cacheReadInputTokens = firstPositiveLong(
                usage.get("cache_read_input_tokens"),
                usage.get("cached_tokens")
        );
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens);
    }

    private static long sumCacheCreationDetails(JsonNode cacheCreation) {
        return Math.max(0, cacheCreation.path("ephemeral_5m_input_tokens").asLong(0)
                + cacheCreation.path("ephemeral_1h_input_tokens").asLong(0));
    }

    private static long firstPositiveLong(JsonNode... values) {
        for (JsonNode value : values) {
            if (value != null && !value.isNull() && !value.isMissingNode() && value.asLong(0) > 0) {
                return value.asLong();
            }
        }
        return 0;
    }
}
