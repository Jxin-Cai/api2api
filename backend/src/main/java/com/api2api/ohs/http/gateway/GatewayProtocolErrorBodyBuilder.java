package com.api2api.ohs.http.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for protocol-compatible error response body construction.
 *
 * <p>Callers are responsible for determining the {@code errorType} value; this class only
 * encodes the JSON structural knowledge for Claude and OpenAI error formats.
 *
 * <ul>
 *   <li>Claude: {@code {"type":"error","error":{"type":"<errorType>","message":"<msg>"}}}
 *   <li>OpenAI: {@code {"error":{"message":"<msg>","type":"<errorType>","param":null,"code":null}}}
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
class GatewayProtocolErrorBodyBuilder {

    @NonNull
    private final ObjectMapper objectMapper;

    String buildClaudeErrorBody(String errorType, String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "error");
            ObjectNode error = objectMapper.createObjectNode();
            error.put("type", errorType == null || errorType.isBlank() ? "invalid_request_error" : errorType);
            error.put("message", message);
            root.set("error", error);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            log.error("event=gateway_error_body_serialization_failed protocol=CLAUDE_MESSAGES", exception);
            return "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Internal server error\"}}";
        }
    }

    String buildOpenAIErrorBody(String errorType, String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", message);
            error.put("type", errorType == null || errorType.isBlank() ? "invalid_request_error" : errorType);
            error.putNull("param");
            error.putNull("code");
            root.set("error", error);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            log.error("event=gateway_error_body_serialization_failed protocol=OPENAI", exception);
            return "{\"error\":{\"message\":\"Internal server error\",\"type\":\"api_error\"}}";
        }
    }
}
