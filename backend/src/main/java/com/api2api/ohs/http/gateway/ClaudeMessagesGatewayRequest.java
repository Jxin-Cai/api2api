package com.api2api.ohs.http.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.Objects;

/**
 * Adapter for a raw Anthropic Claude Messages protocol request.
 */
public final class ClaudeMessagesGatewayRequest implements GatewayProtocolRequest {

    private static final Set<String> TOOL_CONTENT_TYPES = Set.of(
            "tool_use",
            "tool_result",
            "server_tool_use",
            "mcp_tool_use",
            "mcp_tool_result",
            "web_search_tool_result",
            "web_fetch_tool_result",
            "code_execution_tool_result",
            "bash_code_execution_tool_result",
            "text_editor_code_execution_tool_result",
            "tool_search_tool_result"
    );
    private static final Set<String> REASONING_CONTENT_TYPES = Set.of(
            "thinking",
            "redacted_thinking",
            "reasoning"
    );

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
        return presentAndNotEmpty("tools")
                || presentAndNotEmpty("tool_choice")
                || presentAndNotEmpty("mcp_servers")
                || containsMessageContentType(TOOL_CONTENT_TYPES);
    }

    @Override
    public boolean reasoningRequired() {
        return presentAndNotEmpty("thinking")
                || presentAndNotEmpty("reasoning")
                || nestedFieldPresent("output_config", "effort")
                || containsMessageContentType(REASONING_CONTENT_TYPES);
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

    private boolean nestedFieldPresent(String objectField, String nestedField) {
        JsonNode value = root.path(objectField).get(nestedField);
        return value != null && !value.isNull();
    }

    private boolean containsMessageContentType(Set<String> types) {
        JsonNode messages = root.get("messages");
        if (messages == null || !messages.isArray()) {
            return false;
        }
        for (JsonNode message : messages) {
            JsonNode content = message.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                if (types.contains(block.path("type").asText(""))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String requireRawBody(String rawBody) {
        String value = Objects.requireNonNull(rawBody, "Raw request body must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Raw request body must not be blank");
        }
        return value;
    }
}
