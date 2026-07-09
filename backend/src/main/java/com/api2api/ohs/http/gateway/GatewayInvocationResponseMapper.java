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
import com.fasterxml.jackson.databind.ObjectMapper;
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

        if (outcome.hasProviderResponse() && !requiresProtocolConversion(outcome.providerResponse(), invocation)) {
            return mapProviderResponse(outcome.providerResponse());
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
        HttpStatus httpStatus = mapErrorToHttpStatus(error.errorType());
        String errorBody = buildProtocolErrorBody(requestProtocol, error);

        return GatewayRawResponse.of(errorBody, httpStatus.value(), MediaType.APPLICATION_JSON);
    }

    private HttpStatus mapErrorToHttpStatus(InvocationErrorType errorType) {
        return switch (errorType) {
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
            ObjectNode innerError = objectMapper.createObjectNode();
            innerError.put("type", "error");
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
            innerError.put("type", mapErrorTypeToOpenAIType(error.errorType()));
            innerError.putNull("param");
            innerError.putNull("code");
            errorNode.set("error", innerError);
            return objectMapper.writeValueAsString(errorNode);
        } catch (Exception exception) {
            return buildFallbackErrorBody(error);
        }
    }

    private String mapErrorTypeToOpenAIType(InvocationErrorType errorType) {
        return switch (errorType) {
            case AUTHENTICATION_FAILED, MODEL_NOT_ALLOWED -> "invalid_request_error";
            case QUOTA_EXHAUSTED -> "rate_limit_error";
            case NO_AVAILABLE_CHANNEL -> "service_unavailable";
            case CONVERSION_FAILED -> "invalid_request_error";
            case UPSTREAM_FAILED -> "api_error";
        };
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
