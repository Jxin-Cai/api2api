package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/** Translates response usage fields while preserving each protocol's cache accounting. */
final class ProtocolResponseUsageMapper {

    private final ProtocolJsonSupport json;

    ProtocolResponseUsageMapper(ProtocolJsonSupport json) {
        this.json = Objects.requireNonNull(json, "json must not be null");
    }

    private record RawTokenUsage(long input, long output, long cacheRead, long cacheWrite) {
        static RawTokenUsage fromClaude(JsonNode usage) {
            long cacheCreation = usage.path("cache_creation_input_tokens").asLong(0);
            long cacheRead = usage.path("cache_read_input_tokens").asLong(0);
            long input = usage.path("input_tokens").asLong(0) + cacheCreation + cacheRead;
            long output = usage.path("output_tokens").asLong(0);
            return new RawTokenUsage(input, output, cacheRead, cacheCreation);
        }

        static RawTokenUsage fromChat(JsonNode usage) {
            JsonNode details = usage.path("prompt_tokens_details");
            long cached = details.path("cached_tokens").asLong(0);
            long cacheWrite = OpenAIChatCompletionsUsageExtractor.cacheWriteTokens(details);
            long input = usage.path("prompt_tokens").asLong(0);
            long output = usage.path("completion_tokens").asLong(0);
            return new RawTokenUsage(input, output, cached, cacheWrite);
        }

        static RawTokenUsage fromResponses(JsonNode usage) {
            long cached = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
            long cacheWrite = usage.path("input_tokens_details").path("cache_write_tokens").asLong(0);
            long input = usage.path("input_tokens").asLong(0);
            long output = usage.path("output_tokens").asLong(0);
            return new RawTokenUsage(input, output, cached, cacheWrite);
        }
    }

    private ObjectNode toChatUsage(RawTokenUsage raw, boolean includeCacheWrite) {
        ObjectNode target = json.objectNode();
        target.put("prompt_tokens", raw.input());
        target.put("completion_tokens", raw.output());
        target.put("total_tokens", raw.input() + raw.output());
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", raw.cacheRead());
        if (includeCacheWrite) {
            details.put("cache_write_tokens", raw.cacheWrite());
        }
        target.set("prompt_tokens_details", details);
        return target;
    }

    private ObjectNode toResponsesUsage(RawTokenUsage raw) {
        ObjectNode target = json.objectNode();
        target.put("input_tokens", raw.input());
        target.put("output_tokens", raw.output());
        target.put("total_tokens", raw.input() + raw.output());
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", raw.cacheRead());
        if (raw.cacheWrite() > 0) {
            details.put("cache_write_tokens", raw.cacheWrite());
        }
        target.set("input_tokens_details", details);
        return target;
    }

    private ObjectNode toClaudeUsage(RawTokenUsage raw) {
        ObjectNode target = json.objectNode();
        target.put("input_tokens", Math.max(0, raw.input() - raw.cacheRead() - raw.cacheWrite()));
        target.put("output_tokens", raw.output());
        target.put("cache_creation_input_tokens", raw.cacheWrite());
        target.put("cache_read_input_tokens", raw.cacheRead());
        return target;
    }

    ObjectNode chatUsageFromClaude(JsonNode usage) {
        return toChatUsage(RawTokenUsage.fromClaude(usage), true);
    }

    ObjectNode responsesUsageFromClaude(JsonNode usage) {
        return toResponsesUsage(RawTokenUsage.fromClaude(usage));
    }

    ObjectNode claudeUsageFromChat(JsonNode usage) {
        return toClaudeUsage(RawTokenUsage.fromChat(usage));
    }

    ObjectNode responsesUsageFromChat(JsonNode usage) {
        return toResponsesUsage(RawTokenUsage.fromChat(usage));
    }

    ObjectNode chatUsageFromResponses(JsonNode usage) {
        return toChatUsage(RawTokenUsage.fromResponses(usage), false);
    }

    ObjectNode claudeUsageFromResponses(JsonNode usage) {
        return toClaudeUsage(RawTokenUsage.fromResponses(usage));
    }
}
