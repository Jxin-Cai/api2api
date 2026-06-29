package com.api2api.ohs.http.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * Adapter for a raw Anthropic Claude Messages protocol request.
 */
public final class ClaudeMessagesGatewayRequest implements GatewayProtocolRequest {

    private final String rawBody;
    private final JsonNode root;

    private ClaudeMessagesGatewayRequest(String rawBody, JsonNode root) {
        this.rawBody = requireRawBody(rawBody);
        this.root = Objects.requireNonNull(root, "Claude request JSON must not be null");
    }

    public static ClaudeMessagesGatewayRequest of(String rawBody, JsonNode root) {
        return new ClaudeMessagesGatewayRequest(rawBody, root);
    }

    @Override
    public String rawBody() {
        return rawBody;
    }

    @Override
    public String model() {
        JsonNode model = root.get("model");
        if (model == null || !model.isTextual() || model.asText().isBlank()) {
            throw new IllegalArgumentException("model is required");
        }
        return model.asText();
    }

    @Override
    public boolean streaming() {
        return booleanField("stream") || booleanField("streaming");
    }

    @Override
    public boolean toolCallingRequired() {
        return presentAndNotEmpty("tools") || presentAndNotEmpty("tool_choice");
    }

    @Override
    public boolean reasoningRequired() {
        return presentAndNotEmpty("thinking") || presentAndNotEmpty("reasoning");
    }

    private boolean booleanField(String fieldName) {
        JsonNode value = root.get(fieldName);
        return value != null && value.isBoolean() && value.asBoolean();
    }

    private boolean presentAndNotEmpty(String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || value.isNull()) {
            return false;
        }
        return !value.isArray() || !value.isEmpty();
    }

    private static String requireRawBody(String rawBody) {
        String value = Objects.requireNonNull(rawBody, "Raw request body must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Raw request body must not be blank");
        }
        return value;
    }
}
