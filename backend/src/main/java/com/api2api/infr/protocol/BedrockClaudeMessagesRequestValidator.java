package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

final class BedrockClaudeMessagesRequestValidator {

    private static final String TOOL_SEARCH_BETA = "tool-search-tool-2025-10-19";
    private static final String TOOL_EXAMPLES_BETA = "tool-examples-2025-10-29";
    private static final String FINE_GRAINED_TOOL_STREAMING_BETA =
            "fine-grained-tool-streaming-2025-05-14";
    private static final String EFFORT_BETA = "effort-2025-11-24";
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
        validateMessageContent(request.path("messages"));
        validateTools(request.path("tools"), requiredBetaFeatures);
        collectOutputConfigBetaFeatures(request.path("output_config"), requiredBetaFeatures);
        return Collections.unmodifiableSet(new LinkedHashSet<>(requiredBetaFeatures));
    }

    private static void collectOutputConfigBetaFeatures(
            JsonNode outputConfig,
            Set<String> requiredBetaFeatures
    ) {
        if (outputConfig.isObject() && outputConfig.hasNonNull("effort")) {
            requiredBetaFeatures.add(EFFORT_BETA);
        }
    }

    private static void validateMessageContent(JsonNode messages) {
        if (!messages.isArray()) {
            return;
        }
        for (JsonNode message : messages) {
            validateContentValue(message.path("content"));
        }
    }

    private static void validateContentValue(JsonNode content) {
        if (!content.isArray()) {
            return;
        }
        // Traverse only protocol-defined content arrays. Recursing through arbitrary tool input
        // would misclassify customer JSON containing fields such as {"type":"image"}.
        for (JsonNode block : content) {
            if (!block.isObject()) {
                continue;
            }
            String type = block.path("type").asText("");
            if ("image".equals(type)) {
                validateBase64Source(block.path("source"), "image");
                String mediaType = block.path("source").path("media_type").asText("");
                if (!BEDROCK_IMAGE_MEDIA_TYPES.contains(mediaType)) {
                    throw new ProtocolConversionException(
                            "Bedrock InvokeModel image source has unsupported media_type '" + mediaType + "'");
                }
            } else if ("document".equals(type)) {
                validateBase64Source(block.path("source"), "document");
                requireNonBlank(block.path("source").path("media_type"),
                        "Bedrock InvokeModel document source requires media_type");
            } else if ("tool_result".equals(type) || type.endsWith("_tool_result")) {
                validateContentValue(block.path("content"));
            }
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
        Set<String> toolNames = new HashSet<>();
        for (JsonNode tool : tools) {
            if (!tool.isObject()) {
                throw new ProtocolConversionException("Claude Messages tool definition must be an object");
            }
            String wireType = tool.path("type").asText("custom");
            BedrockClaudeToolType toolType = BedrockClaudeToolType.fromWireValue(wireType)
                    .orElseThrow(() -> new ProtocolConversionException(
                            "Claude tool type '" + wireType + "' is not supported by Bedrock InvokeModel"));
            toolType.requiredBeta().ifPresent(requiredBetaFeatures::add);
            String toolName = requireNonBlank(
                    tool.path("name"),
                    "Claude Messages tool definition requires a non-blank name");
            if (!toolNames.add(toolName)) {
                throw new ProtocolConversionException(
                        "Claude Messages tool names must be unique; duplicate '" + toolName + "'");
            }
            if (toolType == BedrockClaudeToolType.CUSTOM) {
                validateCustomToolSchema(tool);
            }
            if (tool.has("allowed_callers")) {
                throw new ProtocolConversionException(
                        "Claude tool field 'allowed_callers' is not supported by Bedrock InvokeModel");
            }
            if (tool.has("input_examples")) {
                if (toolType != BedrockClaudeToolType.CUSTOM) {
                    throw new ProtocolConversionException(
                            "Claude tool field 'input_examples' is only supported on custom tools");
                }
                validateInputExamples(tool.path("input_examples"));
                requiredBetaFeatures.add(TOOL_EXAMPLES_BETA);
            }
            boolean eagerInputStreaming = optionalBoolean(tool, "eager_input_streaming");
            if (eagerInputStreaming) {
                requiredBetaFeatures.add(FINE_GRAINED_TOOL_STREAMING_BETA);
            }
            boolean deferred = optionalBoolean(tool, "defer_loading");
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

    private static void validateCustomToolSchema(JsonNode tool) {
        JsonNode inputSchema = tool.path("input_schema");
        if (!inputSchema.isObject()) {
            throw new ProtocolConversionException(
                    "Bedrock InvokeModel custom tool requires input_schema to be an object");
        }
        if (!"object".equals(inputSchema.path("type").asText(""))) {
            throw new ProtocolConversionException(
                    "Bedrock InvokeModel custom tool input_schema.type must be 'object'");
        }
    }

    private static void validateInputExamples(JsonNode inputExamples) {
        if (!inputExamples.isArray()) {
            throw new ProtocolConversionException(
                    "Bedrock InvokeModel tool input_examples must be an array");
        }
        if (inputExamples.size() > 20) {
            throw new ProtocolConversionException(
                    "Bedrock InvokeModel tool input_examples must contain at most 20 examples");
        }
        for (JsonNode example : inputExamples) {
            if (!example.isObject()) {
                throw new ProtocolConversionException(
                        "Bedrock InvokeModel tool input_examples entries must be objects");
            }
        }
    }

    private static boolean optionalBoolean(JsonNode object, String fieldName) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull()) {
            return false;
        }
        if (!value.isBoolean()) {
            throw new ProtocolConversionException(
                    "Claude tool field '" + fieldName + "' must be a boolean");
        }
        return value.booleanValue();
    }

    private static String requireNonBlank(JsonNode value, String message) {
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new ProtocolConversionException(message);
        }
        return value.asText();
    }
}
