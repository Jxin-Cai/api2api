package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * Tunnels native Claude thinking state (signed thinking and redacted_thinking
 * blocks) through the OpenAI Responses {@code reasoning.encrypted_content}
 * field so a stateless Responses client can round-trip it on the next request.
 *
 * <p>Mirror image of {@link ResponsesReasoningBridge}: that bridge carries
 * OpenAI state through Claude signatures, this one carries Claude state
 * through OpenAI encrypted content.</p>
 */
final class ClaudeThinkingStateBridge {

    static final String STATE_PREFIX = "api2api-claude-thinking:v1:";

    private static final String THINKING_TYPE = "thinking";
    private static final String REDACTED_THINKING_TYPE = "redacted_thinking";
    private static final Set<String> BRIDGEABLE_TYPES = Set.of(THINKING_TYPE, REDACTED_THINKING_TYPE);

    private ClaudeThinkingStateBridge() {
    }

    static boolean isBridgedState(String encryptedContent) {
        return encryptedContent != null && encryptedContent.startsWith(STATE_PREFIX);
    }

    /**
     * Encodes a Claude thinking or redacted_thinking content block into an
     * opaque encrypted_content value. Returns empty when the block carries no
     * replayable state (no signature and no redacted data).
     */
    static Optional<String> encode(ObjectMapper objectMapper, JsonNode claudeBlock) {
        if (claudeBlock == null || !claudeBlock.isObject()) {
            return Optional.empty();
        }
        String type = claudeBlock.path("type").asText("");
        if (!BRIDGEABLE_TYPES.contains(type)) {
            return Optional.empty();
        }
        ObjectNode state = objectMapper.createObjectNode();
        state.put("type", type);
        if (THINKING_TYPE.equals(type)) {
            String signature = claudeBlock.path("signature").asText("");
            if (signature.isBlank()) {
                return Optional.empty();
            }
            state.put("thinking", claudeBlock.path("thinking").asText(""));
            state.put("signature", signature);
        } else {
            String data = claudeBlock.path("data").asText("");
            if (data.isBlank()) {
                return Optional.empty();
            }
            state.put("data", data);
        }
        try {
            return Optional.of(STATE_PREFIX + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(state)));
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("CLAUDE_THINKING_STATE_ENCODING_FAILED", exception);
        }
    }

    /**
     * Decodes a bridged encrypted_content value back into the original Claude
     * thinking or redacted_thinking content block.
     */
    static Optional<JsonNode> decode(ObjectMapper objectMapper, String encryptedContent) {
        if (!isBridgedState(encryptedContent)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encryptedContent.substring(STATE_PREFIX.length()));
            JsonNode state = objectMapper.readTree(bytes);
            String type = state.path("type").asText("");
            if (!BRIDGEABLE_TYPES.contains(type)) {
                return Optional.empty();
            }
            ObjectNode block = objectMapper.createObjectNode();
            block.put("type", type);
            if (THINKING_TYPE.equals(type)) {
                if (state.path("signature").asText("").isBlank()) {
                    return Optional.empty();
                }
                block.put("thinking", state.path("thinking").asText(""));
                block.put("signature", state.path("signature").asText());
            } else {
                if (state.path("data").asText("").isBlank()) {
                    return Optional.empty();
                }
                block.put("data", state.path("data").asText());
            }
            return Optional.of(block);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ProtocolConversionException("CLAUDE_THINKING_STATE_DECODING_FAILED", exception);
        }
    }
}
