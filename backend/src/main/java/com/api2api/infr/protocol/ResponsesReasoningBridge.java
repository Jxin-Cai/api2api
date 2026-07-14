package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

/**
 * Tunnels opaque OpenAI reasoning state through a Claude thinking signature so a
 * stateless Responses conversation can round-trip it on the next request.
 */
final class ResponsesReasoningBridge {

    static final String SIGNATURE_PREFIX = "api2api-openai-reasoning:v1:";
    static final String ITEM_SIGNATURE_PREFIX = "api2api-openai-item:v1:";

    private ResponsesReasoningBridge() {
    }

    static boolean isResponsesSignature(String signature) {
        return signature != null
                && (signature.startsWith(SIGNATURE_PREFIX) || signature.startsWith(ITEM_SIGNATURE_PREFIX));
    }

    static Optional<String> encode(ObjectMapper objectMapper, JsonNode reasoningItem) {
        if (reasoningItem == null || !reasoningItem.isObject()) {
            return Optional.empty();
        }
        String id = reasoningItem.path("id").asText("");
        String encryptedContent = reasoningItem.path("encrypted_content").asText("");
        if (id.isBlank() || encryptedContent.isBlank()) {
            return Optional.empty();
        }
        try {
            ObjectNode state = objectMapper.createObjectNode();
            state.put("id", id);
            state.put("encrypted_content", encryptedContent);
            return Optional.of(SIGNATURE_PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(state)));
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("RESPONSES_REASONING_STATE_ENCODING_FAILED", exception);
        }
    }

    static Optional<JsonNode> decode(ObjectMapper objectMapper, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(signature.substring(SIGNATURE_PREFIX.length()));
            JsonNode state = objectMapper.readTree(bytes);
            if (state.path("id").asText("").isBlank() || state.path("encrypted_content").asText("").isBlank()) {
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ProtocolConversionException("RESPONSES_REASONING_STATE_DECODING_FAILED", exception);
        }
    }

    /**
     * Preserves provider-hosted Responses items (web search, code interpreter,
     * MCP, etc.) in a Claude thinking signature. Claude has no equivalent
     * content block for these items, but Claude Code will return signed thinking
     * blocks in the next request, allowing the original item to be restored.
     */
    static Optional<String> encodeItem(ObjectMapper objectMapper, JsonNode item) {
        if (item == null || !item.isObject() || item.path("type").asText("").isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ITEM_SIGNATURE_PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(item)));
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("RESPONSES_OUTPUT_ITEM_STATE_ENCODING_FAILED", exception);
        }
    }

    static Optional<JsonNode> decodeItem(ObjectMapper objectMapper, String signature) {
        if (signature == null || !signature.startsWith(ITEM_SIGNATURE_PREFIX)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(signature.substring(ITEM_SIGNATURE_PREFIX.length()));
            JsonNode item = objectMapper.readTree(bytes);
            if (!item.isObject() || item.path("type").asText("").isBlank()) {
                return Optional.empty();
            }
            return Optional.of(item);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ProtocolConversionException("RESPONSES_OUTPUT_ITEM_STATE_DECODING_FAILED", exception);
        }
    }
}
