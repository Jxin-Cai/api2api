package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/** Removes provider-bound Claude state when it is not needed by the current request. */
final class ClaudeRequestSanitizer {

    private ClaudeRequestSanitizer() {
    }

    static ProtocolPayload sanitize(
            ObjectMapper objectMapper,
            ProtocolPayload payload,
            ProtocolType targetProtocol
    ) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        if (payload.protocol() != ProtocolType.CLAUDE_MESSAGES
                || targetProtocol == ProtocolType.AWS_BEDROCK_CONVERSE
                || targetProtocol == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            return payload;
        }
        try {
            JsonNode source = objectMapper.readTree(payload.body());
            if (!source.isObject()) {
                return payload;
            }
            JsonNode messages = source.get("messages");
            JsonNode sanitizedMessages = ClaudeConversationContextOptimizer.sanitizeCompactionHistory(
                    messages, source.get("context_management"));
            if (targetProtocol == ProtocolType.CLAUDE_MESSAGES) {
                sanitizedMessages = removeForeignThinkingState(sanitizedMessages);
            }
            if (sanitizedMessages == messages) {
                return payload;
            }
            ObjectNode sanitized = ((ObjectNode) source).deepCopy();
            sanitized.set("messages", sanitizedMessages);
            return ProtocolPayload.of(
                    payload.protocol(),
                    objectMapper.writeValueAsString(sanitized),
                    payload.streaming()
            );
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("CLAUDE_REQUEST_SANITIZATION_FAILED", exception);
        }
    }

    private static JsonNode removeForeignThinkingState(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return messages;
        }
        ArrayNode sanitized = null;
        for (int messageIndex = 0; messageIndex < messages.size(); messageIndex++) {
            JsonNode message = messages.get(messageIndex);
            if (!"assistant".equals(message.path("role").asText(""))) {
                continue;
            }
            JsonNode content = message.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (int contentIndex = content.size() - 1; contentIndex >= 0; contentIndex--) {
                JsonNode block = content.get(contentIndex);
                if (!"thinking".equals(block.path("type").asText(""))
                        || !isForeignThinkingSignature(block.path("signature").asText(""))) {
                    continue;
                }
                if (sanitized == null) {
                    sanitized = ((ArrayNode) messages).deepCopy();
                }
                ((ArrayNode) sanitized.get(messageIndex).path("content")).remove(contentIndex);
            }
        }
        return sanitized == null ? messages : sanitized;
    }

    private static boolean isForeignThinkingSignature(String signature) {
        return ResponsesReasoningBridge.isResponsesSignature(signature)
                || BedrockReasoningBridge.isBedrockSignature(signature);
    }
}
