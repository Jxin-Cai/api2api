package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/** Removes provider-bound Claude state when it is not needed by the current request. */
final class ClaudeRequestSanitizer {

    private ClaudeRequestSanitizer() {
    }

    static ProtocolPayload sanitize(ObjectMapper objectMapper, ProtocolPayload payload) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.protocol() != ProtocolType.CLAUDE_MESSAGES) {
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
}
