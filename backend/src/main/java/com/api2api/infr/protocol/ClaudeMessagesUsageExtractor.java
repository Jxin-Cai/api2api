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
        long inputTokens = usage.path("input_tokens").asLong(0);
        long outputTokens = usage.path("output_tokens").asLong(0);
        long cacheCreationInputTokens = usage.path("cache_creation_input_tokens").asLong(0);
        long cacheReadInputTokens = usage.path("cache_read_input_tokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens);
    }
}
