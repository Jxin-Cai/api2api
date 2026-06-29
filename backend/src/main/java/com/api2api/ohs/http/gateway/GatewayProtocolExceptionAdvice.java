package com.api2api.ohs.http.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gateway-scoped exception handler that returns protocol-compatible error bodies,
 * keeping SDK endpoints isolated from management API ApiResponse formatting.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = GatewayProtocolController.class)
@RequiredArgsConstructor
public class GatewayProtocolExceptionAdvice {

    @NonNull
    private final ObjectMapper objectMapper;

    @ExceptionHandler(GatewayProtocolException.class)
    public ResponseEntity<String> handleGatewayProtocol(GatewayProtocolException exception) {
        return ResponseEntity.status(exception.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildErrorBody(exception.protocol(), exception.errorType(), exception.getMessage()));
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<String> handleBadRequest(RuntimeException exception) {
        String message = exception.getMessage() == null ? "Invalid request" : exception.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildErrorBody(ProtocolType.OPENAI_CHAT_COMPLETIONS, "invalid_request_error", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception exception) {
        log.error("Unexpected gateway error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildErrorBody(ProtocolType.OPENAI_CHAT_COMPLETIONS, "api_error", "Internal server error"));
    }

    private String buildErrorBody(ProtocolType protocol, String errorType, String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode error = objectMapper.createObjectNode();
            if (protocol == ProtocolType.CLAUDE_MESSAGES) {
                error.put("type", "error");
                error.put("message", message);
            } else {
                error.put("message", message);
                error.put("type", errorType);
                error.putNull("param");
                error.putNull("code");
            }
            root.set("error", error);
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return "{\"error\":\"Internal server error\"}";
        }
    }
}
