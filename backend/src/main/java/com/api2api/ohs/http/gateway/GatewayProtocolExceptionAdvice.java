package com.api2api.ohs.http.gateway;

import com.api2api.application.BusinessException;
import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
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

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusiness(BusinessException exception, HttpServletRequest request) {
        ProtocolType protocol = protocolOf(request);
        ErrorMapping mapping = mapBusinessCode(exception.code());
        return ResponseEntity.status(mapping.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildErrorBody(protocol, mapping.errorType(), exception.getMessage()));
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class, MissingRequestHeaderException.class })
    public ResponseEntity<String> handleBadRequest(Exception exception, HttpServletRequest request) {
        String message = exception.getMessage() == null ? "Invalid request" : exception.getMessage();
        ProtocolType protocol = protocolOf(request);
        ErrorMapping mapping = mapBadRequestMessage(message);
        return ResponseEntity.status(mapping.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildErrorBody(protocol, mapping.errorType(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected gateway error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildErrorBody(protocolOf(request), "api_error", "Internal server error"));
    }

    private ProtocolType protocolOf(HttpServletRequest request) {
        String uri = request == null ? "" : request.getRequestURI();
        if (uri != null) {
            if (uri.endsWith("/v1/messages")) {
                return ProtocolType.CLAUDE_MESSAGES;
            }
            if (uri.endsWith("/v1/responses")) {
                return ProtocolType.OPENAI_RESPONSES;
            }
            if (uri.endsWith("/v1/chat/completions")) {
                return ProtocolType.OPENAI_CHAT_COMPLETIONS;
            }
        }
        return ProtocolType.OPENAI_CHAT_COMPLETIONS;
    }

    private ErrorMapping mapBusinessCode(String code) {
        return switch (normalizeCode(code)) {
            case "API_CREDENTIAL_INVALID" -> new ErrorMapping(HttpStatus.UNAUTHORIZED, "authentication_error");
            case "API_CREDENTIAL_DISABLED", "MODEL_NOT_ALLOWED" -> new ErrorMapping(HttpStatus.FORBIDDEN, "permission_error");
            case "TOKEN_QUOTA_EXHAUSTED" -> new ErrorMapping(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_error");
            case "INVALID_TOKEN_TOTAL" -> new ErrorMapping(HttpStatus.INTERNAL_SERVER_ERROR, "api_error");
            default -> new ErrorMapping(HttpStatus.BAD_REQUEST, "invalid_request_error");
        };
    }

    private ErrorMapping mapBadRequestMessage(String message) {
        return switch (normalizeCode(message)) {
            case "API_CREDENTIAL_DISABLED", "MODEL_NOT_ALLOWED" -> new ErrorMapping(HttpStatus.FORBIDDEN, "permission_error");
            case "TOKEN_QUOTA_EXHAUSTED" -> new ErrorMapping(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_error");
            default -> new ErrorMapping(HttpStatus.BAD_REQUEST, "invalid_request_error");
        };
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        int separatorIndex = trimmed.indexOf(':');
        if (separatorIndex >= 0) {
            trimmed = trimmed.substring(0, separatorIndex);
        }
        return trimmed.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String buildErrorBody(ProtocolType protocol, String errorType, String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode error = objectMapper.createObjectNode();
            if (protocol == ProtocolType.CLAUDE_MESSAGES) {
                root.put("type", "error");
                error.put("type", errorType == null || errorType.isBlank() ? "invalid_request_error" : errorType);
                error.put("message", message);
            } else {
                error.put("message", message);
                error.put("type", mapErrorTypeForOpenAI(errorType));
                error.putNull("param");
                error.putNull("code");
            }
            root.set("error", error);
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return "{\"error\":\"Internal server error\"}";
        }
    }

    private String mapErrorTypeForOpenAI(String errorType) {
        if ("rate_limit_error".equals(errorType)) {
            return "rate_limit_error";
        }
        if ("api_error".equals(errorType)) {
            return "api_error";
        }
        return "invalid_request_error";
    }

    private record ErrorMapping(HttpStatus status, String errorType) {
    }
}
