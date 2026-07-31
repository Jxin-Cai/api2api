package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class BedrockStructuredOutputCompatibility {

    static final String TOOL_NAME = "StructuredOutput";
    static final String WRAPPER_PROPERTY = "__api2api_structured_output";
    private static final String ENFORCEMENT_MARKER = "[structured-output-enforce]";

    private BedrockStructuredOutputCompatibility() {
    }

    static void normalizeRequest(ObjectNode request) {
        normalizeConversationHistory(request.path("messages"));
        JsonNode tools = request.path("tools");
        if (!tools.isArray()) {
            return;
        }
        boolean hasStructuredOutputTool = false;
        for (JsonNode toolNode : tools) {
            if (!(toolNode instanceof ObjectNode tool)
                    || !TOOL_NAME.equals(tool.path("name").asText())) {
                continue;
            }
            hasStructuredOutputTool = true;
            normalizeInputSchema(tool);
        }
        if (hasStructuredOutputTool && isEnforcementRetry(request.path("messages"))) {
            forceStructuredOutputTool(request);
        }
    }

    private static boolean isEnforcementRetry(JsonNode messages) {
        if (!(messages instanceof ArrayNode messageArray) || messageArray.isEmpty()) {
            return false;
        }
        JsonNode lastMessage = messageArray.get(messageArray.size() - 1);
        if (!"user".equals(lastMessage.path("role").asText())) {
            return false;
        }
        JsonNode content = lastMessage.get("content");
        if (content != null && content.isTextual()) {
            return content.asText().startsWith(ENFORCEMENT_MARKER);
        }
        if (!(content instanceof ArrayNode blocks)) {
            return false;
        }
        for (JsonNode block : blocks) {
            if ("text".equals(block.path("type").asText())
                    && block.path("text").asText().startsWith(ENFORCEMENT_MARKER)) {
                return true;
            }
        }
        return false;
    }

    private static void forceStructuredOutputTool(ObjectNode request) {
        // Bedrock rejects forced tool choice while thinking is enabled. The enforcement
        // retry is a final serialization turn, so it does not need another thinking pass.
        request.remove("thinking");
        ObjectNode toolChoice = request.putObject("tool_choice");
        toolChoice.put("type", "tool");
        toolChoice.put("name", TOOL_NAME);
        toolChoice.put("disable_parallel_tool_use", true);
    }

    private static void normalizeConversationHistory(JsonNode messages) {
        if (!messages.isArray()) {
            return;
        }
        for (JsonNode message : messages) {
            JsonNode content = message.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode blockNode : content) {
                if (!(blockNode instanceof ObjectNode block)
                        || !"tool_use".equals(block.path("type").asText())
                        || !TOOL_NAME.equals(block.path("name").asText())) {
                    continue;
                }
                JsonNode input = block.get("input");
                if (input != null && !input.isObject()) {
                    ObjectNode wrappedInput = block.objectNode();
                    wrappedInput.set(WRAPPER_PROPERTY, input.deepCopy());
                    block.set("input", wrappedInput);
                }
            }
        }
    }

    static void unwrapResponse(JsonNode response) {
        JsonNode content = response.path("content");
        if (!(content instanceof ArrayNode blocks)) {
            return;
        }
        for (JsonNode blockNode : blocks) {
            if (!(blockNode instanceof ObjectNode block)
                    || !"tool_use".equals(block.path("type").asText())
                    || !TOOL_NAME.equals(block.path("name").asText())) {
                continue;
            }
            JsonNode unwrapped = unwrapInput(block.get("input"));
            if (unwrapped != null) {
                block.set("input", unwrapped.deepCopy());
            }
        }
    }

    static JsonNode unwrapInput(JsonNode input) {
        if (input == null || !input.isObject() || input.size() != 1 || !input.has(WRAPPER_PROPERTY)) {
            return input;
        }
        return input.get(WRAPPER_PROPERTY);
    }

    private static void normalizeInputSchema(ObjectNode tool) {
        JsonNode inputSchema = tool.get("input_schema");
        if (!(inputSchema instanceof ObjectNode schema)) {
            return;
        }
        String type = schema.path("type").asText("");
        if ("object".equals(type)) {
            return;
        }
        if (type.isBlank() && impliesObjectSchema(schema)) {
            schema.put("type", "object");
            return;
        }
        ObjectNode wrapper = tool.objectNode();
        wrapper.put("type", "object");
        ObjectNode properties = wrapper.putObject("properties");
        properties.set(WRAPPER_PROPERTY, schema.deepCopy());
        wrapper.putArray("required").add(WRAPPER_PROPERTY);
        wrapper.put("additionalProperties", false);
        tool.set("input_schema", wrapper);
    }

    private static boolean impliesObjectSchema(ObjectNode schema) {
        return schema.has("properties")
                || schema.has("required")
                || schema.has("additionalProperties")
                || schema.has("patternProperties");
    }
}
