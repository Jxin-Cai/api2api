package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Preserves the distinction between Responses JSON function calls and
 * free-form custom tool calls while exposing both as Claude tool_use blocks.
 */
final class ResponsesToolCallBridge {

    private static final String FUNCTION_CALL = "function_call";
    private static final String CUSTOM_TOOL_CALL = "custom_tool_call";
    private static final String CUSTOM_TOOL_ID_PREFIX = "toolu_api2api_custom_";

    private ResponsesToolCallBridge() {
    }

    static boolean isToolCall(String type) {
        return FUNCTION_CALL.equals(type) || CUSTOM_TOOL_CALL.equals(type);
    }

    static boolean isCustomClaudeToolUseId(String toolUseId) {
        return toolUseId != null && toolUseId.startsWith(CUSTOM_TOOL_ID_PREFIX);
    }

    static String toClaudeToolUseId(JsonNode item) {
        String callId = item.path("call_id").asText(item.path("id").asText(""));
        if (!CUSTOM_TOOL_CALL.equals(item.path("type").asText(""))) {
            return callId;
        }
        return CUSTOM_TOOL_ID_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(callId.getBytes(StandardCharsets.UTF_8));
    }

    static String toResponsesCallId(String toolUseId) {
        if (!isCustomClaudeToolUseId(toolUseId)) {
            return toolUseId;
        }
        String encoded = toolUseId.substring(CUSTOM_TOOL_ID_PREFIX.length());
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_INVALID_CUSTOM_TOOL_USE_ID", exception);
        }
    }

    static JsonNode toClaudeToolInput(ObjectMapper objectMapper, JsonNode item) {
        String type = item.path("type").asText("");
        String rawInput = FUNCTION_CALL.equals(type)
                ? item.path("arguments").asText("{}")
                : item.path("input").asText("");
        JsonNode input = parseToolInput(objectMapper, rawInput, CUSTOM_TOOL_CALL.equals(type));
        return sanitizeClaudeCodeToolInput(input, item.path("name").asText(""));
    }

    static String toClaudeToolInputJson(ObjectMapper objectMapper, String name, String rawInput, boolean custom) {
        JsonNode parsed = parseToolInput(objectMapper, rawInput, custom);
        JsonNode sanitized = sanitizeClaudeCodeToolInput(parsed, name);
        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_TOOL_INPUT_ENCODING_FAILED", exception);
        }
    }

    static ObjectNode toResponsesToolCall(ObjectMapper objectMapper, JsonNode block) {
        String toolUseId = block.path("id").asText("");
        boolean custom = isCustomClaudeToolUseId(toolUseId);
        ObjectNode call = objectMapper.createObjectNode();
        call.put("type", custom ? CUSTOM_TOOL_CALL : FUNCTION_CALL);
        call.put("call_id", toResponsesCallId(toolUseId));
        call.put("name", block.path("name").asText(""));
        JsonNode input = block.get("input");
        if (custom) {
            call.put("input", customInputValue(input));
        } else {
            call.put("arguments", input == null || input.isNull() ? "{}" : input.toString());
        }
        return call;
    }

    private static JsonNode parseToolInput(ObjectMapper objectMapper, String rawInput, boolean custom) {
        String normalized = rawInput == null ? "" : rawInput.trim();
        if (normalized.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode parsed = objectMapper.readTree(normalized);
            if (parsed != null && parsed.isObject()) {
                return parsed;
            }
            if (!custom) {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_FUNCTION_ARGUMENTS_MUST_BE_OBJECT");
            }
        } catch (JsonProcessingException exception) {
            if (!custom) {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_INVALID_FUNCTION_ARGUMENTS", exception);
            }
        }
        ObjectNode wrapped = objectMapper.createObjectNode();
        wrapped.put("input", rawInput);
        return wrapped;
    }

    private static JsonNode sanitizeClaudeCodeToolInput(JsonNode input, String name) {
        if (!"Read".equals(name) || !input.isObject()
                || !input.path("pages").isTextual() || !input.path("pages").asText().isEmpty()) {
            return input;
        }
        ObjectNode sanitized = ((ObjectNode) input).deepCopy();
        sanitized.remove("pages");
        return sanitized;
    }

    private static String customInputValue(JsonNode input) {
        if (input == null || input.isNull()) {
            return "";
        }
        if (input.isObject() && input.size() == 1 && input.path("input").isTextual()) {
            return input.path("input").asText();
        }
        return input.toString();
    }
}
