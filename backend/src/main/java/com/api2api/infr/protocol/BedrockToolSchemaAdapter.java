package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Adapts Claude structured-output schemas to Bedrock Converse tool schemas. */
final class BedrockToolSchemaAdapter {

    static final String ADAPTED_TOOL_NAME_PREFIX = "api2api_wrapped_";
    static final String WRAPPED_VALUE_PROPERTY = "value";
    private static final int BEDROCK_TOOL_NAME_MAX_LENGTH = 64;

    private BedrockToolSchemaAdapter() {
    }

    static boolean requiresWrapping(JsonNode schema) {
        return schema != null
                && !schema.isNull()
                && !"object".equals(schema.path("type").asText(""));
    }

    static JsonNode toBedrockSchema(JsonNode schema) {
        if (schema == null || schema.isNull()) {
            return emptyObjectSchema();
        }
        if (!requiresWrapping(schema)) {
            return schema;
        }
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.set(WRAPPED_VALUE_PROPERTY, schema);

        ObjectNode wrapped = JsonNodeFactory.instance.objectNode();
        wrapped.put("type", "object");
        wrapped.set("properties", properties);
        wrapped.set("required", JsonNodeFactory.instance.arrayNode().add(WRAPPED_VALUE_PROPERTY));
        wrapped.put("additionalProperties", false);
        return wrapped;
    }

    static String toBedrockToolName(String toolName, JsonNode schema) {
        if (!requiresWrapping(schema)) {
            if (isAdaptedToolName(toolName)) {
                throw new ProtocolConversionException(
                        "CLAUDE_BEDROCK_RESERVED_TOOL_NAME_PREFIX: " + toolName);
            }
            return toolName;
        }
        String adaptedName = ADAPTED_TOOL_NAME_PREFIX + toolName;
        if (adaptedName.length() > BEDROCK_TOOL_NAME_MAX_LENGTH) {
            throw new ProtocolConversionException(
                    "CLAUDE_BEDROCK_WRAPPED_TOOL_NAME_TOO_LONG: " + toolName);
        }
        return adaptedName;
    }

    static boolean isAdaptedToolName(String toolName) {
        return toolName != null && toolName.startsWith(ADAPTED_TOOL_NAME_PREFIX);
    }

    static String toClaudeToolName(String toolName) {
        return isAdaptedToolName(toolName)
                ? toolName.substring(ADAPTED_TOOL_NAME_PREFIX.length())
                : toolName;
    }

    static JsonNode wrapInput(JsonNode input) {
        ObjectNode wrapped = JsonNodeFactory.instance.objectNode();
        wrapped.set(WRAPPED_VALUE_PROPERTY,
                input == null || input.isNull() ? JsonNodeFactory.instance.nullNode() : input);
        return wrapped;
    }

    static JsonNode unwrapInput(JsonNode input) {
        if (input == null
                || !input.isObject()
                || !input.has(WRAPPED_VALUE_PROPERTY)) {
            throw new ProtocolConversionException("BEDROCK_WRAPPED_TOOL_INPUT_INVALID");
        }
        return input.get(WRAPPED_VALUE_PROPERTY);
    }

    private static ObjectNode emptyObjectSchema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        schema.set("properties", JsonNodeFactory.instance.objectNode());
        return schema;
    }
}
