package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

abstract class AbstractProtocolMessageConverter implements ProtocolMessageConverter {
    protected final ProtocolJsonSupport json;
    protected final UnifiedUsageExtractor usageExtractor;
    private final ProtocolType sourceProtocol;
    private final ProtocolType targetProtocol;
    private final ProtocolConversionDirection direction;
    private final ConversionCapability capability;
    private final SseEventTransformer sseEventTransformer;

    AbstractProtocolMessageConverter(
            ProtocolJsonSupport json,
            UnifiedUsageExtractor usageExtractor,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            SseEventTransformer sseEventTransformer
    ) {
        this.json = Objects.requireNonNull(json, "json must not be null");
        this.usageExtractor = usageExtractor;
        this.sourceProtocol = Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        this.targetProtocol = Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.sseEventTransformer = Objects.requireNonNull(sseEventTransformer, "sseEventTransformer must not be null");
        Set<ContentMappingType> contentTypes = EnumSet.allOf(ContentMappingType.class);
        this.capability = ConversionCapability.of(true, true, true, true, true, contentTypes);
    }

    @Override
    public ProtocolType sourceProtocol() {
        return sourceProtocol;
    }

    @Override
    public ProtocolType targetProtocol() {
        return targetProtocol;
    }

    @Override
    public ProtocolConversionDirection direction() {
        return direction;
    }

    @Override
    public ConversionCapability capability() {
        return capability;
    }

    @Override
    public ProtocolConversionResult convert(ProtocolPayload payload, ProtocolConversionRequest requirement) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(requirement, "requirement must not be null");
        if (payload.protocol() != sourceProtocol) {
            throw new ProtocolConversionException("payload protocol does not match converter source: " + converterName());
        }
        if (!supports(requirement)) {
            throw new ProtocolConversionException("converter capability does not satisfy request: " + converterName());
        }
        if (payload.streaming() && direction == ProtocolConversionDirection.RESPONSE) {
            String transformed = sseEventTransformer.transform(
                    payload.body(), data -> convertJsonString(data, requirement));
            UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
            return ProtocolConversionResult.of(sourceProtocol, targetProtocol, transformed, false, usage);
        }
        JsonNode source = json.parse(payload.body(), converterName());
        JsonNode target = direction == ProtocolConversionDirection.REQUEST
                ? convertRequestJson(source, requirement)
                : convertResponseJson(source, requirement);
        UnifiedTokenUsage usage = null;
        if (direction == ProtocolConversionDirection.RESPONSE) {
            usage = usageExtractor == null ? UnifiedTokenUsage.unknown() : usageExtractor.extract(source);
        }
        return ProtocolConversionResult.of(sourceProtocol, targetProtocol, json.stringify(target, converterName()), false, usage);
    }

    private String convertJsonString(String data, ProtocolConversionRequest requirement) {
        JsonNode source = json.parse(data, converterName() + " SSE data");
        JsonNode target = direction == ProtocolConversionDirection.REQUEST
                ? convertRequestJson(source, requirement)
                : convertResponseJson(source, requirement);
        return json.stringify(target, converterName() + " SSE data");
    }

    protected abstract JsonNode convertRequestJson(JsonNode source, ProtocolConversionRequest requirement);

    protected abstract JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement);

    protected String converterName() {
        return sourceProtocol + "->" + targetProtocol + " " + direction;
    }

    protected ObjectNode copyCommonRequestFields(JsonNode source, ObjectNode target) {
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "temperature");
        copyIfPresent(source, target, "top_p");
        copyIfPresent(source, target, "stream");
        copyIfPresent(source, target, "metadata");
        return target;
    }

    protected void copyIfPresent(JsonNode source, ObjectNode target, String fieldName) {
        JsonNode value = source.get(fieldName);
        if (value != null && !value.isNull()) {
            target.set(fieldName, value);
        }
    }

    protected void copyIfPresent(JsonNode source, ObjectNode target, String sourceField, String targetField) {
        JsonNode value = source.get(sourceField);
        if (value != null && !value.isNull()) {
            target.set(targetField, value);
        }
    }

    protected String mapStopToFinishReason(String stopReason) {
        if (stopReason == null || stopReason.isBlank()) {
            return "stop";
        }
        return switch (stopReason) {
            case "end_turn", "stop_sequence" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            default -> stopReason;
        };
    }

    protected String mapFinishToStopReason(String finishReason) {
        if (finishReason == null || finishReason.isBlank()) {
            return "end_turn";
        }
        return switch (finishReason) {
            case "stop" -> "end_turn";
            case "length" -> "max_tokens";
            case "tool_calls", "function_call" -> "tool_use";
            default -> finishReason;
        };
    }

    protected String firstTextFromClaudeContent(JsonNode content) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : content) {
                String type = item.path("type").asText("");
                if ("text".equals(type)) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(item.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return content.asText("");
    }

    protected ArrayNode claudeMessagesToOpenAiInput(JsonNode messages) {
        ArrayNode input = json.arrayNode();
        if (messages == null || !messages.isArray()) {
            return input;
        }
        for (JsonNode message : messages) {
            ObjectNode mapped = json.objectNode();
            mapped.put("role", message.path("role").asText("user"));
            mapped.put("content", firstTextFromClaudeContent(message.get("content")));
            input.add(mapped);
        }
        return input;
    }

    protected ArrayNode openAiMessagesToClaudeMessages(JsonNode messages) {
        ArrayNode result = json.arrayNode();
        if (messages == null || !messages.isArray()) {
            return result;
        }
        for (JsonNode message : messages) {
            ObjectNode mapped = json.objectNode();
            String role = message.path("role").asText("user");
            mapped.put("role", "assistant".equals(role) ? "assistant" : "user");
            ArrayNode content = json.arrayNode();
            ObjectNode text = json.objectNode();
            text.put("type", "text");
            text.put("text", message.path("content").asText(""));
            content.add(text);
            mapped.set("content", content);
            result.add(mapped);
        }
        return result;
    }

    protected ArrayNode responsesInputToChatMessages(JsonNode input) {
        ArrayNode messages = json.arrayNode();
        if (input == null || input.isMissingNode() || input.isNull()) {
            return messages;
        }
        if (input.isTextual()) {
            ObjectNode message = json.objectNode();
            message.put("role", "user");
            message.put("content", input.asText());
            messages.add(message);
            return messages;
        }
        if (input.isArray()) {
            for (JsonNode item : input) {
                ObjectNode message = json.objectNode();
                message.put("role", item.path("role").asText("user"));
                message.put("content", extractOpenAiContentText(item.get("content")));
                messages.add(message);
            }
        }
        return messages;
    }

    protected String extractOpenAiContentText(JsonNode content) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : content) {
                String type = item.path("type").asText("");
                if ("input_text".equals(type) || "output_text".equals(type) || "text".equals(type)) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(item.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return content.asText("");
    }

    protected String firstOutputText(JsonNode output) {
        if (output == null || !output.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.get("content");
            String text = extractOpenAiContentText(content);
            if (!text.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(text);
            }
        }
        return builder.toString();
    }
}
