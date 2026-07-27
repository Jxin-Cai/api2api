package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class BedrockClaudeMessagesRequestValidator {

    private static final String TOOL_SEARCH_BETA = "tool-search-tool-2025-10-19";
    private static final String TOOL_EXAMPLES_BETA = "tool-examples-2025-10-29";
    private static final String FINE_GRAINED_TOOL_STREAMING_BETA =
            "fine-grained-tool-streaming-2025-05-14";
    private static final Set<String> BEDROCK_IMAGE_MEDIA_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private BedrockClaudeMessagesRequestValidator() {
    }

    static Set<String> validateAndCollectRequiredBetaFeatures(JsonNode request) {
        Set<String> requiredBetaFeatures = new LinkedHashSet<>();
        validateContentBlocks(request.path("messages"));
        validateTools(request.path("tools"), requiredBetaFeatures);
        return Collections.unmodifiableSet(new LinkedHashSet<>(requiredBetaFeatures));
    }

    private static void validateContentBlocks(JsonNode node) {
        if (node.isObject()) {
            String type = node.path("type").asText("");
            if ("image".equals(type)) {
                validateBase64Source(node.path("source"), "image");
                String mediaType = node.path("source").path("media_type").asText("");
                if (!BEDROCK_IMAGE_MEDIA_TYPES.contains(mediaType)) {
                    throw new ProtocolConversionException(
                            "Bedrock InvokeModel image source has unsupported media_type '" + mediaType + "'");
                }
            } else if ("document".equals(type)) {
                validateBase64Source(node.path("source"), "document");
                requireNonBlank(node.path("source").path("media_type"),
                        "Bedrock InvokeModel document source requires media_type");
            }
            node.elements().forEachRemaining(BedrockClaudeMessagesRequestValidator::validateContentBlocks);
            return;
        }
        if (node.isArray()) {
            node.elements().forEachRemaining(BedrockClaudeMessagesRequestValidator::validateContentBlocks);
        }
    }

    private static void validateBase64Source(JsonNode source, String contentType) {
        if (!source.isObject()) {
            throw new ProtocolConversionException(
                    "Bedrock InvokeModel " + contentType + " content requires a source object");
        }
        String sourceType = source.path("type").asText("");
        if (!"base64".equals(sourceType)) {
            throw new ProtocolConversionException(
                    "Bedrock InvokeModel " + contentType
                            + " source only supports base64; received '" + sourceType + "'");
        }
        requireNonBlank(source.path("data"),
                "Bedrock InvokeModel " + contentType + " base64 source requires data");
    }

    private static void validateTools(JsonNode tools, Set<String> requiredBetaFeatures) {
        if (tools.isMissingNode() || tools.isNull()) {
            return;
        }
        if (!tools.isArray()) {
            throw new ProtocolConversionException("Claude Messages tools must be an array");
        }
        boolean hasDeferredTool = false;
        boolean hasImmediatelyLoadedTool = false;
        for (JsonNode tool : tools) {
            if (!tool.isObject()) {
                throw new ProtocolConversionException("Claude Messages tool definition must be an object");
            }
            String wireType = tool.path("type").asText("custom");
            BedrockClaudeToolType toolType = BedrockClaudeToolType.fromWireValue(wireType)
                    .orElseThrow(() -> new ProtocolConversionException(
                            "Claude tool type '" + wireType + "' is not supported by Bedrock InvokeModel"));
            toolType.requiredBeta().ifPresent(requiredBetaFeatures::add);
            if (tool.has("allowed_callers")) {
                throw new ProtocolConversionException(
                        "Claude tool field 'allowed_callers' is not supported by Bedrock InvokeModel");
            }
            if (tool.has("input_examples")) {
                if (toolType != BedrockClaudeToolType.CUSTOM) {
                    throw new ProtocolConversionException(
                            "Claude tool field 'input_examples' is only supported on custom tools");
                }
                requiredBetaFeatures.add(TOOL_EXAMPLES_BETA);
            }
            if (tool.path("eager_input_streaming").asBoolean(false)) {
                requiredBetaFeatures.add(FINE_GRAINED_TOOL_STREAMING_BETA);
            }
            boolean deferred = tool.path("defer_loading").asBoolean(false);
            hasDeferredTool = hasDeferredTool || deferred;
            hasImmediatelyLoadedTool = hasImmediatelyLoadedTool || !deferred;
        }
        if (hasDeferredTool) {
            requiredBetaFeatures.add(TOOL_SEARCH_BETA);
            if (!hasImmediatelyLoadedTool) {
                throw new ProtocolConversionException(
                        "Bedrock InvokeModel tool search requires at least one tool with defer_loading=false");
            }
        }
    }

    private static void requireNonBlank(JsonNode value, String message) {
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new ProtocolConversionException(message);
        }
    }
}
