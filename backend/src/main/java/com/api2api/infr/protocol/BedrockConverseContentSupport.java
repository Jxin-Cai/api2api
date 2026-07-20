package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shared utility methods for Bedrock Converse stop reason mapping and reasoning content extraction.
 * Used by both streaming and non-streaming converters.
 */
final class BedrockConverseContentSupport {

    private BedrockConverseContentSupport() {}

    static String requiredStopReason(JsonNode source) {
        JsonNode value = source.get("stopReason");
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ProtocolConversionException("BEDROCK_CONVERSE_MISSING_STOP_REASON");
        }
        return value.asText();
    }

    static String toClaudeStop(String stopReason) {
        return switch (stopReason) {
            case "end_turn" -> "end_turn";
            case "stop_sequence" -> "stop_sequence";
            case "max_tokens" -> "max_tokens";
            case "tool_use" -> "tool_use";
            case "model_context_window_exceeded" -> "model_context_window_exceeded";
            case "content_filtered", "guardrail_intervened" -> "refusal";
            case "malformed_model_output", "malformed_tool_use" ->
                    throw new ProtocolConversionException("BEDROCK_CONVERSE_INVALID_MODEL_OUTPUT: " + stopReason);
            default -> throw new ProtocolConversionException("BEDROCK_CONVERSE_UNSUPPORTED_STOP_REASON: " + stopReason);
        };
    }

    static String toResponsesStatus(String stopReason) {
        return switch (stopReason) {
            case "end_turn", "tool_use", "stop_sequence" -> "completed";
            case "max_tokens", "model_context_window_exceeded" -> "incomplete";
            case "content_filtered", "guardrail_intervened" -> "incomplete";
            case "malformed_model_output", "malformed_tool_use" ->
                    throw new ProtocolConversionException("BEDROCK_CONVERSE_INVALID_MODEL_OUTPUT: " + stopReason);
            default -> throw new ProtocolConversionException("BEDROCK_CONVERSE_UNSUPPORTED_STOP_REASON: " + stopReason);
        };
    }

    static JsonNode reasoningTextNode(JsonNode block) {
        JsonNode reasoningContent = block.get("reasoningContent");
        if (reasoningContent == null || reasoningContent.isNull()) {
            return null;
        }
        JsonNode reasoningText = reasoningContent.get("reasoningText");
        if (reasoningText != null && !reasoningText.isNull()) {
            return reasoningText;
        }
        return reasoningContent.has("text") || reasoningContent.has("signature") ? reasoningContent : null;
    }
}
