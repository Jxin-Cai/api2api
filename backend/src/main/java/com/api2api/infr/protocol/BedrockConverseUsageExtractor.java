package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class BedrockConverseUsageExtractor implements UnifiedUsageExtractor {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.AWS_BEDROCK_CONVERSE;
    }

    @Override
    public UnifiedTokenUsage extract(JsonNode payload) {
        JsonNode usage = payload.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return UnifiedTokenUsage.unknown();
        }
        long inputTokens = usage.path("inputTokens").asLong(0);
        long outputTokens = usage.path("outputTokens").asLong(0);
        long cacheWriteInputTokens = usage.path("cacheWriteInputTokens").asLong(0);
        long cacheReadInputTokens = usage.path("cacheReadInputTokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteInputTokens, cacheReadInputTokens);
    }
}
