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
        return extractUsageNode(usage);
    }

    static UnifiedTokenUsage extractUsageNode(JsonNode usage) {
        if (usage == null || !usage.isObject()) {
            return UnifiedTokenUsage.unknown();
        }
        IterationUsage iterationUsage = sumIterations(usage.path("iterations"));
        long inputTokens = iterationUsage.present()
                ? iterationUsage.inputTokens()
                : firstPositiveLong(usage.get("input_tokens"), usage.get("prompt_tokens"));
        long outputTokens = iterationUsage.present()
                ? iterationUsage.outputTokens()
                : firstPositiveLong(usage.get("output_tokens"), usage.get("completion_tokens"));
        long cacheCreationInputTokens = iterationUsage.cacheCreationInputTokens() > 0
                ? iterationUsage.cacheCreationInputTokens()
                : firstPositiveLong(usage.get("cache_creation_input_tokens"));
        if (cacheCreationInputTokens == 0) {
            cacheCreationInputTokens = sumCacheCreationDetails(usage.path("cache_creation"));
        }
        long cacheReadInputTokens = iterationUsage.cacheReadInputTokens() > 0
                ? iterationUsage.cacheReadInputTokens()
                : firstPositiveLong(usage.get("cache_read_input_tokens"), usage.get("cached_tokens"));
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens);
    }

    private static IterationUsage sumIterations(JsonNode iterations) {
        if (!iterations.isArray() || iterations.isEmpty()) {
            return IterationUsage.empty();
        }
        long inputTokens = 0;
        long outputTokens = 0;
        long cacheCreationInputTokens = 0;
        long cacheReadInputTokens = 0;
        for (JsonNode iteration : iterations) {
            inputTokens += nonNegativeLong(iteration.path("input_tokens"));
            outputTokens += nonNegativeLong(iteration.path("output_tokens"));
            cacheCreationInputTokens += nonNegativeLong(iteration.path("cache_creation_input_tokens"));
            cacheReadInputTokens += nonNegativeLong(iteration.path("cache_read_input_tokens"));
        }
        return new IterationUsage(
                true,
                inputTokens,
                outputTokens,
                cacheCreationInputTokens,
                cacheReadInputTokens
        );
    }

    private static long nonNegativeLong(JsonNode value) {
        return Math.max(0, value.asLong(0));
    }

    private static long sumCacheCreationDetails(JsonNode cacheCreation) {
        return Math.max(0, cacheCreation.path("ephemeral_5m_input_tokens").asLong(0)
                + cacheCreation.path("ephemeral_1h_input_tokens").asLong(0));
    }

    static long firstPositiveLong(JsonNode... values) {
        for (JsonNode value : values) {
            if (value != null && !value.isNull() && !value.isMissingNode() && value.asLong(0) > 0) {
                return value.asLong();
            }
        }
        return 0;
    }

    private record IterationUsage(
            boolean present,
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens
    ) {
        private static IterationUsage empty() {
            return new IterationUsage(false, 0, 0, 0, 0);
        }
    }
}
