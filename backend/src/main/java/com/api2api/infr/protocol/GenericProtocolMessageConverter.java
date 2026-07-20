package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class GenericProtocolMessageConverter extends AbstractProtocolMessageConverter {

    private static final boolean RESPONSES_EXPLICIT_CACHE_BREAKPOINTS_ENABLED = false;

    private static final Set<String> CLAUDE_RESPONSES_REQUEST_FIELDS = Set.of(
            "model", "messages", "max_tokens", "system", "stream", "temperature", "top_p", "top_k",
            "stop_sequences", "metadata", "service_tier", "speed", "thinking", "reasoning", "tool_choice",
            "tools", "cache_control", "output_config", "output_format", "context_management", "container", "mcp_servers",
            "inference_geo", "diagnostics"
    );

    private static final String RESPONSES_OPAQUE_STATE_PLACEHOLDER = "Thinking...";
    private static final String RESPONSES_COMPACTION_PLACEHOLDER = "Context compacted.";
    private static final String RESPONSES_COMPACTION_VISIBLE_TEXT = "Conversation compacted.";

    GenericProtocolMessageConverter(
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
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES
                && targetProtocol() == ProtocolType.OPENAI_RESPONSES) {
            return super.supports(requirement);
        }
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES
                && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            // The request converter and the dedicated streaming adapter preserve
            // Claude tool calls in Chat Completions format.
            return super.supports(requirement);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS
                && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
            // Chat→Claude fully supports tools, tool_calls, multimodal content.
            return super.supports(requirement);
        }
        if (direction() == ProtocolConversionDirection.RESPONSE
                && sourceProtocol() == ProtocolType.OPENAI_RESPONSES
                && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
            return super.supports(requirement);
        }
        if (direction() == ProtocolConversionDirection.RESPONSE
                && ((sourceProtocol() == ProtocolType.OPENAI_RESPONSES
                && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS)
                || (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS
                && targetProtocol() == ProtocolType.OPENAI_RESPONSES))) {
            return super.supports(requirement);
        }
        return !requirement.streaming() && !requirement.toolCallingRequired() && super.supports(requirement);
    }

    @Override
    protected JsonNode convertRequestJson(JsonNode source, ProtocolConversionRequest requirement) {
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return claudeRequestToChat(source);
        }
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.OPENAI_RESPONSES) {
            return claudeRequestToResponses(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
            return chatRequestToClaude(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS && targetProtocol() == ProtocolType.OPENAI_RESPONSES) {
            return chatRequestToResponses(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_RESPONSES && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return responsesRequestToChat(source);
        }
        return responsesRequestToClaude(source);
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return claudeResponseToChat(source);
        }
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.OPENAI_RESPONSES) {
            return claudeResponseToResponses(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
            return chatResponseToClaude(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS && targetProtocol() == ProtocolType.OPENAI_RESPONSES) {
            return chatResponseToResponses(source);
        }
        if (sourceProtocol() == ProtocolType.OPENAI_RESPONSES && targetProtocol() == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return responsesResponseToChat(source);
        }
        return responsesResponseToClaude(source);
    }

    private ObjectNode claudeRequestToChat(JsonNode source) {
        ObjectNode target = json.objectNode();
        String model = source.path("model").asText("");
        target.put("model", model);
        boolean reasoning = isReasoningModel(model);

        if (source.hasNonNull("max_tokens")) {
            target.put("max_completion_tokens", source.get("max_tokens").asInt());
        }
        if (!reasoning) {
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
        }
        copyIfPresent(source, target, "stream");
        if (source.path("stream").asBoolean(false)) {
            ObjectNode streamOptions = json.objectNode();
            streamOptions.put("include_usage", true);
            target.set("stream_options", streamOptions);
        }
        if (source.hasNonNull("stop_sequences")) {
            target.set("stop", source.get("stop_sequences"));
        }
        JsonNode serviceTier = source.get("service_tier");
        if (serviceTier != null && !serviceTier.isNull()) {
            target.put("service_tier", mapClaudeServiceTierToChat(serviceTier.asText("")));
        }
        if (source.path("speed").isTextual() && "fast".equals(source.path("speed").asText())) {
            target.put("service_tier", "priority");
        }

        String effort = chatReasoningEffort(source);
        if (effort != null) {
            target.put("reasoning_effort", effort);
        }

        JsonNode metadata = source.get("metadata");
        if (metadata != null && metadata.hasNonNull("user_id")) {
            target.put("user", metadata.get("user_id").asText());
        }

        JsonNode tools = source.get("tools");
        if (tools != null && tools.isArray() && !tools.isEmpty()) {
            target.set("tools", claudeToolsToChat(tools));
            JsonNode toolChoice = source.get("tool_choice");
            if (toolChoice != null && !toolChoice.isNull()) {
                target.set("tool_choice", claudeToolChoiceToChat(toolChoice));
                if (toolChoice.path("disable_parallel_tool_use").asBoolean(false)) {
                    target.put("parallel_tool_calls", false);
                }
            }
        }

        JsonNode outputConfig = source.get("output_config");
        JsonNode format = outputConfig != null && outputConfig.isObject()
                ? outputConfig.get("format") : source.get("output_format");
        if (format != null && !format.isNull()) {
            if (format.isObject()) {
                String formatType = format.path("type").asText("");
                if ("json_schema".equals(formatType)) {
                    ObjectNode responseFormat = json.objectNode();
                    responseFormat.put("type", "json_schema");
                    ObjectNode jsonSchema = json.objectNode();
                    jsonSchema.put("name", format.path("name").asText("response"));
                    if (format.hasNonNull("schema")) {
                        jsonSchema.set("schema", format.get("schema"));
                    }
                    jsonSchema.put("strict", true);
                    responseFormat.set("json_schema", jsonSchema);
                    target.set("response_format", responseFormat);
                } else if ("json".equals(formatType)) {
                    ObjectNode responseFormat = json.objectNode();
                    responseFormat.put("type", "json_object");
                    target.set("response_format", responseFormat);
                }
            }
        }

        ArrayNode messages = json.arrayNode();
        JsonNode system = source.get("system");
        if (system != null && !system.isNull()) {
            ObjectNode systemMessage = json.objectNode();
            systemMessage.put("role", reasoning ? "developer" : "system");
            systemMessage.put("content", system.isTextual() ? system.asText() : extractOpenAiContentText(system));
            messages.add(systemMessage);
        }
        JsonNode optimizedMessages = ClaudeConversationContextOptimizer.optimize(
                source.get("messages"), source.get("context_management"));
        messages.addAll(claudeMessagesToChatMessages(optimizedMessages));
        target.set("messages", messages);
        return target;
    }

    private String chatReasoningEffort(JsonNode source) {
        JsonNode outputConfig = source.get("output_config");
        if (outputConfig != null && outputConfig.hasNonNull("effort")) {
            String effort = outputConfig.get("effort").asText("high");
            return switch (effort.toLowerCase()) {
                case "max" -> "xhigh";
                case "low" -> "low";
                case "medium" -> "medium";
                default -> "high";
            };
        }
        JsonNode thinking = source.get("thinking");
        if (thinking == null) thinking = source.get("reasoning");
        if (thinking != null && thinking.hasNonNull("budget_tokens")) {
            int budget = thinking.get("budget_tokens").asInt(0);
            if (budget <= 1024) return "low";
            if (budget <= 8192) return "medium";
            return "high";
        }
        return null;
    }

    private ArrayNode claudeToolsToChat(JsonNode tools) {
        ArrayNode result = json.arrayNode();
        for (JsonNode tool : tools) {
            String type = tool.path("type").asText("custom");
            if (!"custom".equals(type) && !type.isBlank()) {
                throw new ProtocolConversionException("CLAUDE_CHAT_TOOL_NOT_SUPPORTED: " + type);
            }
            String name = tool.path("name").asText("");
            if (name.isBlank()) {
                throw new ProtocolConversionException("CLAUDE_CHAT_TOOL_NAME_REQUIRED");
            }
            ObjectNode chatTool = json.objectNode();
            chatTool.put("type", "function");
            ObjectNode function = json.objectNode();
            function.put("name", name);
            if (tool.hasNonNull("description")) {
                function.put("description", tool.get("description").asText(""));
            }
            JsonNode schema = tool.get("input_schema");
            ObjectNode parameters = schema == null || schema.isNull() || !schema.isObject()
                    ? json.objectNode()
                    : (ObjectNode) schema.deepCopy();
            if (!parameters.has("type")) {
                parameters.put("type", "object");
            }
            if (!parameters.has("properties")) {
                parameters.set("properties", json.objectNode());
            }
            function.set("parameters", parameters);
            if (tool.hasNonNull("strict")) {
                function.put("strict", tool.path("strict").asBoolean());
            }
            chatTool.set("function", function);
            result.add(chatTool);
        }
        return result;
    }

    private String mapClaudeServiceTierToChat(String serviceTier) {
        return switch (serviceTier) {
            case "standard_only" -> "default";
            case "priority" -> "priority";
            case "flex" -> "flex";
            case "auto" -> "auto";
            case "batch" -> throw new ProtocolConversionException(
                    "CLAUDE_CHAT_SERVICE_TIER_NOT_SUPPORTED: batch");
            default -> "default";
        };
    }

    private JsonNode claudeToolChoiceToChat(JsonNode toolChoice) {
        String type = toolChoice.isTextual()
                ? toolChoice.asText("auto")
                : toolChoice.path("type").asText("auto");
        return switch (type) {
            case "any" -> json.valueToTree("required");
            case "none" -> json.valueToTree("none");
            case "tool" -> {
                ObjectNode obj = json.objectNode();
                obj.put("type", "function");
                ObjectNode fn = json.objectNode();
                fn.put("name", toolChoice.path("name").asText(""));
                obj.set("function", fn);
                yield obj;
            }
            default -> json.valueToTree("auto");
        };
    }

    private ArrayNode claudeMessagesToChatMessages(JsonNode messages) {
        ArrayNode result = json.arrayNode();
        if (messages == null || !messages.isArray()) return result;
        for (JsonNode message : messages) {
            String role = message.path("role").asText("user");
            JsonNode content = message.get("content");
            if ("assistant".equals(role)) {
                result.addAll(convertAssistantMessageToChat(content));
            } else {
                result.addAll(convertUserMessageToChat(content));
            }
        }
        return result;
    }

    private ArrayNode convertAssistantMessageToChat(JsonNode content) {
        ArrayNode result = json.arrayNode();
        if (content == null || content.isNull()) {
            ObjectNode msg = json.objectNode();
            msg.put("role", "assistant");
            msg.putNull("content");
            result.add(msg);
            return result;
        }
        if (content.isTextual()) {
            ObjectNode msg = json.objectNode();
            msg.put("role", "assistant");
            msg.put("content", content.asText());
            result.add(msg);
            return result;
        }
        StringBuilder textParts = new StringBuilder();
        ArrayNode toolCalls = json.arrayNode();
        for (JsonNode block : content) {
            String type = block.path("type").asText("");
            switch (type) {
                case "text" -> textParts.append(block.path("text").asText(""));
                case "tool_use" -> {
                    ObjectNode call = json.objectNode();
                    call.put("id", block.path("id").asText(""));
                    call.put("type", "function");
                    ObjectNode fn = json.objectNode();
                    fn.put("name", block.path("name").asText(""));
                    fn.put("arguments", block.hasNonNull("input")
                            ? block.get("input").toString() : "{}");
                    call.set("function", fn);
                    toolCalls.add(call);
                }
                case "server_tool_use", "mcp_tool_use", "program", "code_execution_tool_use" ->
                        throw new ProtocolConversionException(
                                "CLAUDE_CHAT_SERVER_TOOL_HISTORY_NOT_SUPPORTED: " + type);
                default -> {} // skip thinking, redacted_thinking etc.
            }
        }
        ObjectNode msg = json.objectNode();
        msg.put("role", "assistant");
        if (!textParts.isEmpty()) {
            msg.put("content", textParts.toString());
        } else {
            msg.putNull("content");
        }
        if (!toolCalls.isEmpty()) {
            msg.set("tool_calls", toolCalls);
        }
        result.add(msg);
        return result;
    }

    private ArrayNode convertUserMessageToChat(JsonNode content) {
        ArrayNode result = json.arrayNode();
        if (content == null || content.isNull()) {
            ObjectNode msg = json.objectNode();
            msg.put("role", "user");
            msg.put("content", "");
            result.add(msg);
            return result;
        }
        if (content.isTextual()) {
            ObjectNode msg = json.objectNode();
            msg.put("role", "user");
            msg.put("content", content.asText());
            result.add(msg);
            return result;
        }
        ArrayNode userParts = json.arrayNode();
        for (JsonNode block : content) {
            String type = block.path("type").asText("");
            switch (type) {
                case "text" -> {
                    ObjectNode part = json.objectNode();
                    part.put("type", "text");
                    part.put("text", block.path("text").asText(""));
                    userParts.add(part);
                }
                case "image" -> {
                    ObjectNode part = json.objectNode();
                    part.put("type", "image_url");
                    ObjectNode imageUrl = json.objectNode();
                    JsonNode imgSource = block.get("source");
                    if (imgSource != null && "base64".equals(imgSource.path("type").asText(""))) {
                        String mediaType = imgSource.path("media_type").asText("image/png");
                        String data = imgSource.path("data").asText("");
                        imageUrl.put("url", "data:" + mediaType + ";base64," + data);
                    } else if (imgSource != null && "url".equals(imgSource.path("type").asText(""))) {
                        imageUrl.put("url", imgSource.path("url").asText(""));
                    }
                    part.set("image_url", imageUrl);
                    userParts.add(part);
                }
                case "document" -> appendChatDocumentPart(userParts, block);
                case "search_result" -> {
                    ObjectNode part = json.objectNode();
                    part.put("type", "text");
                    part.put("text", "Source: " + block.path("source").asText("")
                            + "\nTitle: " + block.path("title").asText("")
                            + "\n" + extractOpenAiContentText(block.get("content")));
                    userParts.add(part);
                }
                case "tool_result" -> {
                    if (!userParts.isEmpty()) {
                        ObjectNode userMsg = json.objectNode();
                        userMsg.put("role", "user");
                        userMsg.set("content", userParts.deepCopy());
                        result.add(userMsg);
                        userParts.removeAll();
                    }
                    ObjectNode toolMsg = json.objectNode();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", block.path("tool_use_id").asText(""));
                    toolMsg.put("content", extractToolResultContent(block));
                    result.add(toolMsg);
                    ArrayNode toolResultImages = extractToolResultImages(block);
                    if (!toolResultImages.isEmpty()) {
                        ObjectNode imgMsg = json.objectNode();
                        imgMsg.put("role", "user");
                        imgMsg.set("content", toolResultImages);
                        result.add(imgMsg);
                    }
                }
                case "server_tool_result", "mcp_tool_result", "web_search_tool_result",
                     "web_fetch_tool_result", "code_execution_tool_result",
                     "bash_code_execution_tool_result", "text_editor_code_execution_tool_result" ->
                        throw new ProtocolConversionException(
                                "CLAUDE_CHAT_SERVER_TOOL_HISTORY_NOT_SUPPORTED: " + type);
                default -> {} // skip thinking etc.
            }
        }
        if (!userParts.isEmpty()) {
            ObjectNode userMsg = json.objectNode();
            userMsg.put("role", "user");
            if (userParts.size() == 1 && "text".equals(userParts.get(0).path("type").asText(""))) {
                userMsg.put("content", userParts.get(0).path("text").asText(""));
            } else {
                userMsg.set("content", userParts);
            }
            result.add(userMsg);
        }
        return result;
    }

    private void appendChatDocumentPart(ArrayNode parts, JsonNode block) {
        JsonNode source = block.path("source");
        String sourceType = source.path("type").asText("");
        if ("text".equals(sourceType) || "content".equals(sourceType)) {
            ObjectNode text = json.objectNode();
            text.put("type", "text");
            text.put("text", "text".equals(sourceType)
                    ? source.path("data").asText("")
                    : extractOpenAiContentText(source.get("content")));
            parts.add(text);
            return;
        }
        if ("url".equals(sourceType)) {
            throw new ProtocolConversionException("CLAUDE_CHAT_DOCUMENT_URL_NOT_SUPPORTED");
        }
        ObjectNode part = json.objectNode();
        part.put("type", "file");
        ObjectNode file = json.objectNode();
        if ("file".equals(sourceType)) {
            file.put("file_id", source.path("file_id").asText(""));
        } else if ("base64".equals(sourceType)) {
            file.put("file_data", source.path("data").asText(""));
        } else {
            throw new ProtocolConversionException("CLAUDE_CHAT_DOCUMENT_SOURCE_NOT_SUPPORTED: " + sourceType);
        }
        String title = block.path("title").asText("");
        if (!title.isBlank()) {
            file.put("filename", title);
        }
        part.set("file", file);
        parts.add(part);
    }

    private String extractToolResultContent(JsonNode toolResult) {
        JsonNode content = toolResult.get("content");
        if (content == null || content.isNull()) return "";
        if (content.isTextual()) return content.asText();
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText(""))) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(item.path("text").asText(""));
                }
            }
            return sb.toString();
        }
        return content.toString();
    }

    private ArrayNode extractToolResultImages(JsonNode toolResult) {
        ArrayNode images = json.arrayNode();
        JsonNode content = toolResult.get("content");
        if (content == null || !content.isArray()) return images;
        for (JsonNode item : content) {
            if ("image".equals(item.path("type").asText(""))) {
                ObjectNode part = json.objectNode();
                part.put("type", "image_url");
                ObjectNode imageUrl = json.objectNode();
                JsonNode imgSource = item.get("source");
                if (imgSource != null && "base64".equals(imgSource.path("type").asText(""))) {
                    String mediaType = imgSource.path("media_type").asText("image/png");
                    String data = imgSource.path("data").asText("");
                    imageUrl.put("url", "data:" + mediaType + ";base64," + data);
                } else if (imgSource != null && "url".equals(imgSource.path("type").asText(""))) {
                    imageUrl.put("url", imgSource.path("url").asText(""));
                }
                part.set("image_url", imageUrl);
                images.add(part);
            }
        }
        return images;
    }

    private ObjectNode claudeRequestToResponses(JsonNode source) {
        validateClaudeResponsesRequest(source);
        if (source.path("max_tokens").isNumber() && source.path("max_tokens").asInt() == 0) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_CACHE_ONLY_REQUEST_NOT_SUPPORTED");
        }
        String model = source.path("model").asText("");
        ObjectNode target = json.objectNode();
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "stream");
        copyIfPresent(source, target, "max_tokens", "max_output_tokens");
        if (!isReasoningModel(model)) {
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
        }
        copyIfPresent(source, target, "metadata");
        if (isReasoningModel(model)) {
            target.put("prompt_cache_key", responsesPromptCacheKey(source, model));
        }
        if (source.hasNonNull("service_tier")) {
            String mappedServiceTier = mapClaudeServiceTierToResponses(
                    source.path("service_tier").asText(""));
            // OpenAI defaults to auto. Omitting that default avoids compatibility
            // failures in Responses-compatible upstream proxies with older schemas.
            if (!"auto".equals(mappedServiceTier)) {
                target.put("service_tier", mappedServiceTier);
            }
        }
        JsonNode speed = source.get("speed");
        String speedType = speed != null && speed.isTextual() ? speed.asText() : source.path("speed").path("type").asText("");
        if ("fast".equals(speedType)) {
            target.put("service_tier", "priority");
        }
        JsonNode contextManagement = responsesContextManagement(source.get("context_management"));
        if (contextManagement != null && !contextManagement.isNull()) {
            target.set("context_management", contextManagement);
        }
        target.put("store", false);
        target.put("parallel_tool_calls", true);
        ArrayNode include = json.arrayNode();
        include.add("reasoning.encrypted_content");
        target.set("include", include);
        ObjectNode text = json.objectNode();
        JsonNode outputConfig = source.get("output_config");
        JsonNode format = outputConfig != null && outputConfig.isObject()
                ? outputConfig.get("format") : source.get("output_format");
        if (format != null && !format.isNull()) {
            text.set("format", ensureResponseTextFormat(format));
        }
        if (!text.isEmpty()) {
            target.set("text", text);
        }
        JsonNode optimizedMessages = ClaudeConversationContextOptimizer.optimize(
                source.get("messages"), source.get("context_management"));
        ArrayNode input = json.arrayNode();
        input.addAll(claudeSystemToResponsesInput(source.get("system"), model));
        input.addAll(claudeMessagesToResponsesInput(optimizedMessages, model));
        applyTopLevelCacheControl(input, source.get("cache_control"), model);
        target.set("input", input);
        ArrayNode mappedTools = claudeToolsToResponses(
                source.get("tools"),
                source.get("mcp_servers"),
                source.get("container"),
                model
        );
        if (!mappedTools.isEmpty()) {
            target.set("tools", mappedTools);
        }
        JsonNode toolChoice = source.get("tool_choice");
        if (toolChoice != null && !toolChoice.isNull()) {
            if (toolChoice.path("disable_parallel_tool_use").asBoolean(false)) {
                target.put("parallel_tool_calls", false);
            }
            JsonNode mappedChoice = claudeToolChoiceToResponses(toolChoice);
            if (mappedChoice != null && !mappedChoice.isNull()) {
                target.set("tool_choice", mappedChoice);
            }
        }
        ObjectNode reasoning = responsesReasoningConfig(source);
        if (!reasoning.isEmpty()) {
            if (!isReasoningModel(model)) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_TARGET_MODEL_DOES_NOT_SUPPORT_REASONING");
            }
            target.set("reasoning", reasoning);
        }
        if (RESPONSES_EXPLICIT_CACHE_BREAKPOINTS_ENABLED && containsResponsesCacheBreakpoint(input)) {
            ObjectNode options = json.objectNode();
            options.put("mode", "explicit");
            options.put("ttl", "30m");
            target.set("prompt_cache_options", options);
        }
        return target;
    }

    private void validateClaudeResponsesRequest(JsonNode source) {
        if (source == null || !source.isObject()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_REQUEST_MUST_BE_OBJECT");
        }
        source.fieldNames().forEachRemaining(field -> {
            if (!CLAUDE_RESPONSES_REQUEST_FIELDS.contains(field)) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_REQUEST_FIELD: " + field);
            }
        });
        JsonNode stopSequences = source.get("stop_sequences");
        if (stopSequences != null && stopSequences.isArray() && !stopSequences.isEmpty()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_STOP_SEQUENCES_NOT_SUPPORTED");
        }
        if (source.hasNonNull("top_k")) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_TOP_K_NOT_SUPPORTED");
        }
        JsonNode outputConfig = source.get("output_config");
        if (outputConfig != null && outputConfig.isObject() && outputConfig.hasNonNull("task_budget")) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_TASK_BUDGET_NOT_SUPPORTED");
        }
        if (source.hasNonNull("inference_geo")) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_INFERENCE_GEO_NOT_SUPPORTED");
        }
        if (source.hasNonNull("diagnostics")) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_CACHE_DIAGNOSTICS_NOT_SUPPORTED");
        }
    }

    private String mapClaudeServiceTierToResponses(String serviceTier) {
        return switch (serviceTier) {
            case "standard_only", "default" -> "default";
            case "auto" -> "auto";
            case "priority" -> "priority";
            case "flex" -> "flex";
            case "batch" -> throw new ProtocolConversionException(
                    "CLAUDE_RESPONSES_SERVICE_TIER_NOT_SUPPORTED: batch");
            default -> throw new ProtocolConversionException(
                    "CLAUDE_RESPONSES_SERVICE_TIER_NOT_SUPPORTED: " + serviceTier);
        };
    }

    private ArrayNode claudeSystemToResponsesInput(JsonNode system, String model) {
        ArrayNode input = json.arrayNode();
        if (system == null || system.isNull()) {
            return input;
        }
        ArrayNode content = json.arrayNode();
        if (system.isTextual()) {
            addClaudeTextPart(content, system.asText(""), "input_text");
        } else if (system.isArray()) {
            for (JsonNode block : system) {
                if ("text".equals(block.path("type").asText(""))) {
                    addClaudeTextPart(content, block.path("text").asText(""), "input_text", block, model);
                }
            }
        }
        if (!content.isEmpty()) {
            ObjectNode mapped = json.objectNode();
            mapped.put("type", "message");
            mapped.put("role", "developer");
            mapped.set("content", content);
            input.add(mapped);
        }
        return input;
    }

    private ArrayNode claudeMidConversationSystemToResponsesInput(JsonNode block, String model) {
        ArrayNode input = claudeSystemToResponsesInput(block.get("content"), model);
        applyTopLevelCacheControl(input, block.get("cache_control"), model);
        return input;
    }

    private ArrayNode claudeMessagesToResponsesInput(JsonNode messages, String model) {
        ArrayNode input = json.arrayNode();
        if (messages == null || !messages.isArray()) {
            return input;
        }
        Map<String, JsonNode> toolCallers = collectClaudeToolCallers(messages);
        for (JsonNode message : messages) {
            String role = message.path("role").asText("user");
            JsonNode content = message.get("content");
            if (content == null || content.isTextual()) {
                ObjectNode mapped = json.objectNode();
                mapped.put("type", "message");
                mapped.put("role", role);
                if ("assistant".equals(role)) {
                    mapped.put("phase", "final_answer");
                }
                ArrayNode parts = json.arrayNode();
                addClaudeTextPart(parts, content == null ? "" : content.asText(""), "assistant".equals(role) ? "output_text" : "input_text");
                mapped.set("content", parts);
                input.add(mapped);
                continue;
            }
            if (!content.isArray()) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_MESSAGE_CONTENT_MUST_BE_TEXT_OR_ARRAY");
            }
            ArrayNode messageContent = json.arrayNode();
            boolean containsCompactionState = containsClaudeCompactionState(content);
            String assistantPhase = "assistant".equals(role)
                    ? (containsClaudeToolUse(content) ? "commentary" : "final_answer")
                    : null;
            for (JsonNode block : content) {
                switch (block.path("type").asText("")) {
                    case "text" -> {
                        String text = block.path("text").asText("");
                        if (!(containsCompactionState && RESPONSES_COMPACTION_VISIBLE_TEXT.equals(text))) {
                            addClaudeTextPart(messageContent, text,
                                    "assistant".equals(role) ? "output_text" : "input_text", block, model);
                        }
                    }
                    case "image" -> addClaudeImagePart(messageContent, block, model);
                    case "document" -> addClaudeDocumentPart(messageContent, block, model);
                    case "search_result" -> addClaudeSearchResultPart(messageContent, block,
                            "assistant".equals(role) ? "output_text" : "input_text", model);
                    case "tool_use" -> {
                        flushResponsesMessage(input, role, messageContent, assistantPhase);
                        input.add(claudeToolUseToResponses(block));
                    }
                    case "tool_result" -> {
                        flushResponsesMessage(input, role, messageContent, assistantPhase);
                        input.add(claudeToolResultToResponses(block, toolCallers, model));
                    }
                    case "server_tool_use" -> {
                        if (!ResponsesProgrammaticToolBridge.isSyntheticProgramToolId(
                                block.path("id").asText(""))) {
                            throw new ProtocolConversionException(
                                    "CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: server_tool_use");
                        }
                    }
                    case "code_execution_tool_result" -> {
                        if (!ResponsesProgrammaticToolBridge.isSyntheticProgramToolId(
                                block.path("tool_use_id").asText(""))) {
                            throw new ProtocolConversionException(
                                    "CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: code_execution_tool_result");
                        }
                    }
                    case "mcp_tool_use", "mcp_tool_result", "web_search_tool_result",
                         "web_fetch_tool_result", "bash_code_execution_tool_result",
                         "text_editor_code_execution_tool_result", "tool_search_tool_result" ->
                            throw new ProtocolConversionException("CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: "
                                    + block.path("type").asText(""));
                    case "thinking" -> {
                        flushResponsesMessage(input, role, messageContent, assistantPhase);
                        claudeThinkingToResponses(block).ifPresent(mappedThinking -> {
                            if ("compaction".equals(mappedThinking.path("type").asText(""))) {
                                input.removeAll();
                            }
                            input.add(mappedThinking);
                        });
                    }
                    case "compaction" -> {
                        flushResponsesMessage(input, role, messageContent, assistantPhase);
                        input.removeAll();
                        input.add(claudeCompactionToResponses(block));
                    }
                    case "mid_conv_system" -> {
                        flushResponsesMessage(input, role, messageContent, assistantPhase);
                        input.addAll(claudeMidConversationSystemToResponsesInput(block, model));
                    }
                    case "fallback" -> {
                        // Claude fallback replay blocks are not rendered into the prompt.
                    }
                    case "redacted_thinking" -> throw new ProtocolConversionException("CLAUDE_RESPONSES_REDACTED_THINKING_NOT_SUPPORTED");
                    default -> throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_CONTENT_BLOCK: " + block.path("type").asText(""));
                }
            }
            flushResponsesMessage(input, role, messageContent, assistantPhase);
        }
        return input;
    }

    private boolean containsClaudeCompactionState(JsonNode content) {
        if (content == null || !content.isArray()) {
            return false;
        }
        for (JsonNode block : content) {
            if (!"thinking".equals(block.path("type").asText(""))) {
                continue;
            }
            JsonNode item = ResponsesReasoningBridge.decodeItem(
                    json.objectMapper(), block.path("signature").asText("")).orElse(null);
            if (item != null && isResponsesCompactionType(item.path("type").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsClaudeToolUse(JsonNode content) {
        if (content == null || !content.isArray()) {
            return false;
        }
        for (JsonNode block : content) {
            if ("tool_use".equals(block.path("type").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, JsonNode> collectClaudeToolCallers(JsonNode messages) {
        Map<String, JsonNode> callers = new HashMap<>();
        for (JsonNode message : messages) {
            JsonNode content = message.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                if (!"tool_use".equals(block.path("type").asText(""))
                        || !block.hasNonNull("caller")) {
                    continue;
                }
                String toolUseId = block.path("id").asText("");
                if (!toolUseId.isBlank()) {
                    callers.put(toolUseId, block.get("caller").deepCopy());
                }
            }
        }
        return Map.copyOf(callers);
    }

    private void flushResponsesMessage(ArrayNode input, String role, ArrayNode content, String assistantPhase) {
        if (content.isEmpty()) {
            return;
        }
        ObjectNode mapped = json.objectNode();
        mapped.put("type", "message");
        mapped.put("role", role);
        if (assistantPhase != null) {
            mapped.put("phase", assistantPhase);
        }
        mapped.set("content", content.deepCopy());
        input.add(mapped);
        content.removeAll();
    }

    private void addClaudeTextPart(ArrayNode content, String value, String type) {
        if (value == null || value.isBlank() || value.startsWith("x-anthropic-billing-header: ")) {
            return;
        }
        ObjectNode text = json.objectNode();
        text.put("type", type);
        text.put("text", value);
        content.add(text);
    }

    private void addClaudeTextPart(
            ArrayNode content,
            String value,
            String type,
            JsonNode source,
            String model
    ) {
        int previousSize = content.size();
        addClaudeTextPart(content, value, type);
        if (content.size() > previousSize) {
            applyResponsesCacheBreakpoint((ObjectNode) content.get(content.size() - 1), source, model);
        }
    }

    private void addClaudeImagePart(ArrayNode content, JsonNode block) {
        addClaudeImagePart(content, block, null);
    }

    private void addClaudeImagePart(ArrayNode content, JsonNode block, String model) {
        JsonNode source = block.get("source");
        if (source == null || source.isNull()) {
            return;
        }
        ObjectNode image = json.objectNode();
        image.put("type", "input_image");
        String sourceType = source.path("type").asText("base64");
        if ("url".equals(sourceType)) {
            image.put("image_url", source.path("url").asText(""));
        } else {
            String mediaType = source.path("media_type").asText("image/png");
            String data = source.path("data").asText("");
            if (data.isBlank()) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_IMAGE_DATA_REQUIRED");
            }
            image.put("image_url", "data:" + mediaType + ";base64," + data);
        }
        image.put("detail", "auto");
        applyResponsesCacheBreakpoint(image, block, model);
        content.add(image);
    }

    private void addClaudeDocumentPart(ArrayNode content, JsonNode block) {
        addClaudeDocumentPart(content, block, null);
    }

    private void addClaudeDocumentPart(ArrayNode content, JsonNode block, String model) {
        JsonNode source = block.path("source");
        ObjectNode file = json.objectNode();
        file.put("type", "input_file");
        String type = source.path("type").asText("");
        switch (type) {
            case "base64" -> file.put("file_data", source.path("data").asText(""));
            case "url" -> file.put("file_url", source.path("url").asText(""));
            case "file" -> file.put("file_id", source.path("file_id").asText(""));
            case "text" -> file.put("file_data", java.util.Base64.getEncoder()
                    .encodeToString(source.path("data").asText("").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            default -> throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_DOCUMENT_SOURCE: " + type);
        }
        if (block.hasNonNull("title")) {
            file.put("filename", block.path("title").asText());
        }
        applyResponsesCacheBreakpoint(file, block, model);
        content.add(file);
    }

    private void addClaudeSearchResultPart(ArrayNode content, JsonNode block, String textType, String model) {
        String text = "Source: " + block.path("source").asText("") + "\nTitle: "
                + block.path("title").asText("") + "\n" + extractOpenAiContentText(block.get("content"));
        addClaudeTextPart(content, text, textType, block, model);
    }

    private void applyResponsesCacheBreakpoint(ObjectNode target, JsonNode source, String model) {
        JsonNode cacheControl = source == null ? null : source.get("cache_control");
        if (cacheControl == null || cacheControl.isNull()) {
            return;
        }
        validateClaudeCacheControl(cacheControl);
        if (!RESPONSES_EXPLICIT_CACHE_BREAKPOINTS_ENABLED || !isResponsesCacheablePart(target)) {
            return;
        }
        ObjectNode breakpoint = json.objectNode();
        breakpoint.put("mode", "explicit");
        target.set("prompt_cache_breakpoint", breakpoint);
    }

    private void applyTopLevelCacheControl(ArrayNode input, JsonNode cacheControl, String model) {
        if (cacheControl == null || cacheControl.isNull()) {
            return;
        }
        validateClaudeCacheControl(cacheControl);
        if (!RESPONSES_EXPLICIT_CACHE_BREAKPOINTS_ENABLED) {
            return;
        }
        ObjectNode target = lastResponsesCacheablePart(input);
        if (target == null) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_CACHE_BREAKPOINT_TARGET_NOT_FOUND");
        }
        ObjectNode breakpoint = json.objectNode();
        breakpoint.put("mode", "explicit");
        target.set("prompt_cache_breakpoint", breakpoint);
    }

    private ObjectNode lastResponsesCacheablePart(ArrayNode input) {
        ObjectNode result = null;
        for (JsonNode item : input) {
            JsonNode content = item.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode part : content) {
                    if (isResponsesCacheablePart(part)) {
                        result = (ObjectNode) part;
                    }
                }
            }
            JsonNode output = item.get("output");
            if (output != null && output.isArray()) {
                for (JsonNode part : output) {
                    if (isResponsesCacheablePart(part)) {
                        result = (ObjectNode) part;
                    }
                }
            }
        }
        return result;
    }

    private boolean isResponsesCacheablePart(JsonNode part) {
        if (part == null || !part.isObject()) {
            return false;
        }
        return switch (part.path("type").asText("")) {
            case "input_text", "input_image", "input_file" -> true;
            default -> false;
        };
    }

    private void validateClaudeCacheControl(JsonNode cacheControl) {
        if (!cacheControl.isObject()
                || !"ephemeral".equals(cacheControl.path("type").asText(""))) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_INVALID_CACHE_CONTROL");
        }
        String ttl = cacheControl.path("ttl").asText("");
        if (!ttl.isBlank() && !"5m".equals(ttl) && !"1h".equals(ttl)) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_CACHE_TTL: " + ttl);
        }
    }

    private boolean containsResponsesCacheBreakpoint(ArrayNode input) {
        for (JsonNode item : input) {
            JsonNode content = item.get("content");
            if (containsResponsesCacheBreakpointPart(content)) {
                return true;
            }
            JsonNode output = item.get("output");
            if (containsResponsesCacheBreakpointPart(output)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsResponsesCacheBreakpointPart(JsonNode parts) {
        if (parts == null || !parts.isArray()) {
            return false;
        }
        for (JsonNode part : parts) {
            if (part.has("prompt_cache_breakpoint")) {
                return true;
            }
        }
        return false;
    }

    private String responsesPromptCacheKey(JsonNode source, String model) {
        String userId = source.path("metadata").path("user_id").asText("");
        String stableIdentity = userId.isBlank()
                ? source.path("system") + "\n" + source.path("tools") + "\n" + firstClaudeUserContent(source.path("messages"))
                : userId;
        String seed = model + "\n" + stableIdentity;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(seed.getBytes(StandardCharsets.UTF_8));
            return "api2api-claude-" + HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_PROMPT_CACHE_KEY_HASH_FAILED", exception);
        }
    }

    private String firstClaudeUserContent(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return "";
        }
        for (JsonNode message : messages) {
            if ("user".equals(message.path("role").asText(""))) {
                return message.path("content").toString();
            }
        }
        return "";
    }

    private Optional<ObjectNode> claudeThinkingToResponses(JsonNode block) {
        String signature = block.path("signature").asText("");
        Optional<JsonNode> hostedItem = ResponsesReasoningBridge.decodeItem(json.objectMapper(), signature);
        if (hostedItem.isPresent()) {
            return Optional.of((ObjectNode) hostedItem.get());
        }
        Optional<JsonNode> decodedState = ResponsesReasoningBridge.decode(json.objectMapper(), signature);
        if (decodedState.isEmpty()) {
            return Optional.empty();
        }
        JsonNode state = decodedState.get();
        ObjectNode reasoning = json.objectNode();
        reasoning.put("type", "reasoning");
        reasoning.put("id", state.path("id").asText());
        reasoning.put("encrypted_content", state.path("encrypted_content").asText());
        ArrayNode summary = json.arrayNode();
        String thinking = block.path("thinking").asText("");
        if (!thinking.isBlank()) {
            ObjectNode summaryText = json.objectNode();
            summaryText.put("type", "summary_text");
            summaryText.put("text", thinking);
            summary.add(summaryText);
        }
        reasoning.set("summary", summary);
        return Optional.of(reasoning);
    }

    private ObjectNode claudeCompactionToResponses(JsonNode block) {
        String summary = block.path("content").asText("");
        if (summary.isBlank()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_COMPACTION_CONTENT_REQUIRED");
        }
        ObjectNode message = json.objectNode();
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("phase", "commentary");
        ArrayNode content = json.arrayNode();
        addClaudeTextPart(content, summary, "output_text");
        message.set("content", content);
        return message;
    }

    private ObjectNode claudeToolUseToResponses(JsonNode block) {
        ObjectNode call = ResponsesToolCallBridge.toResponsesToolCall(json.objectMapper(), block);
        ObjectNode caller = ResponsesProgrammaticToolBridge.toResponsesCaller(
                json.objectMapper(), block.get("caller"));
        if (caller != null) {
            call.set("caller", caller);
        }
        return call;
    }

    private ObjectNode claudeToolResultToResponses(
            JsonNode block,
            Map<String, JsonNode> toolCallers,
            String model
    ) {
        ObjectNode result = json.objectNode();
        String toolUseId = block.path("tool_use_id").asText("");
        boolean custom = ResponsesToolCallBridge.isCustomClaudeToolUseId(toolUseId);
        result.put("type", custom ? "custom_tool_call_output" : "function_call_output");
        result.put("call_id", ResponsesToolCallBridge.toResponsesCallId(toolUseId));
        ObjectNode caller = ResponsesProgrammaticToolBridge.toResponsesCaller(
                json.objectMapper(), toolCallers.get(toolUseId));
        JsonNode output = caller == null
                ? claudeToolResultOutputToResponses(block.get("content"), model)
                : claudeProgrammaticToolResultToResponses(block.get("content"));
        if (output.isArray()) {
            ObjectNode cacheablePart = lastResponsesCacheableContentPart((ArrayNode) output);
            if (cacheablePart != null) {
                applyResponsesCacheBreakpoint(cacheablePart, block, model);
            }
        }
        result.set("output", output);
        if (caller != null) {
            result.set("caller", caller);
        }
        return result;
    }

    private JsonNode claudeToolResultOutputToResponses(JsonNode content, String model) {
        if (content == null || content.isNull() || content.isTextual()) {
            return json.valueToTree(content == null ? "" : content.asText(""));
        }
        if (!content.isArray()) {
            return json.valueToTree(content.toString());
        }
        ArrayNode output = json.arrayNode();
        for (JsonNode block : content) {
            String type = block.path("type").asText("");
            switch (type) {
                case "text" -> {
                    ObjectNode text = json.objectNode();
                    text.put("type", "input_text");
                    text.put("text", block.path("text").asText(""));
                    applyResponsesCacheBreakpoint(text, block, model);
                    output.add(text);
                }
                case "image" -> addClaudeImagePart(output, block, model);
                case "document" -> addClaudeDocumentPart(output, block, model);
                default -> throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_TOOL_RESULT_CONTENT: " + type);
            }
        }
        return output.isEmpty() ? json.valueToTree("") : output;
    }

    private JsonNode claudeProgrammaticToolResultToResponses(JsonNode content) {
        if (content == null || content.isNull() || content.isTextual()) {
            return json.valueToTree(content == null ? "" : content.asText(""));
        }
        if (!content.isArray()) {
            throw new ProtocolConversionException(
                    "CLAUDE_RESPONSES_PROGRAMMATIC_TOOL_RESULT_MUST_BE_TEXT");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if (!"text".equals(block.path("type").asText(""))) {
                throw new ProtocolConversionException(
                        "CLAUDE_RESPONSES_PROGRAMMATIC_TOOL_RESULT_MUST_BE_TEXT");
            }
            text.append(block.path("text").asText(""));
        }
        return json.valueToTree(text.toString());
    }

    private ObjectNode lastResponsesCacheableContentPart(ArrayNode parts) {
        ObjectNode result = null;
        for (JsonNode part : parts) {
            if (isResponsesCacheablePart(part)) {
                result = (ObjectNode) part;
            }
        }
        return result;
    }

    private JsonNode claudeToolChoiceToResponses(JsonNode toolChoice) {
        String type = toolChoice.isTextual() ? toolChoice.asText("auto") : toolChoice.path("type").asText("auto");
        if ("auto".equals(type)) {
            return json.valueToTree("auto");
        }
        if ("any".equals(type)) {
            return json.valueToTree("required");
        }
        if ("tool".equals(type)) {
            ObjectNode choice = json.objectNode();
            choice.put("type", "function");
            choice.put("name", toolChoice.path("name").asText(""));
            return choice;
        }
        return json.valueToTree("none".equals(type) ? "none" : "auto");
    }

    private ObjectNode responsesReasoningConfig(JsonNode source) {
        ObjectNode reasoning = json.objectNode();
        JsonNode outputConfig = source.get("output_config");
        if (outputConfig != null && outputConfig.isObject() && outputConfig.hasNonNull("effort")) {
            String effort = switch (outputConfig.path("effort").asText("medium")) {
                case "low" -> "low";
                case "high" -> "high";
                case "xhigh" -> "xhigh";
                case "max" -> supportsMaxReasoningEffort(source.path("model").asText(""))
                        ? "max"
                        : "xhigh";
                default -> "medium";
            };
            reasoning.put("effort", effort);
        }
        JsonNode thinking = source.hasNonNull("thinking") ? source.get("thinking") : source.get("reasoning");
        if (thinking != null && thinking.isObject()) {
            String type = thinking.path("type").asText("");
            if ("disabled".equals(type)) {
                reasoning.put("effort", "none");
            } else if (!reasoning.has("effort") && "adaptive".equals(type)) {
                reasoning.put("effort", "high");
            } else if (!reasoning.has("effort") && "enabled".equals(type)) {
                reasoning.put("effort", reasoningEffortFromBudget(thinking.path("budget_tokens").asInt(0)));
            }
        }
        if (containsResponsesReasoningState(source.get("messages"))
                && supportsPersistedReasoning(source.path("model").asText(""))) {
            reasoning.put("context", "all_turns");
        }
        if (!reasoning.isEmpty()) {
            reasoning.put("summary", "auto");
        }
        return reasoning;
    }

    private boolean containsResponsesReasoningState(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return false;
        }
        for (JsonNode message : messages) {
            JsonNode content = message.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                if ("thinking".equals(block.path("type").asText(""))
                        && block.path("signature").asText("").startsWith(ResponsesReasoningBridge.SIGNATURE_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String reasoningEffortFromBudget(int budgetTokens) {
        if (budgetTokens >= 4096) {
            return "high";
        }
        if (budgetTokens > 0 && budgetTokens <= 1024) {
            return "low";
        }
        return "medium";
    }

    private ArrayNode claudeToolsToResponses(
            JsonNode tools,
            JsonNode mcpServers,
            JsonNode container,
            String model
    ) {
        ArrayNode mappedTools = json.arrayNode();
        boolean toolSearchRequired = false;
        boolean programmaticToolCallingRequired = false;
        if (tools != null && tools.isArray()) {
            for (JsonNode tool : tools) {
                String type = tool.path("type").asText("custom");
                if ("mcp_toolset".equals(type)) {
                    continue;
                }
                if (type.startsWith("tool_search_tool")) {
                    toolSearchRequired = true;
                    continue;
                }
                if (type.startsWith("web_search")) {
                    ObjectNode mapped = json.objectNode();
                    mapped.put("type", "web_search");
                    if (tool.path("allowed_domains").isArray()) {
                        ObjectNode filters = json.objectNode();
                        filters.set("allowed_domains", tool.path("allowed_domains"));
                        mapped.set("filters", filters);
                    }
                    if (tool.path("user_location").isObject()) {
                        mapped.set("user_location", tool.path("user_location"));
                    }
                    if (tool.path("blocked_domains").isArray() && !tool.path("blocked_domains").isEmpty()) {
                        throw new ProtocolConversionException("CLAUDE_RESPONSES_WEB_SEARCH_BLOCKED_DOMAINS_NOT_SUPPORTED");
                    }
                    mappedTools.add(mapped);
                    continue;
                }
                if (type.startsWith("code_execution")) {
                    ObjectNode mapped = json.objectNode();
                    mapped.put("type", "code_interpreter");
                    if (container != null && container.isTextual() && !container.asText().isBlank()) {
                        mapped.put("container", container.asText());
                    } else {
                        ObjectNode automatic = json.objectNode();
                        automatic.put("type", "auto");
                        mapped.set("container", automatic);
                    }
                    mappedTools.add(mapped);
                    continue;
                }
                if (!"custom".equals(type) && !type.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_SERVER_TOOL_NOT_SUPPORTED: " + type);
                }
                ObjectNode mapped = json.objectNode();
                mapped.put("type", "function");
                mapped.put("name", tool.path("name").asText(""));
                ResponsesProgrammaticToolBridge.AllowedCallersMapping allowedCallers =
                        ResponsesProgrammaticToolBridge.toResponsesAllowedCallers(
                                json.objectMapper(), tool.get("allowed_callers"));
                if (allowedCallers.values() != null && supportsResponsesProgrammaticToolCalling(model)) {
                    mapped.set("allowed_callers", allowedCallers.values());
                }
                programmaticToolCallingRequired |= allowedCallers.programmatic();
                String description = tool.path("description").asText("");
                if (tool.path("input_examples").isArray() && !tool.path("input_examples").isEmpty()) {
                    description = description + (description.isBlank() ? "" : "\n\n")
                            + "Input examples: " + tool.path("input_examples");
                }
                if (!description.isBlank()) {
                    mapped.put("description", description);
                }
                JsonNode schema = tool.get("input_schema");
                ObjectNode parameters = schema == null || schema.isNull() || !schema.isObject()
                        ? json.objectNode()
                        : ((ObjectNode) schema.deepCopy());
                if (!parameters.has("type")) {
                    parameters.put("type", "object");
                }
                if (!parameters.has("properties")) {
                    parameters.set("properties", json.objectNode());
                }
                mapped.set("parameters", parameters);
                mapped.put("strict", tool.path("strict").asBoolean(false));
                if (tool.path("defer_loading").asBoolean(false)) {
                    mapped.put("defer_loading", true);
                    toolSearchRequired = true;
                }
                mappedTools.add(mapped);
            }
        }
        if (mcpServers != null && mcpServers.isArray()) {
            for (JsonNode server : mcpServers) {
                ObjectNode mapped = json.objectNode();
                mapped.put("type", "mcp");
                String name = server.path("name").asText("mcp");
                mapped.put("server_label", name);
                String url = server.path("url").asText("");
                if (url.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_MCP_SERVER_URL_REQUIRED: " + name);
                }
                mapped.put("server_url", url);
                if (server.hasNonNull("authorization_token")) {
                    mapped.put("authorization", server.path("authorization_token").asText());
                }
                ArrayNode allowedTools = mcpAllowedTools(tools, name);
                if (allowedTools != null) {
                    mapped.set("allowed_tools", allowedTools);
                }
                if (mcpDeferred(tools, name)) {
                    mapped.put("defer_loading", true);
                    toolSearchRequired = true;
                }
                mapped.put("require_approval", "never");
                mappedTools.add(mapped);
            }
        }
        if (toolSearchRequired) {
            if (!supportsResponsesToolSearch(model)) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_TARGET_MODEL_DOES_NOT_SUPPORT_TOOL_SEARCH");
            }
            mappedTools.insert(0, json.objectNode().put("type", "tool_search"));
        }
        if (programmaticToolCallingRequired) {
            if (!supportsResponsesProgrammaticToolCalling(model)) {
                throw new ProtocolConversionException(
                        "CLAUDE_RESPONSES_TARGET_MODEL_DOES_NOT_SUPPORT_PROGRAMMATIC_TOOL_CALLING");
            }
            mappedTools.insert(0, json.objectNode().put("type", "programmatic_tool_calling"));
        }
        return mappedTools;
    }

    private boolean mcpDeferred(JsonNode tools, String serverName) {
        if (tools == null || !tools.isArray()) {
            return false;
        }
        for (JsonNode tool : tools) {
            if ("mcp_toolset".equals(tool.path("type").asText(""))
                    && serverName.equals(tool.path("mcp_server_name").asText(""))) {
                return tool.path("defer_loading").asBoolean(false);
            }
        }
        return false;
    }

    private ArrayNode mcpAllowedTools(JsonNode tools, String serverName) {
        if (tools == null || !tools.isArray()) {
            return null;
        }
        for (JsonNode tool : tools) {
            if (!"mcp_toolset".equals(tool.path("type").asText(""))
                    || !serverName.equals(tool.path("mcp_server_name").asText(""))) {
                continue;
            }
            JsonNode defaultConfig = tool.path("default_config");
            JsonNode configs = tool.path("configs");
            boolean defaultEnabled = !defaultConfig.has("enabled") || defaultConfig.path("enabled").asBoolean(true);
            if (defaultEnabled) {
                if (configs.isObject()) {
                    java.util.Iterator<JsonNode> values = configs.elements();
                    while (values.hasNext()) {
                        JsonNode config = values.next();
                        if (config.has("enabled") && !config.path("enabled").asBoolean()) {
                            throw new ProtocolConversionException("CLAUDE_RESPONSES_MCP_DENYLIST_NOT_LOSSLESS: " + serverName);
                        }
                    }
                }
                return null;
            }
            ArrayNode allowed = json.arrayNode();
            if (configs.isObject()) {
                configs.fields().forEachRemaining(entry -> {
                    if (entry.getValue().path("enabled").asBoolean(false)) {
                        allowed.add(entry.getKey());
                    }
                });
            }
            return allowed;
        }
        return null;
    }

    private JsonNode ensureResponseTextFormat(JsonNode format) {
        if (format == null || !format.isObject()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_OUTPUT_FORMAT_MUST_BE_OBJECT");
        }
        ObjectNode normalized = (ObjectNode) format.deepCopy();
        JsonNode nestedSchema = normalized.get("json_schema");
        if (nestedSchema != null && nestedSchema.isObject()) {
            normalized = (ObjectNode) nestedSchema.deepCopy();
        }
        String type = normalized.path("type").asText("");
        if (type.isBlank()) {
            if (normalized.has("schema")) {
                normalized.put("type", "json_schema");
            } else if (normalized.isEmpty()) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_OUTPUT_FORMAT_TYPE_REQUIRED");
            } else {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_OUTPUT_FORMAT_TYPE_REQUIRED");
            }
            type = "json_schema";
        }
        if ("json".equals(type)) {
            normalized.put("type", "json_object");
            return normalized;
        }
        if ("text".equals(type) || "json_object".equals(type)) {
            return normalized;
        }
        if (!"json_schema".equals(type)) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_OUTPUT_FORMAT_TYPE_NOT_SUPPORTED: " + type);
        }
        if (!normalized.hasNonNull("name")) {
            normalized.put("name", "json_response");
        }
        JsonNode schema = normalized.get("schema");
        if (schema == null || !schema.isObject()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_OUTPUT_FORMAT_SCHEMA_REQUIRED");
        }
        return normalized;
    }

    private JsonNode responsesContextManagement(JsonNode source) {
        if (source == null || source.isNull()) {
            return null;
        }
        ArrayNode edits = json.arrayNode();
        if (source.isArray()) {
            edits.addAll((ArrayNode) source);
        } else if (source.isObject() && source.path("edits").isArray()) {
            edits.addAll((ArrayNode) source.path("edits"));
        } else {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_INVALID_CONTEXT_MANAGEMENT");
        }
        ArrayNode converted = json.arrayNode();
        for (JsonNode edit : edits) {
            if (isNoopClearThinkingContextEdit(edit)) {
                continue;
            }
            if ("clear_thinking_20251015".equals(edit.path("type").asText(""))
                    || "clear_tool_uses_20250919".equals(edit.path("type").asText(""))) {
                continue;
            }
            if ("compact_20260112".equals(edit.path("type").asText(""))) {
                if (edit.hasNonNull("instructions") || edit.path("pause_after_compaction").asBoolean(false)) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_COMPACTION_OPTIONS_NOT_LOSSLESS");
                }
                ObjectNode compaction = json.objectNode();
                compaction.put("type", "compaction");
                JsonNode threshold = edit.path("trigger").get("value");
                if (threshold != null && threshold.canConvertToInt()) {
                    compaction.put("compact_threshold", threshold.asInt());
                }
                converted.add(compaction);
            } else {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_CONTEXT_EDIT_NOT_SUPPORTED: "
                        + edit.path("type").asText("unknown"));
            }
        }
        return converted.isEmpty() ? null : converted;
    }

    private boolean isNoopClearThinkingContextEdit(JsonNode edit) {
        JsonNode keep = edit.get("keep");
        boolean keepAll = keep != null && ((keep.isTextual() && "all".equals(keep.asText()))
                || (keep.isObject() && "all".equals(keep.path("type").asText(""))));
        return "clear_thinking_20251015".equals(edit.path("type").asText("")) && keepAll;
    }

    private boolean supportsResponsesToolSearch(String model) {
        return gpt5MinorVersion(model) >= 4;
    }

    private boolean supportsPersistedReasoning(String model) {
        return gpt5MinorVersion(model) >= 6;
    }

    private boolean supportsResponsesProgrammaticToolCalling(String model) {
        return gpt5MinorVersion(model) >= 6;
    }

    private boolean supportsMaxReasoningEffort(String model) {
        return gpt5MinorVersion(model) >= 6;
    }

    private int gpt5MinorVersion(String model) {
        if (model == null) {
            return -1;
        }
        String normalized = model.toLowerCase();
        if (!normalized.startsWith("gpt-5.")) {
            return -1;
        }
        int start = "gpt-5.".length();
        int end = start;
        while (end < normalized.length() && Character.isDigit(normalized.charAt(end))) {
            end++;
        }
        if (end == start) {
            return -1;
        }
        try {
            return Integer.parseInt(normalized.substring(start, end));
        } catch (NumberFormatException exception) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_INVALID_TARGET_MODEL_VERSION: " + model, exception);
        }
    }

    private boolean isReasoningModel(String model) {
        if (model == null) {
            return false;
        }
        String normalized = model.toLowerCase();
        return normalized.startsWith("gpt-5")
                || normalized.startsWith("o1")
                || normalized.startsWith("o3")
                || normalized.startsWith("o4")
                || normalized.contains("codex");
    }

    private ObjectNode chatRequestToClaude(JsonNode source) {
        ObjectNode target = json.objectNode();
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "stream");
        // max_completion_tokens → max_tokens (Chat naming convention)
        if (source.hasNonNull("max_completion_tokens")) {
            target.put("max_tokens", source.get("max_completion_tokens").asInt());
        } else {
            copyIfPresent(source, target, "max_tokens");
        }
        copyIfPresent(source, target, "temperature");
        copyIfPresent(source, target, "top_p");
        // stop → stop_sequences
        if (source.hasNonNull("stop")) {
            target.set("stop_sequences", source.get("stop"));
        }
        // tools → Claude tools format
        JsonNode tools = source.get("tools");
        if (tools != null && tools.isArray() && !tools.isEmpty()) {
            target.set("tools", chatToolDefinitionsToClaude(tools));
        }
        // tool_choice + parallel_tool_calls → Claude tool_choice
        JsonNode toolChoice = source.get("tool_choice");
        boolean parallelToolCalls = source.path("parallel_tool_calls").asBoolean(true);
        ObjectNode mappedToolChoice = chatToolChoiceToClaude(toolChoice, parallelToolCalls);
        if (mappedToolChoice != null && !mappedToolChoice.isEmpty()) {
            target.set("tool_choice", mappedToolChoice);
        }

        ArrayNode messages = json.arrayNode();
        StringBuilder system = new StringBuilder();
        JsonNode chatMessages = source.get("messages");
        if (chatMessages != null && chatMessages.isArray()) {
            for (JsonNode message : chatMessages) {
                String role = message.path("role").asText("user");
                if ("system".equals(role) || "developer".equals(role)) {
                    if (!system.isEmpty()) {
                        system.append('\n');
                    }
                    system.append(chatContentToSystemText(message.get("content")));
                    continue;
                }
                if ("tool".equals(role)) {
                    // tool role → user message with tool_result content block
                    messages.add(chatToolMessageToClaude(message));
                    continue;
                }
                ObjectNode mapped = json.objectNode();
                mapped.put("role", "assistant".equals(role) ? "assistant" : "user");
                mapped.set("content", chatMessageContentToClaude(message));
                messages.add(mapped);
            }
        }
        if (!system.isEmpty()) {
            target.put("system", system.toString());
        }
        target.set("messages", messages);
        return target;
    }

    // ---- Chat → Claude helper methods ----

    private ArrayNode chatToolDefinitionsToClaude(JsonNode tools) {
        ArrayNode result = json.arrayNode();
        for (JsonNode tool : tools) {
            if (!"function".equals(tool.path("type").asText("function"))) {
                throw new ProtocolConversionException(
                        "OPENAI_CHAT_CLAUDE_TOOL_TYPE_NOT_SUPPORTED: " + tool.path("type").asText(""));
            }
            JsonNode function = tool.path("function");
            String name = function.path("name").asText("");
            if (name.isBlank()) {
                throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_TOOL_NAME_REQUIRED");
            }
            ObjectNode mapped = json.objectNode();
            mapped.put("name", name);
            if (function.hasNonNull("description")) {
                mapped.put("description", function.path("description").asText(""));
            }
            JsonNode parameters = function.get("parameters");
            mapped.set("input_schema", parameters == null || parameters.isNull()
                    ? json.objectNode().put("type", "object") : parameters.deepCopy());
            if (function.hasNonNull("strict")) {
                mapped.put("strict", function.path("strict").asBoolean());
            }
            result.add(mapped);
        }
        return result;
    }

    private ObjectNode chatToolChoiceToClaude(JsonNode toolChoice, boolean parallelToolCalls) {
        ObjectNode mapped = json.objectNode();
        if (toolChoice == null || toolChoice.isNull()) {
            if (!parallelToolCalls) {
                mapped.put("type", "auto");
                mapped.put("disable_parallel_tool_use", true);
            }
            return mapped;
        }
        if (toolChoice.isTextual()) {
            String value = toolChoice.asText("auto");
            mapped.put("type", switch (value) {
                case "required" -> "any";
                case "none" -> "none";
                default -> "auto";
            });
        } else if ("function".equals(toolChoice.path("type").asText(""))) {
            mapped.put("type", "tool");
            mapped.put("name", toolChoice.path("function").path("name").asText(""));
        } else {
            mapped.put("type", "auto");
        }
        if (!parallelToolCalls) {
            mapped.put("disable_parallel_tool_use", true);
        }
        return mapped;
    }

    private String chatContentToSystemText(JsonNode content) {
        if (content == null || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText("");
        }
        if (!content.isArray()) {
            return content.asText("");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            if ("text".equals(type) || "input_text".equals(type)) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(part.path("text").asText(""));
            }
        }
        return text.toString();
    }

    private ArrayNode chatMessageContentToClaude(JsonNode message) {
        String role = message.path("role").asText("user");
        ArrayNode content = chatContentBlocksToClaude(message.get("content"));
        if ("assistant".equals(role)) {
            // assistant tool_calls → tool_use blocks
            JsonNode toolCalls = message.get("tool_calls");
            if (toolCalls != null && toolCalls.isArray()) {
                for (JsonNode call : toolCalls) {
                    if (!"function".equals(call.path("type").asText("function"))) {
                        throw new ProtocolConversionException(
                                "OPENAI_CHAT_CLAUDE_TOOL_CALL_TYPE_NOT_SUPPORTED: " + call.path("type").asText(""));
                    }
                    content.add(chatFunctionCallToClaudeToolUse(
                            call.path("id").asText(""),
                            call.path("function").path("name").asText(""),
                            call.path("function").path("arguments").asText("{}")));
                }
            }
            // legacy function_call field
            JsonNode functionCall = message.get("function_call");
            if (functionCall != null && !functionCall.isNull() && !functionCall.isMissingNode()) {
                content.add(chatFunctionCallToClaudeToolUse(
                        functionCall.path("name").asText(""),
                        functionCall.path("name").asText(""),
                        functionCall.path("arguments").asText("{}")));
            }
        }
        if (content.isEmpty()) {
            ObjectNode text = json.objectNode();
            text.put("type", "text");
            text.put("text", "");
            content.add(text);
        }
        return content;
    }

    private ObjectNode chatToolMessageToClaude(JsonNode message) {
        ObjectNode mapped = json.objectNode();
        mapped.put("role", "user");
        ArrayNode content = json.arrayNode();
        ObjectNode toolResult = json.objectNode();
        toolResult.put("type", "tool_result");
        toolResult.put("tool_use_id", message.path("tool_call_id").asText(""));
        ArrayNode resultContent = chatContentBlocksToClaude(message.get("content"));
        if (!resultContent.isEmpty()) {
            toolResult.set("content", resultContent);
        }
        content.add(toolResult);
        mapped.set("content", content);
        return mapped;
    }

    private ArrayNode chatContentBlocksToClaude(JsonNode content) {
        ArrayNode blocks = json.arrayNode();
        if (content == null || content.isNull()) {
            return blocks;
        }
        if (content.isTextual()) {
            String text = content.asText("");
            if (!text.isEmpty()) {
                ObjectNode textBlock = json.objectNode();
                textBlock.put("type", "text");
                textBlock.put("text", text);
                blocks.add(textBlock);
            }
            return blocks;
        }
        if (!content.isArray()) {
            ObjectNode textBlock = json.objectNode();
            textBlock.put("type", "text");
            textBlock.put("text", content.asText(""));
            blocks.add(textBlock);
            return blocks;
        }
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            switch (type) {
                case "text" -> {
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("type", "text");
                    textBlock.put("text", part.path("text").asText(""));
                    blocks.add(textBlock);
                }
                case "image_url" -> blocks.add(chatImageUrlToClaudeImage(
                        part.path("image_url").path("url").asText("")));
                case "file" -> blocks.add(chatFileToClaudeDocument(part.path("file")));
                default -> throw new ProtocolConversionException(
                        "OPENAI_CHAT_CLAUDE_CONTENT_PART_NOT_SUPPORTED: " + type);
            }
        }
        return blocks;
    }

    private ObjectNode chatImageUrlToClaudeImage(String url) {
        ObjectNode image = json.objectNode();
        image.put("type", "image");
        ObjectNode source = json.objectNode();
        if (url.startsWith("data:")) {
            int separator = url.indexOf(";base64,");
            if (separator < 0) {
                throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_IMAGE_DATA_URI_INVALID");
            }
            source.put("type", "base64");
            source.put("media_type", url.substring("data:".length(), separator));
            source.put("data", url.substring(separator + ";base64,".length()));
        } else {
            source.put("type", "url");
            source.put("url", url);
        }
        image.set("source", source);
        return image;
    }

    private ObjectNode chatFileToClaudeDocument(JsonNode file) {
        ObjectNode document = json.objectNode();
        document.put("type", "document");
        ObjectNode source = json.objectNode();
        if (file.hasNonNull("file_id")) {
            source.put("type", "file");
            source.put("file_id", file.path("file_id").asText(""));
        } else if (file.hasNonNull("file_data")) {
            source.put("type", "base64");
            source.put("data", file.path("file_data").asText(""));
        } else {
            throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_FILE_SOURCE_REQUIRED");
        }
        document.set("source", source);
        if (file.hasNonNull("filename")) {
            document.put("title", file.path("filename").asText(""));
        }
        return document;
    }

    private ObjectNode chatFunctionCallToClaudeToolUse(String id, String name, String arguments) {
        ObjectNode toolUse = json.objectNode();
        toolUse.put("type", "tool_use");
        toolUse.put("id", id);
        toolUse.put("name", name);
        try {
            toolUse.set("input", json.objectMapper().readTree(
                    arguments == null || arguments.isBlank() ? "{}" : arguments));
        } catch (JsonProcessingException exception) {
            throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_INVALID_TOOL_ARGUMENTS", exception);
        }
        return toolUse;
    }

    private ObjectNode chatRequestToResponses(JsonNode source) {
        ObjectNode target = json.objectNode();
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "stream");
        copyIfPresent(source, target, "max_tokens", "max_output_tokens");
        copyIfPresent(source, target, "temperature");
        copyIfPresent(source, target, "top_p");
        JsonNode messages = source.get("messages");
        ArrayNode input = json.arrayNode();
        StringBuilder instructions = new StringBuilder();
        if (messages != null && messages.isArray()) {
            for (JsonNode message : messages) {
                String role = message.path("role").asText("user");
                String content = message.path("content").asText("");
                if ("system".equals(role)) {
                    if (!instructions.isEmpty()) {
                        instructions.append('\n');
                    }
                    instructions.append(content);
                    continue;
                }
                ObjectNode item = json.objectNode();
                item.put("role", role);
                item.put("content", content);
                input.add(item);
            }
        }
        if (!instructions.isEmpty()) {
            target.put("instructions", instructions.toString());
        }
        target.set("input", input);
        return target;
    }

    private ObjectNode responsesRequestToChat(JsonNode source) {
        ObjectNode target = json.objectNode();
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "stream");
        copyIfPresent(source, target, "max_output_tokens", "max_tokens");
        copyIfPresent(source, target, "temperature");
        copyIfPresent(source, target, "top_p");
        ArrayNode messages = json.arrayNode();
        JsonNode instructions = source.get("instructions");
        if (instructions != null && !instructions.isNull()) {
            ObjectNode system = json.objectNode();
            system.put("role", "system");
            system.put("content", instructions.asText(""));
            messages.add(system);
        }
        messages.addAll(responsesInputToChatMessages(source.get("input")));
        target.set("messages", messages);
        return target;
    }

    private ObjectNode responsesRequestToClaude(JsonNode source) {
        ObjectNode chat = responsesRequestToChat(source);
        return chatRequestToClaude(chat);
    }

    private ObjectNode claudeResponseToChat(JsonNode source) {
        ObjectNode target = json.objectNode();
        target.put("id", source.path("id").asText("chatcmpl-api2api"));
        target.put("object", "chat.completion");
        target.put("created", Instant.now().getEpochSecond());
        target.put("model", source.path("model").asText(""));
        ArrayNode choices = json.arrayNode();
        ObjectNode choice = json.objectNode();
        choice.put("index", 0);
        ObjectNode message = json.objectNode();
        message.put("role", "assistant");

        JsonNode contentBlocks = source.get("content");
        StringBuilder textContent = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder();
        ArrayNode toolCalls = json.arrayNode();
        if (contentBlocks != null && contentBlocks.isArray()) {
            for (JsonNode block : contentBlocks) {
                String type = block.path("type").asText("");
                switch (type) {
                    case "text" -> textContent.append(block.path("text").asText(""));
                    case "thinking" -> thinkingContent.append(block.path("thinking").asText(""));
                    case "tool_use" -> {
                        ObjectNode call = json.objectNode();
                        call.put("id", block.path("id").asText(""));
                        call.put("type", "function");
                        ObjectNode fn = json.objectNode();
                        fn.put("name", block.path("name").asText(""));
                        fn.put("arguments", block.hasNonNull("input")
                                ? block.get("input").toString() : "{}");
                        call.set("function", fn);
                        toolCalls.add(call);
                    }
                    default -> {}
                }
            }
        }
        if (!textContent.isEmpty()) {
            message.put("content", textContent.toString());
        } else {
            message.putNull("content");
        }
        if (!thinkingContent.isEmpty()) {
            message.put("reasoning_content", thinkingContent.toString());
        }
        if (!toolCalls.isEmpty()) {
            message.set("tool_calls", toolCalls);
        }
        choice.set("message", message);
        choice.put("finish_reason", mapStopToFinishReason(requiredClaudeStopReason(source)));
        choices.add(choice);
        target.set("choices", choices);
        target.set("usage", chatUsageFromClaude(source.path("usage")));
        return target;
    }

    private ObjectNode claudeResponseToResponses(JsonNode source) {
        ObjectNode target = json.objectNode();
        target.put("id", source.path("id").asText("resp_api2api"));
        target.put("object", "response");
        target.put("created_at", Instant.now().getEpochSecond());
        target.put("model", source.path("model").asText(""));

        ArrayNode output = json.arrayNode();
        ArrayNode msgParts = json.arrayNode();
        JsonNode content = source.get("content");

        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                String type = block.path("type").asText("");
                switch (type) {
                    case "thinking":
                        ObjectNode reasoning = json.objectNode();
                        reasoning.put("type", "reasoning");
                        reasoning.put("id", "rs_" + source.path("id").asText("").replace("msg_", ""));
                        String signature = block.path("signature").asText("");
                        if (!signature.isEmpty()) {
                            reasoning.put("encrypted_content", signature);
                        }
                        String thinking = block.path("thinking").asText("");
                        if (!thinking.isEmpty()) {
                            ArrayNode summary = json.arrayNode();
                            ObjectNode summaryText = json.objectNode();
                            summaryText.put("type", "summary_text");
                            summaryText.put("text", thinking);
                            summary.add(summaryText);
                            reasoning.set("summary", summary);
                        }
                        output.add(reasoning);
                        break;
                    case "text":
                        ObjectNode textPart = json.objectNode();
                        textPart.put("type", "output_text");
                        textPart.put("text", block.path("text").asText(""));
                        textPart.set("annotations", json.arrayNode());
                        msgParts.add(textPart);
                        break;
                    case "tool_use":
                        ObjectNode functionCall = json.objectNode();
                        functionCall.put("type", "function_call");
                        functionCall.put("id", "fc_" + block.path("id").asText("").replace("toolu_", ""));
                        functionCall.put("call_id", block.path("id").asText(""));
                        functionCall.put("name", block.path("name").asText(""));
                        JsonNode input = block.get("input");
                        functionCall.put("arguments", input != null ? input.toString() : "{}");
                        functionCall.put("status", "completed");
                        output.add(functionCall);
                        break;
                }
            }
        }

        if (!msgParts.isEmpty()) {
            ObjectNode messageItem = json.objectNode();
            messageItem.put("type", "message");
            messageItem.put("role", "assistant");
            messageItem.set("content", msgParts);
            messageItem.put("status", "completed");
            output.add(messageItem);
        }

        target.set("output", output);
        target.put("status", claudeStopReasonToResponsesStatus(requiredClaudeStopReason(source)));
        target.set("usage", responsesUsageFromClaude(source.path("usage")));
        return target;
    }

    private String requiredClaudeStopReason(JsonNode source) {
        JsonNode value = source.get("stop_reason");
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ProtocolConversionException("CLAUDE_MISSING_STOP_REASON");
        }
        return value.asText();
    }

    private String claudeStopReasonToResponsesStatus(String stopReason) {
        return switch (stopReason) {
            case "end_turn", "tool_use", "stop_sequence" -> "completed";
            case "max_tokens" -> "incomplete";
            default -> throw new ProtocolConversionException("CLAUDE_UNSUPPORTED_STOP_REASON: " + stopReason);
        };
    }

    private ObjectNode chatResponseToClaude(JsonNode source) {
        ObjectNode target = json.objectNode();
        JsonNode choice = source.path("choices").path(0);
        target.put("id", source.path("id").asText("msg_api2api"));
        target.put("type", "message");
        target.put("role", "assistant");
        target.put("model", source.path("model").asText(""));
        ArrayNode content = json.arrayNode();

        JsonNode msg = choice.path("message");
        JsonNode reasoning = msg.get("reasoning_content");
        if (reasoning != null && reasoning.isTextual() && !reasoning.asText().isEmpty()) {
            ObjectNode thinkingBlock = json.objectNode();
            thinkingBlock.put("type", "thinking");
            thinkingBlock.put("thinking", reasoning.asText());
            content.add(thinkingBlock);
        }

        String textContent = msg.path("content").asText("");
        if (!textContent.isEmpty()) {
            ObjectNode textBlock = json.objectNode();
            textBlock.put("type", "text");
            textBlock.put("text", textContent);
            content.add(textBlock);
        }

        JsonNode toolCalls = msg.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode call : toolCalls) {
                ObjectNode toolUseBlock = json.objectNode();
                toolUseBlock.put("type", "tool_use");
                toolUseBlock.put("id", call.path("id").asText(""));
                toolUseBlock.put("name", call.path("function").path("name").asText(""));
                String args = call.path("function").path("arguments").asText("{}");
                try {
                    toolUseBlock.set("input", json.objectMapper().readTree(args));
                } catch (JsonProcessingException exception) {
                    throw new ProtocolConversionException(
                            "OPENAI_CHAT_CLAUDE_INVALID_TOOL_ARGUMENTS", exception);
                }
                content.add(toolUseBlock);
            }
        }

        if (content.isEmpty()) {
            ObjectNode emptyText = json.objectNode();
            emptyText.put("type", "text");
            emptyText.put("text", "");
            content.add(emptyText);
        }
        target.set("content", content);
        target.put("stop_reason", mapFinishToStopReason(choice.path("finish_reason").asText("stop")));
        target.set("usage", claudeUsageFromChat(source.path("usage")));
        return target;
    }

    private ObjectNode chatResponseToResponses(JsonNode source) {
        ObjectNode target = json.objectNode();
        JsonNode choice = source.path("choices").path(0);
        target.put("id", source.path("id").asText("resp_api2api"));
        target.put("object", "response");
        target.put("created_at", source.path("created").asLong(Instant.now().getEpochSecond()));
        target.put("model", source.path("model").asText(""));
        JsonNode message = choice.path("message");
        ArrayNode output = json.arrayNode();
        String text = message.path("content").asText("");
        if (!text.isEmpty()) {
            output.add(outputMessage(text));
        }
        JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode call : toolCalls) {
                ObjectNode functionCall = json.objectNode();
                functionCall.put("type", "function_call");
                functionCall.put("call_id", call.path("id").asText(""));
                functionCall.put("name", call.path("function").path("name").asText(""));
                functionCall.put("arguments", call.path("function").path("arguments").asText("{}"));
                output.add(functionCall);
            }
        }
        target.set("output", output);
        target.set("usage", responsesUsageFromChat(source.path("usage")));
        return target;
    }

    private ObjectNode responsesResponseToChat(JsonNode source) {
        throwIfResponsesFailed(source);
        ObjectNode target = json.objectNode();
        target.put("id", source.path("id").asText("chatcmpl-api2api"));
        target.put("object", "chat.completion");
        target.put("created", source.path("created_at").asLong(Instant.now().getEpochSecond()));
        target.put("model", source.path("model").asText(""));
        ArrayNode choices = json.arrayNode();
        ObjectNode choice = json.objectNode();
        choice.put("index", 0);
        ObjectNode message = json.objectNode();
        message.put("role", "assistant");
        StringBuilder text = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        ArrayNode toolCalls = json.arrayNode();
        JsonNode output = source.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                String type = item.path("type").asText("");
                if ("message".equals(type)) {
                    JsonNode parts = item.get("content");
                    if (parts != null && parts.isArray()) {
                        for (JsonNode part : parts) {
                            if ("output_text".equals(part.path("type").asText(""))) {
                                text.append(part.path("text").asText(""));
                            } else if ("refusal".equals(part.path("type").asText(""))) {
                                text.append(part.path("refusal").asText(""));
                            }
                        }
                    }
                } else if (ResponsesToolCallBridge.isToolCall(type)) {
                    ObjectNode call = json.objectNode();
                    call.put("id", item.path("call_id").asText(item.path("id").asText("")));
                    call.put("type", "function");
                    ObjectNode function = json.objectNode();
                    function.put("name", item.path("name").asText(""));
                    function.put("arguments", ResponsesToolCallBridge
                            .toClaudeToolInput(json.objectMapper(), item).toString());
                    call.set("function", function);
                    toolCalls.add(call);
                } else if ("reasoning".equals(type)) {
                    JsonNode summary = item.get("summary");
                    if (summary != null && summary.isArray()) {
                        for (JsonNode part : summary) {
                            reasoning.append(part.path("text").asText(""));
                        }
                    }
                }
            }
        }
        message.put("content", text.toString());
        if (!reasoning.isEmpty()) {
            message.put("reasoning_content", reasoning.toString());
        }
        if (!toolCalls.isEmpty()) {
            message.set("tool_calls", toolCalls);
        }
        choice.set("message", message);
        choice.put("finish_reason", responsesFinishReasonToChat(source, toolCalls));
        choices.add(choice);
        target.set("choices", choices);
        target.set("usage", chatUsageFromResponses(source.path("usage")));
        return target;
    }

    private String responsesFinishReasonToChat(JsonNode source, ArrayNode toolCalls) {
        if (!toolCalls.isEmpty()) {
            return "tool_calls";
        }
        if ("incomplete".equals(source.path("status").asText(""))) {
            return "length";
        }
        return "stop";
    }

    private ObjectNode responsesResponseToClaude(JsonNode source) {
        throwIfResponsesFailed(source);
        ObjectNode target = json.objectNode();
        target.put("id", source.path("id").asText("msg_api2api"));
        target.put("type", "message");
        target.put("role", "assistant");
        target.put("model", source.path("model").asText(""));
        ArrayNode content = responsesOutputToClaudeContent(source.get("output"));
        String outputText = source.path("output_text").asText("");
        if (!outputText.isBlank() && !hasNonEmptyClaudeText(content)) {
            ObjectNode text = json.objectNode();
            text.put("type", "text");
            text.put("text", outputText);
            content.add(text);
        }
        if (!hasNonEmptyClaudeText(content)) {
            String compactionText = responsesCompactionVisibleText(source.get("output"));
            if (!compactionText.isBlank()) {
                ObjectNode text = json.objectNode();
                text.put("type", "text");
                text.put("text", compactionText);
                content.add(text);
            }
        }
        if (content.isEmpty()) {
            ObjectNode text = json.objectNode();
            text.put("type", "text");
            text.put("text", "");
            content.add(text);
        }
        target.set("content", content);
        target.put("stop_reason", responsesStopReason(source));
        target.set("usage", claudeUsageFromResponses(source.path("usage")));
        return target;
    }

    private boolean hasNonEmptyClaudeText(ArrayNode content) {
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText(""))
                    && !block.path("text").asText("").isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String responsesCompactionVisibleText(JsonNode output) {
        if (output == null || !output.isArray()) {
            return "";
        }
        for (JsonNode item : output) {
            if (!isResponsesCompactionType(item.path("type").asText(""))) {
                continue;
            }
            JsonNode summary = item.path("summary");
            if (summary.isArray()) {
                for (JsonNode part : summary) {
                    String text = part.path("text").asText("");
                    if ("summary_text".equals(part.path("type").asText("")) && !text.isBlank()) {
                        return text;
                    }
                }
            }
            return RESPONSES_COMPACTION_VISIBLE_TEXT;
        }
        return "";
    }

    private ArrayNode responsesOutputToClaudeContent(JsonNode output) {
        ArrayNode content = json.arrayNode();
        if (output == null || !output.isArray()) {
            return content;
        }
        for (JsonNode item : output) {
            String type = item.path("type").asText("");
            if ("message".equals(type)) {
                JsonNode parts = item.get("content");
                if (parts != null && parts.isArray()) {
                    for (JsonNode part : parts) {
                        if ("output_text".equals(part.path("type").asText())) {
                            ObjectNode text = json.objectNode();
                            text.put("type", "text");
                            text.put("text", part.path("text").asText(""));
                            content.add(text);
                        } else if ("refusal".equals(part.path("type").asText())) {
                            ObjectNode refusal = json.objectNode();
                            refusal.put("type", "text");
                            refusal.put("text", part.path("refusal").asText(""));
                            content.add(refusal);
                        }
                    }
                }
                continue;
            }
            if (ResponsesToolCallBridge.isToolCall(type)) {
                ObjectNode toolUse = json.objectNode();
                toolUse.put("type", "tool_use");
                toolUse.put("id", ResponsesToolCallBridge.toClaudeToolUseId(item));
                toolUse.put("name", item.path("name").asText(""));
                toolUse.set("input", ResponsesToolCallBridge.toClaudeToolInput(json.objectMapper(), item));
                ObjectNode caller = ResponsesProgrammaticToolBridge.toClaudeCaller(
                        json.objectMapper(), item.get("caller"));
                if (caller != null) {
                    toolUse.set("caller", caller);
                }
                content.add(toolUse);
                continue;
            }
            if ("reasoning".equals(type)) {
                content.add(responsesReasoningToClaude(item));
                continue;
            }
            if (isResponsesCompactionType(type)) {
                ObjectNode normalizedItem = (ObjectNode) item.deepCopy();
                normalizedItem.put("type", "compaction");
                content.add(responsesOpaqueItemToClaude(normalizedItem, RESPONSES_COMPACTION_PLACEHOLDER));
                continue;
            }
            if ("program".equals(type)) {
                content.add(responsesOpaqueItemToClaude(item, RESPONSES_OPAQUE_STATE_PLACEHOLDER));
                content.add(responsesProgramToClaudeServerTool(item));
                continue;
            }
            if ("program_output".equals(type)) {
                content.add(responsesOpaqueItemToClaude(item, RESPONSES_OPAQUE_STATE_PLACEHOLDER));
                content.add(responsesProgramOutputToClaudeResult(item));
                continue;
            }
            content.add(responsesOpaqueItemToClaude(item, RESPONSES_OPAQUE_STATE_PLACEHOLDER));
        }
        return content;
    }

    private boolean isResponsesCompactionType(String type) {
        return "compaction".equals(type) || "compaction_summary".equals(type);
    }

    private ObjectNode responsesReasoningToClaude(JsonNode item) {
        StringBuilder summaryText = new StringBuilder();
        JsonNode summary = item.get("summary");
        if (summary != null && summary.isArray()) {
            for (JsonNode part : summary) {
                if (part.hasNonNull("text")) {
                    summaryText.append(part.path("text").asText());
                }
            }
        }
        ObjectNode thinking = json.objectNode();
        thinking.put("type", "thinking");
        thinking.put("thinking", summaryText.isEmpty()
                ? RESPONSES_OPAQUE_STATE_PLACEHOLDER
                : summaryText.toString());
        String signature = ResponsesReasoningBridge.encode(json.objectMapper(), item)
                .orElseThrow(() -> new ProtocolConversionException("RESPONSES_CLAUDE_REASONING_STATE_MISSING"));
        thinking.put("signature", signature);
        return thinking;
    }

    private ObjectNode responsesOpaqueItemToClaude(JsonNode item, String placeholder) {
        ObjectNode thinking = json.objectNode();
        thinking.put("type", "thinking");
        thinking.put("thinking", placeholder);
        String signature = ResponsesReasoningBridge.encodeItem(json.objectMapper(), item)
                .orElseThrow(() -> new ProtocolConversionException("RESPONSES_CLAUDE_OUTPUT_ITEM_STATE_MISSING"));
        thinking.put("signature", signature);
        return thinking;
    }

    private ObjectNode responsesProgramToClaudeServerTool(JsonNode item) {
        String callId = item.path("call_id").asText("");
        ObjectNode serverToolUse = json.objectNode();
        serverToolUse.put("type", "server_tool_use");
        serverToolUse.put("id", ResponsesProgrammaticToolBridge.toClaudeProgramToolId(callId));
        serverToolUse.put("name", "code_execution");
        ObjectNode input = json.objectNode();
        input.put("code", item.path("code").asText(""));
        serverToolUse.set("input", input);
        return serverToolUse;
    }

    private ObjectNode responsesProgramOutputToClaudeResult(JsonNode item) {
        String callId = item.path("call_id").asText("");
        boolean completed = "completed".equals(item.path("status").asText(""));
        ObjectNode toolResult = json.objectNode();
        toolResult.put("type", "code_execution_tool_result");
        toolResult.put("tool_use_id", ResponsesProgrammaticToolBridge.toClaudeProgramToolId(callId));
        ObjectNode result = json.objectNode();
        result.put("type", "code_execution_result");
        result.put("stdout", item.path("result").asText(""));
        result.put("stderr", completed ? "" : "Program did not complete.");
        result.put("return_code", completed ? 0 : 1);
        result.set("content", json.arrayNode());
        toolResult.set("content", result);
        return toolResult;
    }

    private String responsesStopReason(JsonNode source) {
        JsonNode output = source.get("output");
        boolean hasOutputItem = false;
        boolean hasFinalMessage = false;
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                hasOutputItem = true;
                if (ResponsesToolCallBridge.isToolCall(item.path("type").asText())) {
                    return "tool_use";
                }
                if ("message".equals(item.path("type").asText(""))) {
                    hasFinalMessage = true;
                }
            }
        }
        if ("incomplete".equals(source.path("status").asText())
                && "max_output_tokens".equals(source.path("incomplete_details").path("reason").asText())) {
            return "max_tokens";
        }
        if ("failed".equals(source.path("status").asText())
                || "cancelled".equals(source.path("status").asText())
                || "content_filter".equals(source.path("incomplete_details").path("reason").asText())) {
            return "refusal";
        }
        if (hasOutputItem && !hasFinalMessage) {
            return "pause_turn";
        }
        return "end_turn";
    }

    private void throwIfResponsesFailed(JsonNode source) {
        String status = source.path("status").asText("");
        if (!"failed".equals(status) && !"cancelled".equals(status) && !"canceled".equals(status)) {
            return;
        }
        String message = source.path("error").path("message").asText("");
        if (message.isBlank()) {
            message = source.path("error").asText("upstream response failed");
        }
        throw new ProtocolConversionException("RESPONSES_CLAUDE_RESPONSE_FAILED: " + message);
    }

    private ArrayNode outputMessage(String value) {
        ArrayNode output = json.arrayNode();
        ObjectNode message = json.objectNode();
        message.put("type", "message");
        message.put("role", "assistant");
        ArrayNode content = json.arrayNode();
        ObjectNode text = json.objectNode();
        text.put("type", "output_text");
        text.put("text", value == null ? "" : value);
        content.add(text);
        message.set("content", content);
        output.add(message);
        return output;
    }

    private ObjectNode chatUsageFromClaude(JsonNode usage) {
        ObjectNode target = json.objectNode();
        long cacheCreation = usage.path("cache_creation_input_tokens").asLong(0);
        long cacheRead = usage.path("cache_read_input_tokens").asLong(0);
        long input = usage.path("input_tokens").asLong(0) + cacheCreation + cacheRead;
        long output = usage.path("output_tokens").asLong(0);
        target.put("prompt_tokens", input);
        target.put("completion_tokens", output);
        target.put("total_tokens", input + output);
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", cacheRead);
        details.put("cache_write_tokens", cacheCreation);
        target.set("prompt_tokens_details", details);
        return target;
    }

    private ObjectNode responsesUsageFromClaude(JsonNode usage) {
        ObjectNode target = json.objectNode();
        long cacheCreation = usage.path("cache_creation_input_tokens").asLong(0);
        long cacheRead = usage.path("cache_read_input_tokens").asLong(0);
        long input = usage.path("input_tokens").asLong(0) + cacheCreation + cacheRead;
        long output = usage.path("output_tokens").asLong(0);
        target.put("input_tokens", input);
        target.put("output_tokens", output);
        target.put("total_tokens", input + output);
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", cacheRead);
        target.set("input_tokens_details", details);
        return target;
    }

    private ObjectNode claudeUsageFromChat(JsonNode usage) {
        ObjectNode target = json.objectNode();
        JsonNode details = usage.path("prompt_tokens_details");
        long cached = details.path("cached_tokens").asLong(0);
        long cacheWrite = details.path("cache_write_tokens").asLong(0);
        target.put("input_tokens", Math.max(0, usage.path("prompt_tokens").asLong(0) - cached - cacheWrite));
        target.put("output_tokens", usage.path("completion_tokens").asLong(0));
        target.put("cache_creation_input_tokens", cacheWrite);
        target.put("cache_read_input_tokens", cached);
        return target;
    }

    private ObjectNode responsesUsageFromChat(JsonNode usage) {
        ObjectNode target = json.objectNode();
        long cached = usage.path("prompt_tokens_details").path("cached_tokens").asLong(0);
        long input = usage.path("prompt_tokens").asLong(0);
        long output = usage.path("completion_tokens").asLong(0);
        target.put("input_tokens", input);
        target.put("output_tokens", output);
        target.put("total_tokens", input + output);
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", cached);
        target.set("input_tokens_details", details);
        return target;
    }

    private ObjectNode chatUsageFromResponses(JsonNode usage) {
        ObjectNode target = json.objectNode();
        long cached = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
        long input = usage.path("input_tokens").asLong(0);
        long output = usage.path("output_tokens").asLong(0);
        target.put("prompt_tokens", input);
        target.put("completion_tokens", output);
        target.put("total_tokens", input + output);
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", cached);
        target.set("prompt_tokens_details", details);
        return target;
    }

    private ObjectNode claudeUsageFromResponses(JsonNode usage) {
        ObjectNode target = json.objectNode();
        long cached = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
        long cacheWrite = usage.path("input_tokens_details").path("cache_write_tokens").asLong(0);
        target.put("input_tokens", Math.max(0,
                usage.path("input_tokens").asLong(0) - cached - cacheWrite));
        target.put("output_tokens", usage.path("output_tokens").asLong(0));
        target.put("cache_creation_input_tokens", cacheWrite);
        target.put("cache_read_input_tokens", cached);
        return target;
    }
}
