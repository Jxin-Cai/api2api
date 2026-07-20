package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationOutcome;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.gateway.model.ConversionTrace;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.GatewayInvocationResult;
import com.api2api.domain.gateway.model.InvocationError;
import com.api2api.domain.gateway.model.InvocationErrorType;
import com.api2api.domain.gateway.model.InvocationStatus;
import com.api2api.domain.protocol.model.ConversionResult;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RouteFailureType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Maps GatewayInvocation to protocol-compatible raw response.
 */
@Component
@RequiredArgsConstructor
public class GatewayInvocationResponseMapper {

    @NonNull
    private final ObjectMapper objectMapper;

    public GatewayRawResponse toRawResponse(GatewayInvocation invocation) {
        Objects.requireNonNull(invocation, "Gateway invocation must not be null");
        return toRawResponse(GatewayInvocationOutcome.withoutProviderResponse(invocation));
    }

    public GatewayRawResponse toRawResponse(GatewayInvocationOutcome outcome) {
        Objects.requireNonNull(outcome, "Gateway invocation outcome must not be null");
        GatewayInvocation invocation = outcome.invocation();

        GatewayInvocationResult result = invocation.result();
        if (result == null) {
            throw new IllegalArgumentException("Gateway invocation result must not be null");
        }

        if (outcome.hasProviderResponse()) {
            if (result.status() != InvocationStatus.SUCCESS) {
                return mapProviderFailure(outcome.providerResponse(), invocation.requestProtocol());
            }
            if (!requiresProtocolConversion(outcome.providerResponse(), invocation)) {
                return mapProviderResponse(outcome.providerResponse());
            }
        }
        if (result.status() == InvocationStatus.SUCCESS) {
            return mapSuccessResponse(invocation);
        } else {
            return mapFailureResponse(invocation);
        }
    }

    private boolean requiresProtocolConversion(ProviderGatewayResponse providerResponse, GatewayInvocation invocation) {
        return providerResponse.protocol() != invocation.requestProtocol();
    }

    private GatewayRawResponse mapProviderResponse(ProviderGatewayResponse providerResponse) {
        return GatewayRawResponse.of(
                providerResponse.body(),
                providerResponse.statusCode(),
                contentTypeOf(providerResponse.headers(), providerResponse.streaming()),
                providerResponse.headers()
        );
    }

    private GatewayRawResponse mapProviderFailure(ProviderGatewayResponse providerResponse, ProtocolType clientProtocol) {
        if (providerResponse.protocol() == clientProtocol) {
            return mapProviderResponse(providerResponse);
        }
        String message = upstreamErrorMessage(providerResponse.body());
        String body = clientProtocol == ProtocolType.CLAUDE_MESSAGES
                ? buildClaudeUpstreamErrorBody(providerResponse.statusCode(), message)
                : buildOpenAIUpstreamErrorBody(providerResponse.statusCode(), message);
        return GatewayRawResponse.of(
                body,
                providerResponse.statusCode(),
                MediaType.APPLICATION_JSON,
                providerResponse.headers()
        );
    }

