package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class OpenAIImagesUsageExtractor implements UnifiedUsageExtractor {

    @Override
    public ProtocolType protocol() {
        return ProtocolType.OPENAI_IMAGES;
    }

    @Override
    public UnifiedTokenUsage extract(JsonNode payload) {
        JsonNode usage = payload.path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            return UnifiedTokenUsage.unknown();
        }
        return extractUsage(usage);
    }

    /**
     * Images usage carries no prompt-cache fields, so cache write/read are always zero. dall-e
     * models return no usage object at all; that case is handled by the caller as unknown usage.
     */
    static UnifiedTokenUsage extractUsage(JsonNode usage) {
        long inputTokens = usage.path("input_tokens").asLong(0);
        long outputTokens = usage.path("output_tokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, 0, 0);
    }
}
