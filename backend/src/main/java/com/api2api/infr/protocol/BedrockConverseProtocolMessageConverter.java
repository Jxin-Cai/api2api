package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;

final class BedrockConverseProtocolMessageConverter extends AbstractProtocolMessageConverter {

    BedrockConverseProtocolMessageConverter(
            ProtocolJsonSupport json,
            UnifiedUsageExtractor usageExtractor,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            SseEventTransformer sseEventTransformer
    ) {
        super(json, usageExtractor, sourceProtocol, targetProtocol, direction, sseEventTransformer);
    }

    @Override
    public boolean supports(ProtocolConversionRequest requirement) {
        return !requirement.streaming() && !requirement.toolCallingRequired() && super.supports(requirement);
    }

    @Override
    protected JsonNode convertRequestJson(JsonNode source) {
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE) {
            return claudeRequestToBedrock(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS && targetProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE) {
            return chatRequestToBedrock(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_RESPONSES && targetProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE) {
            return responsesRequestToBedrock(source);
        }
        throw new IllegalStateException("Unexpected request conversion: " + sourceProtocol() + " -> " + targetProtocol());
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source) {
        if (sourceProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
            return bedrockResponseToClaude(source);
        }
        if (sourceProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return bedrockResponseToChat(source);
        }
        if (sourceProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE && targetProtocol() == ProtocolType.OPENAI_RESPONSES) {
            return bedrockResponseToResponses(source);
        }
        throw new IllegalStateException("Unexpected response conversion: " + sourceProtocol() + " -> " + targetProtocol());
    }

    // ==================== Request: Claude Messages -> Bedrock Converse ====================

    private ObjectNode claudeRequestToBedrock(JsonNode source) {
        ObjectNode target = json.objectNode();

        ArrayNode bedrockMessages = json.arrayNode();
        JsonNode messages = source.get("messages");
        if (messages != null && messages.isArray()) {
            String lastRole = null;
            for (JsonNode msg : messages) {
                String role = msg.path("role").asText("user");
                if (role.equals(lastRole)) {
                    insertDummyAlternation(bedrockMessages, role);
                }
                bedrockMessages.add(toBedrockMessage(role, msg.get("content")));
                lastRole = role;
            }
        }
        target.set("messages", bedrockMessages);

        JsonNode system = source.get("system");
        if (system != null && !system.isNull()) {
            ArrayNode systemBlocks = json.arrayNode();
            ObjectNode textBlock = json.objectNode();
            textBlock.put("text", system.isTextual() ? system.asText() : firstTextFromClaudeContent(system));
            systemBlocks.add(textBlock);
            target.set("system", systemBlocks);
        }

        ObjectNode inferenceConfig = json.objectNode();
        JsonNode maxTokens = source.get("max_tokens");
        if (maxTokens != null && !maxTokens.isNull()) {
            inferenceConfig.put("maxTokens", maxTokens.asInt());
        }
        JsonNode temperature = source.get("temperature");
        if (temperature != null && !temperature.isNull()) {
            inferenceConfig.put("temperature", temperature.floatValue());
        }
        JsonNode topP = source.get("top_p");
        if (topP != null && !topP.isNull()) {
            inferenceConfig.put("topP", topP.floatValue());
        }
        JsonNode stopSequences = source.get("stop_sequences");
        if (stopSequences != null && stopSequences.isArray() && !stopSequences.isEmpty()) {
            inferenceConfig.set("stopSequences", stopSequences);
        }
        if (!inferenceConfig.isEmpty()) {
            target.set("inferenceConfig", inferenceConfig);
        }

        return target;
    }

    // ==================== Request: OpenAI Chat -> Bedrock Converse ====================

    private ObjectNode chatRequestToBedrock(JsonNode source) {
        ObjectNode target = json.objectNode();

        ArrayNode bedrockMessages = json.arrayNode();
        ArrayNode systemBlocks = json.arrayNode();
        JsonNode messages = source.get("messages");
        if (messages != null && messages.isArray()) {
            String lastRole = null;
            for (JsonNode msg : messages) {
                String role = msg.path("role").asText("user");
                if ("system".equals(role)) {
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("text", msg.path("content").asText(""));
                    systemBlocks.add(textBlock);
                    continue;
                }
                String bedrockRole = "assistant".equals(role) ? "assistant" : "user";
                if (bedrockRole.equals(lastRole)) {
                    insertDummyAlternation(bedrockMessages, lastRole);
                }
                ObjectNode bedrockMsg = json.objectNode();
                bedrockMsg.put("role", bedrockRole);
                ArrayNode content = json.arrayNode();
                ObjectNode textBlock = json.objectNode();
                textBlock.put("text", msg.path("content").asText(""));
                content.add(textBlock);
                bedrockMsg.set("content", content);
                bedrockMessages.add(bedrockMsg);
                lastRole = bedrockRole;
            }
        }
        target.set("messages", bedrockMessages);
        if (!systemBlocks.isEmpty()) {
            target.set("system", systemBlocks);
        }

        ObjectNode inferenceConfig = json.objectNode();
        JsonNode maxTokens = source.get("max_tokens");
        if (maxTokens != null && !maxTokens.isNull()) {
            inferenceConfig.put("maxTokens", maxTokens.asInt());
        }
        JsonNode temperature = source.get("temperature");
        if (temperature != null && !temperature.isNull()) {
            inferenceConfig.put("temperature", temperature.floatValue());
        }
        JsonNode topP = source.get("top_p");
        if (topP != null && !topP.isNull()) {
            inferenceConfig.put("topP", topP.floatValue());
        }
        JsonNode stop = source.get("stop");
        if (stop != null && stop.isArray() && !stop.isEmpty()) {
            inferenceConfig.set("stopSequences", stop);
        }
        if (!inferenceConfig.isEmpty()) {
            target.set("inferenceConfig", inferenceConfig);
        }

        return target;
    }

    // ==================== Request: OpenAI Responses -> Bedrock Converse ====================

    private ObjectNode responsesRequestToBedrock(JsonNode source) {
        ObjectNode target = json.objectNode();

        ArrayNode bedrockMessages = json.arrayNode();
        JsonNode input = source.get("input");
        if (input != null) {
            if (input.isTextual()) {
                ObjectNode msg = json.objectNode();
                msg.put("role", "user");
                ArrayNode content = json.arrayNode();
                ObjectNode textBlock = json.objectNode();
                textBlock.put("text", input.asText());
                content.add(textBlock);
                msg.set("content", content);
                bedrockMessages.add(msg);
            } else if (input.isArray()) {
                String lastRole = null;
                for (JsonNode item : input) {
                    String role = item.path("role").asText("user");
                    String bedrockRole = "assistant".equals(role) ? "assistant" : "user";
                    if (bedrockRole.equals(lastRole)) {
                        insertDummyAlternation(bedrockMessages, lastRole);
                    }
                    ObjectNode msg = json.objectNode();
                    msg.put("role", bedrockRole);
                    ArrayNode content = json.arrayNode();
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("text", extractOpenAiContentText(item.get("content")));
                    content.add(textBlock);
                    msg.set("content", content);
                    bedrockMessages.add(msg);
                    lastRole = bedrockRole;
                }
            }
        }
        target.set("messages", bedrockMessages);

        JsonNode instructions = source.get("instructions");
        if (instructions != null && !instructions.isNull()) {
            ArrayNode systemBlocks = json.arrayNode();
            ObjectNode textBlock = json.objectNode();
            textBlock.put("text", instructions.asText(""));
            systemBlocks.add(textBlock);
            target.set("system", systemBlocks);
        }

        ObjectNode inferenceConfig = json.objectNode();
        JsonNode maxTokens = source.get("max_output_tokens");
        if (maxTokens != null && !maxTokens.isNull()) {
            inferenceConfig.put("maxTokens", maxTokens.asInt());
        }
        JsonNode temperature = source.get("temperature");
        if (temperature != null && !temperature.isNull()) {
            inferenceConfig.put("temperature", temperature.floatValue());
        }
        JsonNode topP = source.get("top_p");
        if (topP != null && !topP.isNull()) {
            inferenceConfig.put("topP", topP.floatValue());
        }
        if (!inferenceConfig.isEmpty()) {
            target.set("inferenceConfig", inferenceConfig);
        }

        return target;
    }

    // ==================== Response: Bedrock Converse -> Claude Messages ====================

    private ObjectNode bedrockResponseToClaude(JsonNode source) {
        ObjectNode target = json.objectNode();
        target.put("id", "msg_api2api_bedrock");
        target.put("type", "message");
        target.put("role", "assistant");
        target.put("model", "bedrock");

        ArrayNode content = json.arrayNode();
        JsonNode output = source.path("output").path("message").path("content");
        if (output.isArray()) {
            for (JsonNode block : output) {
                if (block.has("text")) {
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("type", "text");
                    textBlock.put("text", block.path("text").asText(""));
                    content.add(textBlock);
                }
            }
        }
        if (content.isEmpty()) {
            ObjectNode emptyText = json.objectNode();
            emptyText.put("type", "text");
            emptyText.put("text", "");
            content.add(emptyText);
        }
        target.set("content", content);

        target.put("stop_reason", mapBedrockStopToClaudeStop(source.path("stopReason").asText("end_turn")));

        ObjectNode usage = json.objectNode();
        JsonNode bedrockUsage = source.path("usage");
        long inputTokens = bedrockUsage.path("inputTokens").asLong(0);
        long outputTokens = bedrockUsage.path("outputTokens").asLong(0);
        long cacheRead = bedrockUsage.path("cacheReadInputTokens").asLong(0);
        long cacheWrite = bedrockUsage.path("cacheWriteInputTokens").asLong(0);
        usage.put("input_tokens", inputTokens);
        usage.put("output_tokens", outputTokens);
        if (cacheRead > 0) {
            usage.put("cache_read_input_tokens", cacheRead);
        }
        if (cacheWrite > 0) {
            usage.put("cache_creation_input_tokens", cacheWrite);
        }
        target.set("usage", usage);

        return target;
    }

    // ==================== Response: Bedrock Converse -> OpenAI Chat ====================

    private ObjectNode bedrockResponseToChat(JsonNode source) {
        ObjectNode target = json.objectNode();
        target.put("id", "chatcmpl-api2api-bedrock");
        target.put("object", "chat.completion");
        target.put("created", Instant.now().getEpochSecond());
        target.put("model", "bedrock");

        ArrayNode choices = json.arrayNode();
        ObjectNode choice = json.objectNode();
        choice.put("index", 0);
        ObjectNode message = json.objectNode();
        message.put("role", "assistant");
        message.put("content", extractTextFromBedrockOutput(source));
        choice.set("message", message);
        choice.put("finish_reason", mapBedrockStopToFinishReason(source.path("stopReason").asText("end_turn")));
        choices.add(choice);
        target.set("choices", choices);

        ObjectNode usage = json.objectNode();
        JsonNode bedrockUsage = source.path("usage");
        long inputTokens = bedrockUsage.path("inputTokens").asLong(0);
        long outputTokens = bedrockUsage.path("outputTokens").asLong(0);
        long cacheRead = bedrockUsage.path("cacheReadInputTokens").asLong(0);
        usage.put("prompt_tokens", inputTokens);
        usage.put("completion_tokens", outputTokens);
        usage.put("total_tokens", inputTokens + outputTokens);
        if (cacheRead > 0) {
            ObjectNode details = json.objectNode();
            details.put("cached_tokens", cacheRead);
            usage.set("prompt_tokens_details", details);
        }
        target.set("usage", usage);

        return target;
    }

    // ==================== Response: Bedrock Converse -> OpenAI Responses ====================

    private ObjectNode bedrockResponseToResponses(JsonNode source) {
        ObjectNode target = json.objectNode();
        target.put("id", "resp_api2api_bedrock");
        target.put("object", "response");
        target.put("created_at", Instant.now().getEpochSecond());
        target.put("model", "bedrock");

        ArrayNode output = json.arrayNode();
        ObjectNode outputMessage = json.objectNode();
        outputMessage.put("type", "message");
        outputMessage.put("role", "assistant");
        ArrayNode outputContent = json.arrayNode();
        ObjectNode textItem = json.objectNode();
        textItem.put("type", "output_text");
        textItem.put("text", extractTextFromBedrockOutput(source));
        outputContent.add(textItem);
        outputMessage.set("content", outputContent);
        output.add(outputMessage);
        target.set("output", output);

        ObjectNode usage = json.objectNode();
        JsonNode bedrockUsage = source.path("usage");
        long inputTokens = bedrockUsage.path("inputTokens").asLong(0);
        long outputTokens = bedrockUsage.path("outputTokens").asLong(0);
        long cacheRead = bedrockUsage.path("cacheReadInputTokens").asLong(0);
        usage.put("input_tokens", inputTokens);
        usage.put("output_tokens", outputTokens);
        usage.put("total_tokens", inputTokens + outputTokens);
        if (cacheRead > 0) {
            ObjectNode details = json.objectNode();
            details.put("cached_tokens", cacheRead);
            usage.set("input_tokens_details", details);
        }
        target.set("usage", usage);

        return target;
    }

    // ==================== Helper methods ====================

    private ObjectNode toBedrockMessage(String role, JsonNode claudeContent) {
        ObjectNode msg = json.objectNode();
        msg.put("role", "assistant".equals(role) ? "assistant" : "user");
        ArrayNode contentBlocks = json.arrayNode();

        if (claudeContent == null || claudeContent.isNull()) {
            ObjectNode textBlock = json.objectNode();
            textBlock.put("text", "");
            contentBlocks.add(textBlock);
        } else if (claudeContent.isTextual()) {
            ObjectNode textBlock = json.objectNode();
            textBlock.put("text", claudeContent.asText());
            contentBlocks.add(textBlock);
        } else if (claudeContent.isArray()) {
            for (JsonNode item : claudeContent) {
                String type = item.path("type").asText("");
                if ("text".equals(type)) {
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("text", item.path("text").asText(""));
                    contentBlocks.add(textBlock);
                }
            }
            if (contentBlocks.isEmpty()) {
                ObjectNode textBlock = json.objectNode();
                textBlock.put("text", "");
                contentBlocks.add(textBlock);
            }
        }

        msg.set("content", contentBlocks);
        return msg;
    }

    private void insertDummyAlternation(ArrayNode messages, String currentRole) {
        ObjectNode dummy = json.objectNode();
        dummy.put("role", "assistant".equals(currentRole) ? "user" : "assistant");
        ArrayNode content = json.arrayNode();
        ObjectNode textBlock = json.objectNode();
        textBlock.put("text", ".");
        content.add(textBlock);
        dummy.set("content", content);
        messages.add(dummy);
    }

    private String extractTextFromBedrockOutput(JsonNode source) {
        JsonNode content = source.path("output").path("message").path("content");
        if (!content.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode block : content) {
            if (block.has("text")) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(block.path("text").asText(""));
            }
        }
        return builder.toString();
    }

    private String mapBedrockStopToClaudeStop(String stopReason) {
        return switch (stopReason) {
            case "end_turn", "stop_sequence" -> "end_turn";
            case "max_tokens" -> "max_tokens";
            case "tool_use" -> "tool_use";
            default -> "end_turn";
        };
    }

    private String mapBedrockStopToFinishReason(String stopReason) {
        return switch (stopReason) {
            case "end_turn", "stop_sequence" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            default -> "stop";
        };
    }
}
