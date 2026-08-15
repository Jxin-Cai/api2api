package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared infrastructure for emitting OpenAI Responses SSE events.
 *
 * <p>Extracted from {@link ChatCompletionsToResponsesStreamingConverter} and
 * {@link ClaudeMessagesToResponsesStreamingConverter} to eliminate duplication of
 * protocol-level formatting logic.</p>
 */
final class ResponsesSseEmitter {

    private final ObjectMapper objectMapper;

    ResponsesSseEmitter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * Writes a single SSE event with the given type and payload.
     * Mutates the event node by inserting {@code type} and {@code sequence_number}.
     *
     * @param type           Responses event type (e.g. "response.created")
     * @param event          the payload node (will be mutated)
     * @param sequenceHolder single-element int array holding the current sequence number;
     *                       incremented after use
     * @param output         the output stream to write to
     */
    void writeEvent(
            String type,
            ObjectNode event,
            int[] sequenceHolder,
            OutputStream output
    ) throws IOException {
        event.put("type", type);
        event.put("sequence_number", sequenceHolder[0]++);
        output.write(("event: " + type + "\n").getBytes(StandardCharsets.UTF_8));
        output.write(("data: " + objectMapper.writeValueAsString(event) + "\n\n")
                .getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    /**
     * Builds the base response object shared by {@code response.created} and
     * {@code response.completed} events.
     */
    ObjectNode baseResponse(String responseId, long createdAt, String model) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", responseId);
        response.put("object", "response");
        response.put("created_at", createdAt);
        response.put("model", model);
        return response;
    }

    /**
     * Builds a Responses-protocol usage node from token counts.
     */
    ObjectNode responsesUsage(
            long inputTokens,
            long outputTokens,
            long cacheCreationInputTokens,
            long cacheReadInputTokens
    ) {
        long totalInput = inputTokens + cacheReadInputTokens + cacheCreationInputTokens;
        ObjectNode usage = objectMapper.createObjectNode();
        usage.put("input_tokens", totalInput);
        usage.put("output_tokens", outputTokens);
        usage.put("total_tokens", totalInput + outputTokens);
        if (cacheReadInputTokens > 0 || cacheCreationInputTokens > 0) {
            ObjectNode details = objectMapper.createObjectNode();
            details.put("cached_tokens", cacheReadInputTokens);
            if (cacheCreationInputTokens > 0) {
                details.put("cache_write_tokens", cacheCreationInputTokens);
            }
            usage.set("input_tokens_details", details);
        }
        return usage;
    }

    /**
     * Generates a unique item ID with the given prefix (e.g. "msg", "rs", "fc", "call").
     */
    String itemId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /**
     * Collects all output text from message items in the completed output array.
     * Handles both "output_text" and "refusal" content part types.
     */
    String collectOutputText(ArrayNode output) {
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText(""))) {
                continue;
            }
            for (JsonNode part : item.path("content")) {
                text.append("refusal".equals(part.path("type").asText(""))
                        ? part.path("refusal").asText("")
                        : part.path("text").asText(""));
            }
        }
        return text.toString();
    }
}