    private String upstreamErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Upstream request failed";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            for (JsonNode candidate : List.of(
                    root.path("error").path("message"),
                    root.path("message"),
                    root.path("detail"),
                    root.path("error")
            )) {
                if (candidate.isTextual() && !candidate.asText().isBlank()) {
                    return candidate.asText();
                }
            }
        } catch (Exception ignored) {
            // Preserve plain-text upstream errors as the message.
        }
        return body;
    }

    private String buildClaudeUpstreamErrorBody(int statusCode, String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "error");
            ObjectNode error = objectMapper.createObjectNode();
            error.put("type", switch (statusCode) {
                case 400 -> "invalid_request_error";
                case 401 -> "authentication_error";
                case 403 -> "permission_error";
                case 404 -> "not_found_error";
                case 429 -> "rate_limit_error";
                default -> "api_error";
            });
            error.put("message", message);
            root.set("error", error);
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"Upstream request failed\"}}";
        }
    }

    private String buildOpenAIUpstreamErrorBody(int statusCode, String message) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode error = objectMapper.createObjectNode();
            error.put("message", message);
            error.put("type", statusCode == 429 ? "rate_limit_error" : statusCode >= 500 ? "api_error" : "invalid_request_error");
            error.putNull("param");
            error.putNull("code");
            root.set("error", error);
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return "{\"error\":{\"message\":\"Upstream request failed\",\"type\":\"api_error\"}}";
        }
    }

    private MediaType contentTypeOf(Map<String, List<String>> headers, boolean streaming) {
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase("content-type")
                        && entry.getValue() != null
                        && !entry.getValue().isEmpty()
                        && entry.getValue().get(0) != null
                        && !entry.getValue().get(0).isBlank()) {
                    return MediaType.parseMediaType(entry.getValue().get(0));
                }
            }
        }
        return streaming ? MediaType.TEXT_EVENT_STREAM : MediaType.APPLICATION_JSON;
    }

    private GatewayRawResponse mapSuccessResponse(GatewayInvocation invocation) {
        ConversionTrace conversionTrace = invocation.conversionTrace();
        if (conversionTrace == null || conversionTrace.responseConversion() == null) {
            throw new IllegalStateException("Success invocation must contain response conversion");
        }

        ConversionResult responseConversion = conversionTrace.responseConversion();
        String body = responseConversion.body();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Response conversion body must not be blank");
        }

        MediaType contentType = invocation.result().streaming()
                ? MediaType.TEXT_EVENT_STREAM
                : MediaType.APPLICATION_JSON;

        return GatewayRawResponse.of(body, HttpStatus.OK.value(), contentType);
    }

    private GatewayRawResponse mapFailureResponse(GatewayInvocation invocation) {
        GatewayInvocationResult result = invocation.result();
        InvocationError error = result.error();
        if (error == null) {
            throw new IllegalStateException("Failed invocation must contain error");
        }

        ProtocolType requestProtocol = invocation.requestProtocol();
        HttpStatus httpStatus = mapErrorToHttpStatus(error);
        String errorBody = buildProtocolErrorBody(requestProtocol, error);

        return GatewayRawResponse.of(errorBody, httpStatus.value(), MediaType.APPLICATION_JSON);
    }

    private HttpStatus mapErrorToHttpStatus(InvocationError error) {
        if (error.errorType() == InvocationErrorType.UPSTREAM_FAILED) {
            return switch (latestRouteFailureType(error)) {
                case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
                case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
                case CHANNEL_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
                default -> HttpStatus.BAD_GATEWAY;
            };
        }
        return switch (error.errorType()) {
            case AUTHENTICATION_FAILED, MODEL_NOT_ALLOWED -> HttpStatus.UNAUTHORIZED;
            case QUOTA_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case NO_AVAILABLE_CHANNEL -> HttpStatus.SERVICE_UNAVAILABLE;
            case CONVERSION_FAILED, UPSTREAM_FAILED -> HttpStatus.BAD_GATEWAY;
        };
    }

    private String buildProtocolErrorBody(ProtocolType protocol, InvocationError error) {
        try {
            return switch (protocol) {
                case CLAUDE_MESSAGES -> buildClaudeErrorBody(error);
                case OPENAI_RESPONSES, OPENAI_CHAT_COMPLETIONS -> buildOpenAIErrorBody(error);
                case AWS_BEDROCK_CONVERSE -> buildOpenAIErrorBody(error);
            };
        } catch (Exception exception) {
            return buildFallbackErrorBody(error);
        }
    }

    private String buildClaudeErrorBody(InvocationError error) {
        try {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("type", "error");
            ObjectNode innerError = objectMapper.createObjectNode();
            innerError.put("type", switch (error.errorType()) {
                case AUTHENTICATION_FAILED -> "authentication_error";
                case MODEL_NOT_ALLOWED -> "permission_error";
                case QUOTA_EXHAUSTED -> "rate_limit_error";
                case CONVERSION_FAILED -> "invalid_request_error";
                case NO_AVAILABLE_CHANNEL -> "api_error";
                case UPSTREAM_FAILED -> latestRouteFailureType(error) == RouteFailureType.RATE_LIMITED
                        ? "rate_limit_error"
                        : "api_error";
            });
            innerError.put("message", error.message());
            errorNode.set("error", innerError);
            return objectMapper.writeValueAsString(errorNode);
        } catch (Exception exception) {
            return buildFallbackErrorBody(error);
        }
    }

    private String buildOpenAIErrorBody(InvocationError error) {
        try {
            ObjectNode errorNode = objectMapper.createObjectNode();
            ObjectNode innerError = objectMapper.createObjectNode();
            innerError.put("message", error.message());
            innerError.put("type", mapErrorTypeToOpenAIType(error));
            innerError.putNull("param");
            innerError.putNull("code");
            errorNode.set("error", innerError);
            return objectMapper.writeValueAsString(errorNode);
        } catch (Exception exception) {
            return buildFallbackErrorBody(error);
        }
    }

    private String mapErrorTypeToOpenAIType(InvocationError error) {
        return switch (error.errorType()) {
            case AUTHENTICATION_FAILED, MODEL_NOT_ALLOWED -> "invalid_request_error";
            case QUOTA_EXHAUSTED -> "rate_limit_error";
            case NO_AVAILABLE_CHANNEL -> "service_unavailable";
            case CONVERSION_FAILED -> "invalid_request_error";
            case UPSTREAM_FAILED -> latestRouteFailureType(error) == RouteFailureType.RATE_LIMITED
                    ? "rate_limit_error"
                    : "api_error";
        };
    }

    private RouteFailureType latestRouteFailureType(InvocationError error) {
        List<RouteFailure> failures = error.failures();
        if (failures.isEmpty()) {
            return RouteFailureType.UPSTREAM_ERROR;
        }
        return failures.get(failures.size() - 1).failureType();
    }

    private String buildFallbackErrorBody(InvocationError error) {
        try {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("error", error.message());
            return objectMapper.writeValueAsString(errorNode);
        } catch (Exception exception) {
            return "{\"error\":\"Internal server error\"}";
        }
    }
}
