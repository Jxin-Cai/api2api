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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GenericProtocolMessageConverter extends AbstractProtocolMessageConverter {

    private static final Logger log = LoggerFactory.getLogger(GenericProtocolMessageConverter.class);

    private static final boolean RESPONSES_EXPLICIT_CACHE_BREAKPOINTS_ENABLED = false;
    private static final int MIN_CHAT_COMPLETION_TOKENS = 128;
    private static final String EMPTY_TOOL_RESULT = "(empty)";
    private static final String ANTHROPIC_BILLING_HEADER_PREFIX = "x-anthropic-billing-header: ";

    private static final Set<String> CLAUDE_RESPONSES_REQUEST_FIELDS = Set.of(
            "model", "messages", "max_tokens", "system", "stream", "temperature", "top_p", "top_k",
            "stop_sequences", "metadata", "service_tier", "speed", "thinking", "reasoning", "tool_choice",
            "tools", "cache_control", "output_config", "output_format", "context_management", "container", "mcp_servers",
            "inference_geo", "diagnostics"
    );

    private static final String RESPONSES_OPAQUE_STATE_PLACEHOLDER = ResponsesProtocolConstants.OPAQUE_STATE_PLACEHOLDER;
    private static final String RESPONSES_COMPACTION_PLACEHOLDER = ResponsesProtocolConstants.COMPACTION_PLACEHOLDER;
    private static final String RESPONSES_COMPACTION_VISIBLE_TEXT = ResponsesProtocolConstants.COMPACTION_VISIBLE_TEXT;

    private final Function<JsonNode, JsonNode> requestConverter;
    private final Function<JsonNode, JsonNode> responseConverter;
    private final boolean fullStreamingSupport;
    private final List<String> reasoningModelPrefixes;
    private final List<String> reasoningModelContains;
    private final ResponsesToClaudeRequestConverter responsesToClaudeRequestConverter;

    GenericProtocolMessageConverter(
            ProtocolJsonSupport json,
            UnifiedUsageExtractor usageExtractor,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            SseEventTransformer sseEventTransformer,
            ProtocolConversionProperties properties
    ) {
        super(json, usageExtractor, sourceProtocol, targetProtocol, direction, sseEventTransformer);
        this.requestConverter = resolveRequestConverter(sourceProtocol, targetProtocol);
        this.responseConverter = resolveResponseConverter(sourceProtocol, targetProtocol);
        this.fullStreamingSupport = isFullStreamingPair(sourceProtocol, targetProtocol);
        this.reasoningModelPrefixes = properties.getReasoningModelPrefixes();
        this.reasoningModelContains = properties.getReasoningModelContains();
        this.responsesToClaudeRequestConverter = new ResponsesToClaudeRequestConverter(json);
    }

    private Function<JsonNode, JsonNode> resolveRequestConverter(ProtocolType source, ProtocolType target) {
        if (source == ProtocolType.CLAUDE_MESSAGES && target == ProtocolType.OPENAI_CHAT_COMPLETIONS) return this::claudeRequestToChat;
        if (source == ProtocolType.CLAUDE_MESSAGES && target == ProtocolType.OPENAI_RESPONSES) return this::claudeRequestToResponses;
        if (source == ProtocolType.OPENAI_CHAT_COMPLETIONS && target == ProtocolType.CLAUDE_MESSAGES) return this::chatRequestToClaude;
        if (source == ProtocolType.OPENAI_CHAT_COMPLETIONS && target == ProtocolType.OPENAI_RESPONSES) return this::chatRequestToResponses;
        if (source == ProtocolType.OPENAI_RESPONSES && target == ProtocolType.OPENAI_CHAT_COMPLETIONS) return this::responsesRequestToChat;
        return this::responsesRequestToClaude;
    }

    private Function<JsonNode, JsonNode> resolveResponseConverter(ProtocolType source, ProtocolType target) {
        if (source == ProtocolType.CLAUDE_MESSAGES && target == ProtocolType.OPENAI_CHAT_COMPLETIONS) return this::claudeResponseToChat;
        if (source == ProtocolType.CLAUDE_MESSAGES && target == ProtocolType.OPENAI_RESPONSES) return this::claudeResponseToResponses;
        if (source == ProtocolType.OPENAI_CHAT_COMPLETIONS && target == ProtocolType.CLAUDE_MESSAGES) return this::chatResponseToClaude;
        if (source == ProtocolType.OPENAI_CHAT_COMPLETIONS && target == ProtocolType.OPENAI_RESPONSES) return this::chatResponseToResponses;
        if (source == ProtocolType.OPENAI_RESPONSES && target == ProtocolType.OPENAI_CHAT_COMPLETIONS) return this::responsesResponseToChat;
        return this::responsesResponseToClaude;
    }

    private static boolean isFullStreamingPair(ProtocolType source, ProtocolType target) {
        if (source == ProtocolType.CLAUDE_MESSAGES && target == ProtocolType.OPENAI_RESPONSES) return true;
        if (source == ProtocolType.CLAUDE_MESSAGES && target == ProtocolType.OPENAI_CHAT_COMPLETIONS) return true;
        if (source == ProtocolType.OPENAI_CHAT_COMPLETIONS && target == ProtocolType.CLAUDE_MESSAGES) return true;
        if (source == ProtocolType.OPENAI_RESPONSES && target == ProtocolType.CLAUDE_MESSAGES) return true;
        if (source == ProtocolType.OPENAI_RESPONSES && target == ProtocolType.OPENAI_CHAT_COMPLETIONS) return true;
        if (source == ProtocolType.OPENAI_CHAT_COMPLETIONS && target == ProtocolType.OPENAI_RESPONSES) return true;
        return false;
    }

    @Override
    public boolean supports(ProtocolConversionRequest requirement) {
        if (fullStreamingSupport) {
            return super.supports(requirement);
        }
        return !requirement.streaming() && !requirement.toolCallingRequired() && super.supports(requirement);
    }

    @Override
    protected JsonNode convertRequestJson(JsonNode source, ProtocolConversionRequest requirement) {
        return requestConverter.apply(source);
    }

    @Override
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        return responseConverter.apply(source);
    }

    private ObjectNode claudeRequestToChat(JsonNode source) {
        ObjectNode target = json.objectNode();
        String model = source.path("model").asText("");
        target.put("model", model);
        boolean reasoning = isReasoningModel(model);

        mapClaudeToChatModelParameters(source, target, reasoning);
        mapClaudeToChatStreamOptions(source, target);
        mapClaudeToChatServiceTier(source, target);
        mapClaudeToChatReasoningEffort(source, target, reasoning);
        mapClaudeToChatMetadata(source, target);
        mapClaudeToChatTools(source, target);
        mapClaudeToChatOutputFormat(source, target);
        target.set("messages", assembleClaudeToChatMessages(source, reasoning));
        return target;
    }

    private void mapClaudeToChatModelParameters(JsonNode source, ObjectNode target, boolean reasoning) {
        if (source.hasNonNull("max_tokens")) {
            int maxTokens = source.get("max_tokens").asInt();
            target.put("max_completion_tokens", maxTokens > 0
                    ? Math.max(maxTokens, MIN_CHAT_COMPLETION_TOKENS)
                    : maxTokens);
        }
        if (!reasoning) {
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
        }
        if (source.hasNonNull("stop_sequences")) {
            target.set("stop", source.get("stop_sequences"));
        }
    }

    private void mapClaudeToChatStreamOptions(JsonNode source, ObjectNode target) {
        copyIfPresent(source, target, "stream");
        if (source.path("stream").asBoolean(false)) {
            ObjectNode streamOptions = json.objectNode();
            streamOptions.put("include_usage", true);
            target.set("stream_options", streamOptions);
        }
    }

    private void mapClaudeToChatServiceTier(JsonNode source, ObjectNode target) {
        JsonNode serviceTier = source.get("service_tier");
        if (serviceTier != null && !serviceTier.isNull()) {
            target.put("service_tier", mapClaudeServiceTierToChat(serviceTier.asText("")));
        }
        if (source.path("speed").isTextual() && "fast".equals(source.path("speed").asText())) {
            target.put("service_tier", "priority");
        }
    }

    private void mapClaudeToChatReasoningEffort(JsonNode source, ObjectNode target, boolean reasoning) {
        String effort = chatReasoningEffort(source);
        if (effort == null && reasoning) {
            effort = "medium";
        }
        if (effort != null) {
            target.put("reasoning_effort", effort);
        }
    }

    private void mapClaudeToChatMetadata(JsonNode source, ObjectNode target) {
        JsonNode metadata = source.get("metadata");
        if (metadata != null && metadata.hasNonNull("user_id")) {
            target.put("user", metadata.get("user_id").asText());
        }
    }

    private void mapClaudeToChatTools(JsonNode source, ObjectNode target) {
        JsonNode tools = source.get("tools");
        if (tools != null && tools.isArray() && !tools.isEmpty()) {
            target.set("tools", claudeToolsToChat(tools));
            target.put("parallel_tool_calls", true);
            JsonNode toolChoice = source.get("tool_choice");
            if (toolChoice != null && !toolChoice.isNull()) {
                target.set("tool_choice", claudeToolChoiceToChat(toolChoice));
                if (toolChoice.path("disable_parallel_tool_use").asBoolean(false)) {
                    target.put("parallel_tool_calls", false);
                }
            }
        }
    }

    private void mapClaudeToChatOutputFormat(JsonNode source, ObjectNode target) {
        JsonNode outputConfig = source.get("output_config");
        JsonNode format = outputConfig != null && outputConfig.isObject()
                ? outputConfig.get("format") : source.get("output_format");
        if (format != null && !format.isNull() && format.isObject()) {
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

    private ArrayNode assembleClaudeToChatMessages(JsonNode source, boolean reasoning) {
        ArrayNode messages = json.arrayNode();
        JsonNode system = source.get("system");
        if (system != null && !system.isNull()) {
            String systemText = claudeSystemToChatText(system);
            if (!systemText.isBlank()) {
                ObjectNode systemMessage = json.objectNode();
                systemMessage.put("role", reasoning ? "developer" : "system");
                systemMessage.put("content", systemText);
                messages.add(systemMessage);
            }
        }
        JsonNode optimizedMessages = ClaudeConversationContextOptimizer.optimize(
                source.get("messages"), source.get("context_management"));
        messages.addAll(normalizeChatToolHistory(claudeMessagesToChatMessages(optimizedMessages)));
        return messages;
    }

    private String claudeSystemToChatText(JsonNode system) {
        if (system.isTextual()) {
            String text = system.asText("");
            return isAnthropicBillingHeader(text) ? "" : text;
        }
        if (!system.isArray()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : system) {
            if (!"text".equals(block.path("type").asText(""))) {
                continue;
            }
            appendSeparatedText(text, block.path("text").asText(""));
        }
        return text.toString();
    }

    private boolean isAnthropicBillingHeader(String text) {
        return text.startsWith(ANTHROPIC_BILLING_HEADER_PREFIX);
    }

    private void appendSeparatedText(StringBuilder target, String text) {
        if (text.isEmpty() || isAnthropicBillingHeader(text)) {
            return;
        }
        if (!target.isEmpty()) {
            target.append("\n\n");
        }
        target.append(text);
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
            // 与 reasoningEffortFromBudget 同理：budget 推导的 effort 封顶 high，
            // xhigh 仅保留给显式 output_config.effort。
            int budget = thinking.get("budget_tokens").asInt(0);
            if (budget <= 1024) return "low";
            if (budget <= 4096) return "medium";
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
            ObjectNode parameters = normalizeToolInputSchema(tool);
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
                case "text" -> appendSeparatedText(textParts, block.path("text").asText(""));
                case "tool_use" -> toolCalls.add(claudeToolUseToChatFunctionCall(block));
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
                case "image" -> claudeImageSourceToChatImageUrl(block).ifPresent(userParts::add);
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
        if (content == null || content.isNull()) return EMPTY_TOOL_RESULT;
        if (content.isTextual()) {
            return content.asText().isEmpty() ? EMPTY_TOOL_RESULT : content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText(""))) {
                    appendSeparatedText(sb, item.path("text").asText(""));
                }
            }
            return sb.isEmpty() ? EMPTY_TOOL_RESULT : sb.toString();
        }
        return content.toString();
    }

    private ArrayNode extractToolResultImages(JsonNode toolResult) {
        ArrayNode images = json.arrayNode();
        JsonNode content = toolResult.get("content");
        if (content == null || !content.isArray()) return images;
        for (JsonNode item : content) {
            if ("image".equals(item.path("type").asText(""))) {
                claudeImageSourceToChatImageUrl(item).ifPresent(images::add);
            }
        }
        return images;
    }

    private Optional<ObjectNode> claudeImageSourceToChatImageUrl(JsonNode imageBlock) {
        JsonNode imgSource = imageBlock.get("source");
        String url = "";
        if (imgSource != null && "base64".equals(imgSource.path("type").asText(""))) {
            String mediaType = imgSource.path("media_type").asText("image/png");
            String data = imgSource.path("data").asText("");
            if (!data.isBlank()) {
                url = "data:" + mediaType + ";base64," + data;
            }
        } else if (imgSource != null && "url".equals(imgSource.path("type").asText(""))) {
            url = imgSource.path("url").asText("");
        }
        if (url.isBlank()) {
            return Optional.empty();
        }
        ObjectNode part = json.objectNode();
        part.put("type", "image_url");
        ObjectNode imageUrl = json.objectNode();
        imageUrl.put("url", url);
        part.set("image_url", imageUrl);
        return Optional.of(part);
    }

    private ArrayNode normalizeChatToolHistory(ArrayNode messages) {
        Map<String, JsonNode> repliesById = new HashMap<>();
        for (JsonNode message : messages) {
            if ("tool".equals(message.path("role").asText(""))) {
                String toolCallId = message.path("tool_call_id").asText("");
                if (!toolCallId.isBlank()) {
                    repliesById.put(toolCallId, message);
                }
            }
        }

        ArrayNode normalized = json.arrayNode();
        for (JsonNode message : messages) {
            String role = message.path("role").asText("");
            if ("tool".equals(role)) {
                if (message.path("tool_call_id").asText("").isBlank()) {
                    normalized.add(message.deepCopy());
                }
                continue;
            }

            JsonNode toolCalls = message.get("tool_calls");
            if (toolCalls == null || !toolCalls.isArray() || toolCalls.isEmpty()) {
                normalized.add(message.deepCopy());
                continue;
            }

            ArrayNode answeredCalls = json.arrayNode();
            for (JsonNode toolCall : toolCalls) {
                String toolCallId = toolCall.path("id").asText("");
                if (!toolCallId.isBlank() && repliesById.containsKey(toolCallId)) {
                    answeredCalls.add(toolCall.deepCopy());
                }
            }

            ObjectNode normalizedAssistant = (ObjectNode) message.deepCopy();
            if (answeredCalls.isEmpty()) {
                normalizedAssistant.remove("tool_calls");
                if (!isBlankChatMessageContent(normalizedAssistant.get("content"))) {
                    normalized.add(normalizedAssistant);
                }
                continue;
            }

            normalizedAssistant.set("tool_calls", answeredCalls);
            normalized.add(normalizedAssistant);
            for (JsonNode answeredCall : answeredCalls) {
                normalized.add(repliesById.get(answeredCall.path("id").asText()).deepCopy());
            }
        }
        return normalized;
    }

    private ObjectNode claudeToolUseToChatFunctionCall(JsonNode block) {
        ObjectNode call = json.objectNode();
        call.put("id", block.path("id").asText(""));
        call.put("type", "function");
        ObjectNode fn = json.objectNode();
        fn.put("name", block.path("name").asText(""));
        fn.put("arguments", block.hasNonNull("input")
                ? block.get("input").toString() : "{}");
        call.set("function", fn);
        return call;
    }

    private boolean isBlankChatMessageContent(JsonNode content) {
        if (content == null || content.isNull()) {
            return true;
        }
        if (content.isTextual()) {
            return content.asText().isBlank();
        }
        return extractOpenAiContentText(content).isBlank();
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
                    case "text" -> convertTextBlockToResponses(
                            block, messageContent, role, containsCompactionState, model);
                    case "image" -> addClaudeImagePart(messageContent, block, model);
                    case "document" -> addClaudeDocumentPart(messageContent, block, model);
                    case "search_result" -> addClaudeSearchResultPart(messageContent, block,
                            "assistant".equals(role) ? "output_text" : "input_text", model);
                    case "tool_use", "server_tool_use" -> convertToolUseBlockToResponses(
                            block, input, role, messageContent, assistantPhase);
                    case "tool_result", "code_execution_tool_result" -> convertToolResultBlockToResponses(
                            block, input, role, messageContent, assistantPhase, toolCallers, model);
                    case "mcp_tool_use", "mcp_tool_result", "web_search_tool_result",
                         "web_fetch_tool_result", "bash_code_execution_tool_result",
                         "text_editor_code_execution_tool_result", "tool_search_tool_result" ->
                            throw new ProtocolConversionException("CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: "
                                    + block.path("type").asText(""));
                    case "thinking", "redacted_thinking" -> convertThinkingBlockToResponses(
                            block, input, role, messageContent, assistantPhase);
                    case "compaction" -> convertCompactionBlockToResponses(
                            block, input, role, messageContent, assistantPhase);
                    case "mid_conv_system" -> {
                        flushResponsesMessage(input, role, messageContent, assistantPhase);
                        input.addAll(claudeMidConversationSystemToResponsesInput(block, model));
                    }
                    case "fallback" -> {
                        // Claude fallback replay blocks are not rendered into the prompt.
                    }
                    default -> throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_CONTENT_BLOCK: " + block.path("type").asText(""));
                }
            }
            flushResponsesMessage(input, role, messageContent, assistantPhase);
        }
        return input;
    }

    private void convertTextBlockToResponses(JsonNode block, ArrayNode messageContent,
                                               String role, boolean containsCompactionState, String model) {
        String text = block.path("text").asText("");
        if (!(containsCompactionState && RESPONSES_COMPACTION_VISIBLE_TEXT.equals(text))) {
            addClaudeTextPart(messageContent, text,
                    "assistant".equals(role) ? "output_text" : "input_text", block, model);
        }
    }

    private void convertToolUseBlockToResponses(JsonNode block, ArrayNode input,
                                                String role, ArrayNode messageContent, String assistantPhase) {
        String blockType = block.path("type").asText("");
        if ("server_tool_use".equals(blockType)) {
            if (!ResponsesProgrammaticToolBridge.isSyntheticProgramToolId(
                    block.path("id").asText(""))) {
                throw new ProtocolConversionException(
                        "CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: server_tool_use");
            }
        } else {
            flushResponsesMessage(input, role, messageContent, assistantPhase);
            input.add(claudeToolUseToResponses(block));
        }
    }

    private void convertToolResultBlockToResponses(JsonNode block, ArrayNode input,
                                                   String role, ArrayNode messageContent, String assistantPhase,
                                                   Map<String, JsonNode> toolCallers, String model) {
        String blockType = block.path("type").asText("");
        if ("code_execution_tool_result".equals(blockType)) {
            if (!ResponsesProgrammaticToolBridge.isSyntheticProgramToolId(
                    block.path("tool_use_id").asText(""))) {
                throw new ProtocolConversionException(
                        "CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: code_execution_tool_result");
            }
        } else {
            flushResponsesMessage(input, role, messageContent, assistantPhase);
            input.add(claudeToolResultToResponses(block, toolCallers, model));
        }
    }

    private void convertThinkingBlockToResponses(JsonNode block, ArrayNode input,
                                                 String role, ArrayNode messageContent, String assistantPhase) {
        String blockType = block.path("type").asText("");
        if ("redacted_thinking".equals(blockType)) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_REDACTED_THINKING_NOT_SUPPORTED");
        }
        flushResponsesMessage(input, role, messageContent, assistantPhase);
        claudeThinkingToResponses(block).ifPresent(mappedThinking -> {
            if ("compaction".equals(mappedThinking.path("type").asText(""))) {
                input.removeAll();
            }
            input.add(mappedThinking);
        });
    }

    private void convertCompactionBlockToResponses(JsonNode block, ArrayNode input,
                                                   String role, ArrayNode messageContent, String assistantPhase) {
        flushResponsesMessage(input, role, messageContent, assistantPhase);
        input.removeAll();
        input.add(claudeCompactionToResponses(block));
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
        if (value == null || value.isBlank() || isAnthropicBillingHeader(value)) {
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
                case "tool_reference" -> {
                    ObjectNode text = json.objectNode();
                    text.put("type", "input_text");
                    text.put("text", "[tool_reference: " + block.path("tool_name").asText("") + "]");
                    output.add(text);
                }
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
        // 阈值取自 ResponsesToClaudeRequestConverter.thinkingBudgetForEffort 的正向表
        // （low=1024, medium=4096, high=10240），但封顶 high：budget 是 Claude 客户端
        // 开启 thinking 的常规副产品（Claude Code 默认即 31999），xhigh 只有部分上游
        // 接受，仅当用户显式传 output_config.effort=xhigh/max 时才发送。
        if (budgetTokens > 0 && budgetTokens <= 1024) {
            return "low";
        }
        if (budgetTokens <= 4096) {
            return "medium";
        }
        return "high";
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
                    mappedTools.add(mapWebSearchToolToResponses(tool));
                    continue;
                }
                if (type.startsWith("code_execution")) {
                    mappedTools.add(mapCodeExecutionToolToResponses(tool, container));
                    continue;
                }
                if (!"custom".equals(type) && !type.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_SERVER_TOOL_NOT_SUPPORTED: " + type);
                }
                CustomToolMappingResult customToolResult = mapCustomToolToResponses(tool, model, mappedTools);
                toolSearchRequired |= customToolResult.toolSearchRequired();
                programmaticToolCallingRequired |= customToolResult.programmaticToolCallingRequired();
            }
        }
        if (mcpServers != null && mcpServers.isArray()) {
            for (JsonNode server : mcpServers) {
                toolSearchRequired |= mapMcpServerToResponses(server, tools, mappedTools);
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

    private ObjectNode mapWebSearchToolToResponses(JsonNode tool) {
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
        return mapped;
    }

    private ObjectNode mapCodeExecutionToolToResponses(JsonNode tool, JsonNode container) {
        ObjectNode mapped = json.objectNode();
        mapped.put("type", "code_interpreter");
        if (container != null && container.isTextual() && !container.asText().isBlank()) {
            mapped.put("container", container.asText());
        } else {
            ObjectNode automatic = json.objectNode();
            automatic.put("type", "auto");
            mapped.set("container", automatic);
        }
        return mapped;
    }

    private record CustomToolMappingResult(boolean toolSearchRequired, boolean programmaticToolCallingRequired) {}

    /**
     * Maps a custom tool to Responses format and adds it to mappedTools.
     *
     * @return CustomToolMappingResult with toolSearchRequired and programmaticToolCallingRequired flags
     */
    private CustomToolMappingResult mapCustomToolToResponses(JsonNode tool, String model, ArrayNode mappedTools) {
        ObjectNode mapped = json.objectNode();
        mapped.put("type", "function");
        mapped.put("name", tool.path("name").asText(""));
        ResponsesProgrammaticToolBridge.AllowedCallersMapping allowedCallers =
                ResponsesProgrammaticToolBridge.toResponsesAllowedCallers(
                        json.objectMapper(), tool.get("allowed_callers"));
        if (allowedCallers.values() != null && supportsResponsesProgrammaticToolCalling(model)) {
            mapped.set("allowed_callers", allowedCallers.values());
        }
        String description = tool.path("description").asText("");
        if (tool.path("input_examples").isArray() && !tool.path("input_examples").isEmpty()) {
            description = description + (description.isBlank() ? "" : "\n\n")
                    + "Input examples: " + tool.path("input_examples");
        }
        if (!description.isBlank()) {
            mapped.put("description", description);
        }
        ObjectNode parameters = normalizeToolInputSchema(tool);
        mapped.set("parameters", parameters);
        mapped.put("strict", tool.path("strict").asBoolean(false));
        boolean deferLoading = tool.path("defer_loading").asBoolean(false);
        if (deferLoading) {
            mapped.put("defer_loading", true);
        }
        mappedTools.add(mapped);
        return new CustomToolMappingResult(deferLoading, allowedCallers.programmatic());
    }

    private ObjectNode normalizeToolInputSchema(JsonNode tool) {
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
        return parameters;
    }

    /**
     * Maps an MCP server to Responses format and adds it to mappedTools.
     *
     * @return true if tool search is required by this MCP server (due to defer_loading)
     */
    private boolean mapMcpServerToResponses(JsonNode server, JsonNode tools, ArrayNode mappedTools) {
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
        boolean deferred = mcpDeferred(tools, name);
        if (deferred) {
            mapped.put("defer_loading", true);
        }
        mapped.put("require_approval", "never");
        mappedTools.add(mapped);
        return deferred;
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
        return GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5_4);
    }

    private boolean supportsPersistedReasoning(String model) {
        return GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5_6);
    }

    private boolean supportsResponsesProgrammaticToolCalling(String model) {
        return GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5_6);
    }

    private boolean supportsMaxReasoningEffort(String model) {
        return GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5_6);
    }

    /**
     * Reasoning models require special protocol handling:
     * - Disable temperature/top_p parameters (reasoning models manage their own sampling)
     * - Use "developer" role instead of "system" (provider requirement)
     * - Generate reasoning configuration block
     * - Block unsupported protocol conversions (e.g. Claude Responses → non-reasoning)
     *
     * Every GPT generation from gpt-5 onwards is reasoning-only, so those are detected by
     * version rather than by an ever-growing prefix list. Non-versioned families (o-series,
     * codex, ...) stay configurable via api2api.protocol.reasoning-model-prefixes and
     * api2api.protocol.reasoning-model-contains.
     */
    private boolean isReasoningModel(String model) {
        if (model == null) {
            return false;
        }
        if (GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5)) {
            return true;
        }
        String normalized = model.toLowerCase();
        return reasoningModelPrefixes.stream().anyMatch(normalized::startsWith)
                || reasoningModelContains.stream().anyMatch(normalized::contains);
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
        String model = source.path("model").asText("");
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "stream");
        copyIfPresent(source, target, "store");
        copyIfPresent(source, target, "user");
        copyIfPresent(source, target, "parallel_tool_calls");
        mapChatToResponsesModelParameters(source, target, isReasoningModel(model));
        mapChatToResponsesReasoning(source, target);
        mapChatToResponsesTools(source, target);
        mapChatToResponsesTextFormat(source, target);
        target.set("input", chatMessagesToResponsesInput(source.get("messages")));
        return target;
    }

    private void mapChatToResponsesModelParameters(JsonNode source, ObjectNode target, boolean reasoning) {
        JsonNode maxTokens = source.hasNonNull("max_completion_tokens")
                ? source.get("max_completion_tokens")
                : source.get("max_tokens");
        if (maxTokens != null && !maxTokens.isNull()) {
            target.set("max_output_tokens", maxTokens.deepCopy());
        }
        if (!reasoning) {
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
        }
    }

    private void mapChatToResponsesReasoning(JsonNode source, ObjectNode target) {
        if (!source.hasNonNull("reasoning_effort")) {
            return;
        }
        ObjectNode reasoning = json.objectNode();
        reasoning.put("effort", source.get("reasoning_effort").asText(""));
        reasoning.put("summary", "auto");
        target.set("reasoning", reasoning);
    }

    private void mapChatToResponsesTools(JsonNode source, ObjectNode target) {
        ArrayNode tools = json.arrayNode();
        JsonNode chatTools = source.get("tools");
        if (chatTools != null && chatTools.isArray()) {
            for (JsonNode tool : chatTools) {
                JsonNode function = tool.get("function");
                if (function != null && function.isObject()) {
                    tools.add(chatFunctionDefinitionToResponsesTool(function));
                }
            }
        }
        JsonNode legacyFunctions = source.get("functions");
        if (legacyFunctions != null && legacyFunctions.isArray()) {
            for (JsonNode function : legacyFunctions) {
                if (function.isObject()) {
                    tools.add(chatFunctionDefinitionToResponsesTool(function));
                }
            }
        }
        if (!tools.isEmpty()) {
            target.set("tools", tools);
        }
        JsonNode toolChoice = source.hasNonNull("tool_choice")
                ? chatToolChoiceToResponses(source.get("tool_choice"))
                : chatLegacyFunctionCallToResponsesToolChoice(source.get("function_call"));
        if (toolChoice != null) {
            target.set("tool_choice", toolChoice);
        }
    }

    private ObjectNode chatFunctionDefinitionToResponsesTool(JsonNode function) {
        ObjectNode tool = json.objectNode();
        tool.put("type", "function");
        tool.put("name", function.path("name").asText(""));
        if (function.hasNonNull("description")) {
            tool.put("description", function.get("description").asText(""));
        }
        if (function.hasNonNull("parameters")) {
            tool.set("parameters", function.get("parameters").deepCopy());
        }
        tool.put("strict", function.path("strict").asBoolean(false));
        return tool;
    }

    private JsonNode chatToolChoiceToResponses(JsonNode toolChoice) {
        if (toolChoice.isTextual()) {
            return toolChoice.deepCopy();
        }
        if (toolChoice.isObject() && "function".equals(toolChoice.path("type").asText(""))) {
            String name = toolChoice.path("function").path("name").asText("");
            if (!name.isBlank()) {
                ObjectNode mapped = json.objectNode();
                mapped.put("type", "function");
                mapped.put("name", name);
                return mapped;
            }
        }
        return null;
    }

    private JsonNode chatLegacyFunctionCallToResponsesToolChoice(JsonNode functionCall) {
        if (functionCall == null || functionCall.isNull()) {
            return null;
        }
        if (functionCall.isTextual()) {
            String value = functionCall.asText("");
            return "auto".equals(value) || "none".equals(value) ? functionCall.deepCopy() : null;
        }
        String name = functionCall.path("name").asText("");
        if (name.isBlank()) {
            return null;
        }
        ObjectNode mapped = json.objectNode();
        mapped.put("type", "function");
        mapped.put("name", name);
        return mapped;
    }

    private void mapChatToResponsesTextFormat(JsonNode source, ObjectNode target) {
        JsonNode responseFormat = source.get("response_format");
        if (responseFormat == null || !responseFormat.isObject()) {
            return;
        }
        String type = responseFormat.path("type").asText("");
        ObjectNode format;
        if ("json_schema".equals(type)) {
            JsonNode jsonSchema = responseFormat.get("json_schema");
            format = jsonSchema != null && jsonSchema.isObject()
                    ? (ObjectNode) jsonSchema.deepCopy()
                    : json.objectNode();
            format.put("type", "json_schema");
        } else if (!type.isBlank()) {
            format = (ObjectNode) responseFormat.deepCopy();
        } else {
            return;
        }
        ObjectNode text = json.objectNode();
        text.set("format", format);
        target.set("text", text);
    }

    private ArrayNode chatMessagesToResponsesInput(JsonNode messages) {
        ArrayNode input = json.arrayNode();
        if (messages == null || !messages.isArray()) {
            return input;
        }
        for (JsonNode message : messages) {
            String role = message.path("role").asText("user");
            switch (role) {
                case "system", "developer" -> addChatSystemMessageToResponsesInput(input, message, role);
                case "assistant" -> addChatAssistantMessageToResponsesInput(input, message);
                case "tool" -> input.add(responsesFunctionCallOutputItem(
                        message.path("tool_call_id").asText(""),
                        extractOpenAiContentText(message.get("content"))));
                // legacy function 协议没有 call_id，只能沿用函数名对齐 function_call 侧的同名回填
                case "function" -> input.add(responsesFunctionCallOutputItem(
                        message.path("name").asText(""),
                        extractOpenAiContentText(message.get("content"))));
                default -> addChatUserMessageToResponsesInput(input, message, role);
            }
        }
        return input;
    }

    private void addChatSystemMessageToResponsesInput(ArrayNode input, JsonNode message, String role) {
        String text = extractOpenAiContentText(message.get("content"));
        if (text.isBlank()) {
            return;
        }
        ArrayNode parts = json.arrayNode();
        parts.add(responsesTextPart("input_text", text));
        input.add(responsesMessageItem(role, parts));
    }

    private void addChatUserMessageToResponsesInput(ArrayNode input, JsonNode message, String role) {
        JsonNode content = message.get("content");
        ArrayNode parts = json.arrayNode();
        if (content != null && content.isArray()) {
            for (JsonNode part : content) {
                chatContentPartToResponses(part).ifPresent(parts::add);
            }
        } else {
            String text = content == null || content.isNull() ? "" : content.asText("");
            if (!text.isEmpty()) {
                parts.add(responsesTextPart("input_text", text));
            }
        }
        if (parts.isEmpty()) {
            return;
        }
        input.add(responsesMessageItem(role, parts));
    }

    private Optional<ObjectNode> chatContentPartToResponses(JsonNode part) {
        String type = part.path("type").asText("");
        switch (type) {
            case "text" -> {
                return Optional.of(responsesTextPart("input_text", part.path("text").asText("")));
            }
            case "image_url" -> {
                String url = part.path("image_url").path("url").asText("");
                if (url.isBlank() || isEmptyBase64DataUri(url)) {
                    return Optional.empty();
                }
                ObjectNode mapped = json.objectNode();
                mapped.put("type", "input_image");
                mapped.put("image_url", url);
                JsonNode detail = part.path("image_url").get("detail");
                if (detail != null && detail.isTextual()) {
                    mapped.put("detail", detail.asText());
                }
                return Optional.of(mapped);
            }
            case "file" -> {
                return Optional.of(chatFilePartToResponsesInputFile(part.path("file")));
            }
            default -> throw new ProtocolConversionException(
                    "OPENAI_CHAT_RESPONSES_CONTENT_PART_NOT_SUPPORTED: " + type);
        }
    }

    private ObjectNode chatFilePartToResponsesInputFile(JsonNode file) {
        ObjectNode mapped = json.objectNode();
        mapped.put("type", "input_file");
        if (file.hasNonNull("file_id")) {
            mapped.put("file_id", file.get("file_id").asText(""));
        } else if (file.hasNonNull("file_data")) {
            if (file.hasNonNull("filename")) {
                mapped.put("filename", file.get("filename").asText(""));
            }
            mapped.put("file_data", file.get("file_data").asText(""));
        } else {
            throw new ProtocolConversionException("OPENAI_CHAT_RESPONSES_FILE_SOURCE_REQUIRED");
        }
        return mapped;
    }

    private boolean isEmptyBase64DataUri(String url) {
        int marker = url.indexOf(";base64,");
        return url.startsWith("data:") && marker >= 0 && url.length() == marker + ";base64,".length();
    }

    private void addChatAssistantMessageToResponsesInput(ArrayNode input, JsonNode message) {
        String text = assembleChatAssistantResponsesText(message);
        if (!text.isBlank()) {
            ArrayNode parts = json.arrayNode();
            parts.add(responsesTextPart("output_text", text));
            input.add(responsesMessageItem("assistant", parts));
        }
        JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode toolCall : toolCalls) {
                JsonNode function = toolCall.path("function");
                input.add(responsesFunctionCallItem(
                        toolCall.path("id").asText(""),
                        function.path("name").asText(""),
                        function.path("arguments").asText("")));
            }
        }
        JsonNode legacyCall = message.get("function_call");
        if (legacyCall != null && legacyCall.isObject()) {
            String name = legacyCall.path("name").asText("");
            input.add(responsesFunctionCallItem(name, name, legacyCall.path("arguments").asText("")));
        }
    }

    private String assembleChatAssistantResponsesText(JsonNode message) {
        StringBuilder text = new StringBuilder();
        String reasoning = message.path("reasoning_content").asText("");
        if (!reasoning.isBlank()) {
            // Responses input 项没有 reasoning_content 字段，用 thinking 标签保留推理轨迹
            text.append("<thinking>\n").append(reasoning).append("\n</thinking>");
        }
        String content = extractOpenAiContentText(message.get("content"));
        if (!content.isBlank()) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(content);
        }
        return text.toString();
    }

    private ObjectNode responsesFunctionCallItem(String callId, String name, String arguments) {
        ObjectNode item = json.objectNode();
        item.put("type", "function_call");
        item.put("call_id", callId);
        item.put("name", name);
        item.put("arguments", arguments == null || arguments.isBlank() ? "{}" : arguments);
        return item;
    }

    private ObjectNode responsesFunctionCallOutputItem(String callId, String output) {
        ObjectNode item = json.objectNode();
        item.put("type", "function_call_output");
        item.put("call_id", callId);
        item.put("output", output.isBlank() ? EMPTY_TOOL_RESULT : output);
        return item;
    }

    private ObjectNode responsesMessageItem(String role, ArrayNode content) {
        ObjectNode item = json.objectNode();
        item.put("type", "message");
        item.put("role", role);
        item.set("content", content);
        return item;
    }

    private ObjectNode responsesTextPart(String type, String text) {
        ObjectNode part = json.objectNode();
        part.put("type", type);
        part.put("text", text);
        return part;
    }

    private ObjectNode responsesRequestToChat(JsonNode source) {
        ObjectNode target = json.objectNode();
        String model = source.path("model").asText("");
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "user");
        copyIfPresent(source, target, "parallel_tool_calls");
        mapResponsesToChatStreamOptions(source, target);
        mapResponsesToChatModelParameters(source, target, isReasoningModel(model));
        Set<String> functionToolNames = mapResponsesToChatTools(source, target);
        mapResponsesToChatToolChoice(source, target, functionToolNames);
        mapResponsesToChatReasoningEffort(source, target);
        mapResponsesToChatResponseFormat(source, target);
        target.set("messages", assembleResponsesToChatMessages(source));
        return target;
    }

    private void mapResponsesToChatStreamOptions(JsonNode source, ObjectNode target) {
        copyIfPresent(source, target, "stream");
        if (source.path("stream").asBoolean(false)) {
            ObjectNode streamOptions = json.objectNode();
            streamOptions.put("include_usage", true);
            target.set("stream_options", streamOptions);
        }
    }

    private void mapResponsesToChatModelParameters(JsonNode source, ObjectNode target, boolean reasoning) {
        if (source.hasNonNull("max_output_tokens")) {
            int maxTokens = source.get("max_output_tokens").asInt();
            target.put("max_completion_tokens", maxTokens > 0
                    ? Math.max(maxTokens, MIN_CHAT_COMPLETION_TOKENS)
                    : maxTokens);
        }
        if (!reasoning) {
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
        }
    }

    private Set<String> mapResponsesToChatTools(JsonNode source, ObjectNode target) {
        JsonNode tools = source.get("tools");
        if (tools == null || !tools.isArray() || tools.isEmpty()) {
            return Set.of();
        }
        ArrayNode chatTools = json.arrayNode();
        Set<String> functionToolNames = new LinkedHashSet<>();
        for (JsonNode tool : tools) {
            if (!"function".equals(tool.path("type").asText(""))) {
                // 服务端托管工具（web_search、code_interpreter 等）在 Chat 协议没有等价能力，静默丢弃
                continue;
            }
            ObjectNode function = json.objectNode();
            String name = tool.path("name").asText("");
            function.put("name", name);
            if (tool.hasNonNull("description")) {
                function.put("description", tool.get("description").asText(""));
            }
            if (tool.hasNonNull("parameters")) {
                function.set("parameters", tool.get("parameters").deepCopy());
            }
            if (tool.hasNonNull("strict")) {
                function.put("strict", tool.get("strict").asBoolean(false));
            }
            ObjectNode chatTool = json.objectNode();
            chatTool.put("type", "function");
            chatTool.set("function", function);
            chatTools.add(chatTool);
            functionToolNames.add(name);
        }
        if (!chatTools.isEmpty()) {
            target.set("tools", chatTools);
        }
        return functionToolNames;
    }

    private void mapResponsesToChatToolChoice(JsonNode source, ObjectNode target, Set<String> functionToolNames) {
        JsonNode toolChoice = source.get("tool_choice");
        if (toolChoice == null || toolChoice.isNull()) {
            return;
        }
        if (toolChoice.isTextual()) {
            target.set("tool_choice", toolChoice.deepCopy());
            return;
        }
        if ("function".equals(toolChoice.path("type").asText(""))) {
            String name = toolChoice.path("name").asText("");
            // 指定的函数工具若已随服务端工具一并丢弃，则不转发 tool_choice，避免上游校验失败
            if (!name.isBlank() && functionToolNames.contains(name)) {
                ObjectNode function = json.objectNode();
                function.put("name", name);
                ObjectNode mapped = json.objectNode();
                mapped.put("type", "function");
                mapped.set("function", function);
                target.set("tool_choice", mapped);
            }
        }
    }

    private void mapResponsesToChatReasoningEffort(JsonNode source, ObjectNode target) {
        JsonNode reasoning = source.get("reasoning");
        if (reasoning != null && reasoning.hasNonNull("effort")) {
            target.put("reasoning_effort", reasoning.get("effort").asText(""));
        }
    }

    private void mapResponsesToChatResponseFormat(JsonNode source, ObjectNode target) {
        JsonNode format = source.path("text").get("format");
        if (format == null || !format.isObject()) {
            return;
        }
        String type = format.path("type").asText("");
        if ("json_schema".equals(type)) {
            ObjectNode jsonSchema = (ObjectNode) format.deepCopy();
            jsonSchema.remove("type");
            ObjectNode responseFormat = json.objectNode();
            responseFormat.put("type", "json_schema");
            responseFormat.set("json_schema", jsonSchema);
            target.set("response_format", responseFormat);
        } else if (!type.isBlank() && !"text".equals(type)) {
            target.set("response_format", format.deepCopy());
        }
    }

    private ArrayNode assembleResponsesToChatMessages(JsonNode source) {
        ArrayNode messages = json.arrayNode();
        JsonNode instructions = source.get("instructions");
        if (instructions != null && instructions.isTextual() && !instructions.asText().isBlank()) {
            ObjectNode system = json.objectNode();
            system.put("role", "system");
            system.put("content", instructions.asText());
            messages.add(system);
        }
        messages.addAll(responsesInputItemsToChatMessages(source.get("input")));
        return normalizeChatToolHistory(messages);
    }

    private ArrayNode responsesInputItemsToChatMessages(JsonNode input) {
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
        if (!input.isArray()) {
            return messages;
        }
        Set<String> invalidFunctionCallIds = new HashSet<>();
        StringBuilder pendingReasoning = new StringBuilder();
        StringBuilder lastTurnReasoning = new StringBuilder();
        ObjectNode pendingAssistant = null;
        for (JsonNode item : input) {
            if (item.isTextual()) {
                ObjectNode user = json.objectNode();
                user.put("role", "user");
                user.put("content", item.asText());
                messages.add(user);
                pendingAssistant = null;
                pendingReasoning.setLength(0);
                lastTurnReasoning.setLength(0);
                continue;
            }
            String type = responsesInputItemType(item);
            switch (type) {
                case "message" -> {
                    pendingAssistant = null;
                    appendResponsesMessageItemToChat(messages, item, pendingReasoning, lastTurnReasoning);
                }
                case "reasoning" -> {
                    appendSeparatedText(pendingReasoning, extractResponsesReasoningText(item));
                    if (!pendingReasoning.isEmpty()) {
                        lastTurnReasoning.setLength(0);
                        lastTurnReasoning.append(pendingReasoning);
                    }
                }
                case "function_call" -> pendingAssistant = appendResponsesFunctionCallToChat(
                        messages, item, pendingAssistant, pendingReasoning, lastTurnReasoning, invalidFunctionCallIds);
                case "function_call_output" -> {
                    pendingAssistant = null;
                    // 工具输出不结束本轮 thinking，lastTurnReasoning 保留供链式调用复播
                    pendingReasoning.setLength(0);
                    appendResponsesFunctionCallOutputToChat(messages, item, invalidFunctionCallIds);
                }
                default -> {
                    // 服务端工具轨迹（web_search_call 等）在 Chat 协议没有等价物，跳过
                    pendingAssistant = null;
                    pendingReasoning.setLength(0);
                }
            }
        }
        return messages;
    }

    private String responsesInputItemType(JsonNode item) {
        String type = item.path("type").asText("");
        if (type.isBlank() && item.hasNonNull("role")) {
            return "message";
        }
        return type;
    }

    private void appendResponsesMessageItemToChat(
            ArrayNode messages,
            JsonNode item,
            StringBuilder pendingReasoning,
            StringBuilder lastTurnReasoning
    ) {
        String role = item.path("role").asText("user");
        ObjectNode message = json.objectNode();
        message.put("role", "developer".equals(role) ? "system" : role);
        JsonNode content = item.get("content");
        if ("user".equals(role) && content != null && content.isArray() && containsNonTextResponsesPart(content)) {
            message.set("content", responsesUserPartsToChatParts(content));
        } else {
            message.put("content", extractOpenAiContentText(content));
        }
        if ("assistant".equals(role)) {
            // DeepSeek thinking 模型要求 assistant 历史消息回传其 reasoning_content，缺失会报 400
            String reasoning = pendingReasoning.isEmpty() ? lastTurnReasoning.toString() : pendingReasoning.toString();
            if (!reasoning.isBlank()) {
                message.put("reasoning_content", reasoning);
            }
            pendingReasoning.setLength(0);
        } else {
            // user 侧项结束本轮 thinking
            pendingReasoning.setLength(0);
            lastTurnReasoning.setLength(0);
        }
        messages.add(message);
    }

    private boolean containsNonTextResponsesPart(JsonNode content) {
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            if ("input_image".equals(type) || "input_file".equals(type)) {
                return true;
            }
        }
        return false;
    }

    private ArrayNode responsesUserPartsToChatParts(JsonNode content) {
        ArrayNode parts = json.arrayNode();
        for (JsonNode part : content) {
            switch (part.path("type").asText("")) {
                case "input_text", "output_text", "text" -> {
                    ObjectNode text = json.objectNode();
                    text.put("type", "text");
                    text.put("text", part.path("text").asText(""));
                    parts.add(text);
                }
                case "input_image" -> {
                    String url = part.path("image_url").asText("");
                    if (!url.isBlank()) {
                        ObjectNode imageUrl = json.objectNode();
                        imageUrl.put("url", url);
                        if (part.hasNonNull("detail")) {
                            imageUrl.put("detail", part.get("detail").asText(""));
                        }
                        ObjectNode image = json.objectNode();
                        image.put("type", "image_url");
                        image.set("image_url", imageUrl);
                        parts.add(image);
                    }
                }
                case "input_file" -> {
                    ObjectNode file = json.objectNode();
                    if (part.hasNonNull("file_id")) {
                        file.put("file_id", part.get("file_id").asText(""));
                    }
                    if (part.hasNonNull("filename")) {
                        file.put("filename", part.get("filename").asText(""));
                    }
                    if (part.hasNonNull("file_data")) {
                        file.put("file_data", part.get("file_data").asText(""));
                    }
                    ObjectNode mapped = json.objectNode();
                    mapped.put("type", "file");
                    mapped.set("file", file);
                    parts.add(mapped);
                }
                default -> {
                    // 其余部件（refusal 等）在 Chat 用户消息中没有等价表达，跳过
                }
            }
        }
        return parts;
    }

    private String extractResponsesReasoningText(JsonNode item) {
        StringBuilder text = new StringBuilder();
        JsonNode summary = item.get("summary");
        if (summary != null && summary.isArray()) {
            for (JsonNode part : summary) {
                appendSeparatedText(text, part.path("text").asText(""));
            }
        }
        if (text.isEmpty()) {
            JsonNode content = item.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode part : content) {
                    appendSeparatedText(text, part.path("text").asText(""));
                }
            }
        }
        return text.toString();
    }

    private ObjectNode appendResponsesFunctionCallToChat(
            ArrayNode messages,
            JsonNode item,
            ObjectNode pendingAssistant,
            StringBuilder pendingReasoning,
            StringBuilder lastTurnReasoning,
            Set<String> invalidFunctionCallIds
    ) {
        String callId = item.path("call_id").asText(item.path("id").asText(""));
        String arguments = item.path("arguments").asText("");
        if (!isValidFunctionCallArguments(arguments)) {
            // 历史中被截断的工具参数会让上游拒绝整个请求，跳过该调用并连带跳过其结果，自愈污染历史
            if (!callId.isBlank()) {
                invalidFunctionCallIds.add(callId);
            }
            log.warn("Dropping responses function_call with invalid JSON arguments, callId: {}", callId);
            pendingReasoning.setLength(0);
            return pendingAssistant;
        }
        ObjectNode assistant = pendingAssistant;
        if (assistant == null) {
            assistant = json.objectNode();
            assistant.put("role", "assistant");
            assistant.putNull("content");
            assistant.set("tool_calls", json.arrayNode());
            messages.add(assistant);
        }
        if (!pendingReasoning.isEmpty()) {
            // DeepSeek 等 thinking 模型要求触发工具调用的 assistant 消息携带 reasoning_content，否则报 400
            StringBuilder combined = new StringBuilder(assistant.path("reasoning_content").asText(""));
            appendSeparatedText(combined, pendingReasoning.toString());
            assistant.put("reasoning_content", combined.toString());
            pendingReasoning.setLength(0);
        } else if (!assistant.hasNonNull("reasoning_content") && !lastTurnReasoning.isEmpty()) {
            // DeepSeek 每轮只发一次 reasoning，链式调用（reasoning → call A → output A → call B）中
            // call B 的 assistant 消息需复播本轮 reasoning，否则历史校验报 400
            assistant.put("reasoning_content", lastTurnReasoning.toString());
        }
        ObjectNode function = json.objectNode();
        function.put("name", item.path("name").asText(""));
        function.put("arguments", arguments.isBlank() ? "{}" : arguments);
        ObjectNode toolCall = json.objectNode();
        toolCall.put("id", callId);
        toolCall.put("type", "function");
        toolCall.set("function", function);
        ((ArrayNode) assistant.get("tool_calls")).add(toolCall);
        return assistant;
    }

    private boolean isValidFunctionCallArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return true;
        }
        try {
            json.objectMapper().readTree(arguments);
            return true;
        } catch (JsonProcessingException exception) {
            return false;
        }
    }

    private void appendResponsesFunctionCallOutputToChat(
            ArrayNode messages, JsonNode item, Set<String> invalidFunctionCallIds) {
        String callId = item.path("call_id").asText("");
        if (invalidFunctionCallIds.contains(callId)) {
            return;
        }
        ObjectNode message = json.objectNode();
        message.put("role", "tool");
        message.put("tool_call_id", callId);
        String output = responsesFunctionCallOutputText(item.get("output"));
        message.put("content", output.isBlank() ? EMPTY_TOOL_RESULT : output);
        messages.add(message);
    }

    private String responsesFunctionCallOutputText(JsonNode output) {
        if (output == null || output.isNull()) {
            return "";
        }
        if (output.isTextual()) {
            return output.asText();
        }
        if (output.isArray()) {
            return extractOpenAiContentText(output);
        }
        return output.toString();
    }

    private ObjectNode responsesRequestToClaude(JsonNode source) {
        return responsesToClaudeRequestConverter.convert(source);
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
                    case "tool_use" -> toolCalls.add(claudeToolUseToChatFunctionCall(block));
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
        String responseId = toResponsesResponseId(source.path("id").asText(""));
        target.put("id", responseId);
        target.put("object", "response");
        target.put("created_at", Instant.now().getEpochSecond());
        target.put("model", source.path("model").asText(""));
        ObjectNode conversation = claudeContainerToResponsesConversation(source.get("container"));
        if (conversation != null) {
            target.set("conversation", conversation);
        }

        ArrayNode output = json.arrayNode();
        ArrayNode msgParts = json.arrayNode();
        JsonNode content = source.get("content");
        String stopReason = requiredClaudeStopReason(source);
        boolean refusal = "refusal".equals(stopReason);
        int outputOrdinal = 0;

        if (content != null && !content.isNull() && !content.isArray()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_RESPONSE_CONTENT_MUST_BE_ARRAY");
        }
        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                String type = block.path("type").asText("");
                switch (type) {
                    case "thinking", "redacted_thinking" -> {
                        outputOrdinal = flushClaudeResponseMessage(
                                output, msgParts, responseId, outputOrdinal);
                        output.add(claudeResponseThinkingToResponses(block, responseId, outputOrdinal));
                        outputOrdinal++;
                    }
                    case "compaction" -> {
                        outputOrdinal = flushClaudeResponseMessage(
                                output, msgParts, responseId, outputOrdinal);
                        output.add(claudeResponseCompactionToResponses(block, responseId, outputOrdinal));
                        outputOrdinal++;
                    }
                    case "text" -> {
                        ObjectNode textPart = json.objectNode();
                        if (refusal) {
                            textPart.put("type", "refusal");
                            textPart.put("refusal", block.path("text").asText(""));
                        } else {
                            textPart.put("type", "output_text");
                            textPart.put("text", block.path("text").asText(""));
                            textPart.set("annotations", json.arrayNode());
                        }
                        msgParts.add(textPart);
                    }
                    case "tool_use" -> {
                        outputOrdinal = flushClaudeResponseMessage(
                                output, msgParts, responseId, outputOrdinal);
                        ObjectNode functionCall = claudeToolUseToResponses(block);
                        String idPrefix = "custom_tool_call".equals(functionCall.path("type").asText(""))
                                ? "ctc_" : "fc_";
                        functionCall.put("id", responsesOutputItemId(idPrefix, responseId, outputOrdinal));
                        functionCall.put("status", "completed");
                        output.add(functionCall);
                        outputOrdinal++;
                    }
                    case "mcp_tool_use", "server_tool_use", "code_execution_tool_result",
                         "mcp_tool_result", "web_search_tool_result", "web_fetch_tool_result",
                         "bash_code_execution_tool_result", "text_editor_code_execution_tool_result",
                         "tool_search_tool_result" ->
                            throw new ProtocolConversionException(
                                    "CLAUDE_RESPONSES_UNSUPPORTED_RESPONSE_BLOCK: " + type);
                    default -> throw new ProtocolConversionException(
                            "CLAUDE_RESPONSES_UNSUPPORTED_RESPONSE_BLOCK: " + type);
                }
            }
        }

        outputOrdinal = flushClaudeResponseMessage(output, msgParts, responseId, outputOrdinal);
        if (output.isEmpty()) {
            ObjectNode emptyText = json.objectNode();
            emptyText.put("type", refusal ? "refusal" : "output_text");
            emptyText.put(refusal ? "refusal" : "text", "");
            if (!refusal) {
                emptyText.set("annotations", json.arrayNode());
            }
            msgParts.add(emptyText);
            flushClaudeResponseMessage(output, msgParts, responseId, outputOrdinal);
        }

        target.set("output", output);
        target.put("output_text", responsesOutputText(output));
        applyClaudeStopReasonToResponses(target, stopReason);
        target.set("usage", responsesUsageFromClaude(source.path("usage")));
        return target;
    }

    private int flushClaudeResponseMessage(
            ArrayNode output,
            ArrayNode messageParts,
            String responseId,
            int outputOrdinal
    ) {
        if (messageParts.isEmpty()) {
            return outputOrdinal;
        }
        ObjectNode messageItem = json.objectNode();
        messageItem.put("type", "message");
        messageItem.put("id", responsesOutputItemId("msg_", responseId, outputOrdinal));
        messageItem.put("role", "assistant");
        messageItem.set("content", messageParts.deepCopy());
        messageItem.put("status", "completed");
        output.add(messageItem);
        messageParts.removeAll();
        return outputOrdinal + 1;
    }

    private ObjectNode claudeResponseThinkingToResponses(
            JsonNode block,
            String responseId,
            int outputOrdinal
    ) {
        if ("redacted_thinking".equals(block.path("type").asText(""))) {
            ObjectNode reasoning = json.objectNode();
            reasoning.put("type", "reasoning");
            reasoning.put("id", responsesOutputItemId("rs_", responseId, outputOrdinal));
            reasoning.set("summary", json.arrayNode());
            String bridged = ClaudeThinkingStateBridge.encode(json.objectMapper(), block)
                    .orElseThrow(() -> new ProtocolConversionException(
                            "CLAUDE_RESPONSES_REDACTED_THINKING_STATE_MISSING"));
            reasoning.put("encrypted_content", bridged);
            return reasoning;
        }

        String signature = block.path("signature").asText("");
        Optional<JsonNode> opaqueItem = ResponsesReasoningBridge.decodeItem(json.objectMapper(), signature);
        if (opaqueItem.isPresent()) {
            return (ObjectNode) opaqueItem.get().deepCopy();
        }

        ObjectNode reasoning = json.objectNode();
        reasoning.put("type", "reasoning");
        Optional<JsonNode> state = ResponsesReasoningBridge.decode(json.objectMapper(), signature);
        if (state.isPresent()) {
            reasoning.put("id", state.get().path("id").asText());
            reasoning.put("encrypted_content", state.get().path("encrypted_content").asText());
        } else {
            reasoning.put("id", responsesOutputItemId("rs_", responseId, outputOrdinal));
            // Native Claude signatures are tunneled through encrypted_content so a
            // Responses client can replay the signed thinking block on the next turn.
            ClaudeThinkingStateBridge.encode(json.objectMapper(), block)
                    .ifPresent(bridged -> reasoning.put("encrypted_content", bridged));
        }

        String thinking = block.path("thinking").asText("");
        if (!thinking.isBlank()) {
            ArrayNode summary = json.arrayNode();
            ObjectNode summaryText = json.objectNode();
            summaryText.put("type", "summary_text");
            summaryText.put("text", thinking);
            summary.add(summaryText);
            reasoning.set("summary", summary);
        } else if (state.isEmpty() && !reasoning.hasNonNull("encrypted_content")) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_THINKING_STATE_NOT_REPLAYABLE");
        }
        return reasoning;
    }

    private ObjectNode claudeResponseCompactionToResponses(
            JsonNode block,
            String responseId,
            int outputOrdinal
    ) {
        String summaryText = block.path("content").asText("");
        if (summaryText.isBlank()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_COMPACTION_CONTENT_REQUIRED");
        }
        ObjectNode compaction = json.objectNode();
        compaction.put("type", "compaction");
        compaction.put("id", responsesOutputItemId("cmp_", responseId, outputOrdinal));
        ArrayNode summary = json.arrayNode();
        ObjectNode summaryPart = json.objectNode();
        summaryPart.put("type", "summary_text");
        summaryPart.put("text", summaryText);
        summary.add(summaryPart);
        compaction.set("summary", summary);
        compaction.put("status", "completed");
        return compaction;
    }

    private ObjectNode claudeContainerToResponsesConversation(JsonNode container) {
        if (container == null || container.isNull()) {
            return null;
        }
        String id = container.isTextual()
                ? container.asText("")
                : container.path("id").asText("");
        if (id.isBlank()) {
            return null;
        }
        ObjectNode conversation = json.objectNode();
        conversation.put("id", id);
        return conversation;
    }

    private String toResponsesResponseId(String claudeMessageId) {
        if (claudeMessageId == null || claudeMessageId.isBlank()) {
            return "resp_api2api";
        }
        if (claudeMessageId.startsWith("resp_")) {
            return claudeMessageId;
        }
        String suffix = claudeMessageId.startsWith("msg_")
                ? claudeMessageId.substring("msg_".length())
                : claudeMessageId;
        return "resp_" + suffix;
    }

    private String responsesOutputItemId(String prefix, String responseId, int outputOrdinal) {
        String base = responseId.startsWith("resp_")
                ? responseId.substring("resp_".length())
                : responseId;
        String normalized = base.replaceAll("[^A-Za-z0-9_-]", "_");
        return prefix + normalized + "_" + outputOrdinal;
    }

    private String responsesOutputText(ArrayNode output) {
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText(""))) {
                continue;
            }
            for (JsonNode part : item.path("content")) {
                if ("output_text".equals(part.path("type").asText(""))) {
                    text.append(part.path("text").asText(""));
                } else if ("refusal".equals(part.path("type").asText(""))) {
                    text.append(part.path("refusal").asText(""));
                }
            }
        }
        return text.toString();
    }

    private void applyClaudeStopReasonToResponses(ObjectNode target, String stopReason) {
        switch (stopReason) {
            case "end_turn", "tool_use", "stop_sequence", "pause_turn", "refusal" ->
                    target.put("status", "completed");
            case "max_tokens", "model_context_window_exceeded" -> {
                target.put("status", "incomplete");
                ObjectNode incompleteDetails = json.objectNode();
                incompleteDetails.put("reason", "max_tokens".equals(stopReason)
                        ? "max_output_tokens" : "model_context_window_exceeded");
                target.set("incomplete_details", incompleteDetails);
            }
            default -> throw new ProtocolConversionException(
                    "CLAUDE_UNSUPPORTED_STOP_REASON: " + stopReason);
        }
    }

    private String requiredClaudeStopReason(JsonNode source) {
        JsonNode value = source.get("stop_reason");
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ProtocolConversionException("CLAUDE_MISSING_STOP_REASON");
        }
        return value.asText();
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
        String reasoningContent = reasoning != null && reasoning.isTextual()
                ? reasoning.asText("") : "";
        if (!reasoningContent.isEmpty()) {
            ObjectNode thinkingBlock = json.objectNode();
            thinkingBlock.put("type", "thinking");
            thinkingBlock.put("thinking", reasoningContent);
            content.add(thinkingBlock);
        }

        JsonNode toolCalls = msg.get("tool_calls");
        boolean hasToolCalls = toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty();
        String textContent = chatResponseContentText(msg.get("content"));
        if (textContent.isBlank() && msg.path("refusal").isTextual()) {
            textContent = msg.path("refusal").asText("");
        }
        if (textContent.isBlank() && !reasoningContent.isBlank() && !hasToolCalls) {
            textContent = reasoningContent;
        }
        if (!textContent.isEmpty()) {
            ObjectNode textBlock = json.objectNode();
            textBlock.put("type", "text");
            textBlock.put("text", textContent);
            content.add(textBlock);
        }

        if (hasToolCalls) {
            for (JsonNode call : toolCalls) {
                ObjectNode toolUseBlock = json.objectNode();
                toolUseBlock.put("type", "tool_use");
                toolUseBlock.put("id", call.path("id").asText(""));
                String toolName = call.path("function").path("name").asText("");
                toolUseBlock.put("name", toolName);
                String args = call.path("function").path("arguments").asText("{}");
                try {
                    String normalizedArgs = ResponsesToolCallBridge.toClaudeToolInputJson(
                            json.objectMapper(), toolName, args, false);
                    toolUseBlock.set("input", json.objectMapper().readTree(normalizedArgs));
                } catch (ProtocolConversionException exception) {
                    throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_INVALID_TOOL_ARGUMENTS", exception);
                } catch (JsonProcessingException exception) {
                    throw new ProtocolConversionException("OPENAI_CHAT_CLAUDE_TOOL_ARGUMENTS_ENCODING_FAILED", exception);
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
        target.put("stop_reason", chatFinishReasonToClaude(
                choice.path("finish_reason").asText("stop"), hasToolCalls));
        target.set("usage", claudeUsageFromChat(source.path("usage")));
        return target;
    }

    private String chatResponseContentText(JsonNode content) {
        if (content == null || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText("");
        }
        if (!content.isArray()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            if (!("text".equals(type) || "output_text".equals(type))) {
                continue;
            }
            String value = part.path("text").asText("");
            if (value.isEmpty()) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(value);
        }
        return text.toString();
    }

    private String chatFinishReasonToClaude(String finishReason, boolean hasToolCalls) {
        return switch (finishReason) {
            case "length" -> "max_tokens";
            case "tool_calls", "function_call" -> "tool_use";
            case "content_filter" -> "refusal";
            case "stop" -> hasToolCalls ? "tool_use" : "end_turn";
            default -> hasToolCalls ? "tool_use" : "end_turn";
        };
    }

    private ObjectNode chatResponseToResponses(JsonNode source) {
        ObjectNode target = json.objectNode();
        JsonNode choice = source.path("choices").path(0);
        String responseId = source.path("id").asText("resp_api2api");
        target.put("id", responseId);
        target.put("object", "response");
        target.put("created_at", source.path("created").asLong(Instant.now().getEpochSecond()));
        target.put("model", source.path("model").asText(""));
        JsonNode message = choice.path("message");
        ArrayNode output = json.arrayNode();
        int outputOrdinal = 0;

        String reasoning = message.path("reasoning_content").asText("");
        if (!reasoning.isBlank()) {
            ObjectNode reasoningItem = json.objectNode();
            reasoningItem.put("type", "reasoning");
            reasoningItem.put("id", responsesOutputItemId("rs_", responseId, outputOrdinal++));
            ArrayNode summary = json.arrayNode();
            summary.add(json.objectNode().put("type", "summary_text").put("text", reasoning));
            reasoningItem.set("summary", summary);
            output.add(reasoningItem);
        }

        String text = chatResponseContentText(message.get("content"));
        String refusal = message.path("refusal").asText("");
        JsonNode toolCalls = message.get("tool_calls");
        boolean hasToolCalls = toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty();
        if (text.isBlank() && !reasoning.isBlank() && !hasToolCalls) {
            text = reasoning;
        }
        if (!text.isEmpty() || !refusal.isEmpty() || !hasToolCalls) {
            ObjectNode messageItem = outputMessage(text);
            messageItem.put("id", responsesOutputItemId("msg_", responseId, outputOrdinal++));
            messageItem.put("status", "completed");
            ObjectNode textPart = (ObjectNode) messageItem.path("content").path(0);
            if (message.hasNonNull("annotations") && message.path("annotations").isArray()) {
                textPart.set("annotations", message.path("annotations").deepCopy());
            } else {
                textPart.set("annotations", json.arrayNode());
            }
            if (!refusal.isEmpty()) {
                ArrayNode content = (ArrayNode) messageItem.path("content");
                if (text.isEmpty()) {
                    content.removeAll();
                }
                content.add(json.objectNode().put("type", "refusal").put("refusal", refusal));
            }
            JsonNode logprobs = choice.path("logprobs").path("content");
            if (logprobs.isArray() && !logprobs.isEmpty() && !text.isEmpty()) {
                textPart.set("logprobs", logprobs.deepCopy());
            }
            output.add(messageItem);
        }
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode call : toolCalls) {
                ObjectNode functionCall = json.objectNode();
                functionCall.put("type", "function_call");
                functionCall.put("id", responsesOutputItemId("fc_", responseId, outputOrdinal++));
                functionCall.put("call_id", call.path("id").asText(""));
                functionCall.put("name", call.path("function").path("name").asText(""));
                String arguments = call.path("function").path("arguments").asText("");
                functionCall.put("arguments", arguments.isBlank() ? "{}" : arguments);
                functionCall.put("status", "completed");
                output.add(functionCall);
            }
        }
        target.set("output", output);
        target.put("output_text", responsesOutputText(output));
        if ("length".equals(choice.path("finish_reason").asText(""))) {
            target.put("status", "incomplete");
            target.set("incomplete_details", json.objectNode().put("reason", "max_output_tokens"));
        } else if ("content_filter".equals(choice.path("finish_reason").asText(""))) {
            target.put("status", "incomplete");
            target.set("incomplete_details", json.objectNode().put("reason", "content_filter"));
        } else {
            target.put("status", "completed");
        }
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
        StringBuilder refusal = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        ArrayNode toolCalls = json.arrayNode();
        ArrayNode annotations = json.arrayNode();
        JsonNode responseLogprobs = null;
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
                                if (part.path("annotations").isArray()) {
                                    annotations.addAll((ArrayNode) part.path("annotations"));
                                }
                                if (part.path("logprobs").isArray() && !part.path("logprobs").isEmpty()) {
                                    responseLogprobs = part.path("logprobs").deepCopy();
                                }
                            } else if ("refusal".equals(part.path("type").asText(""))) {
                                refusal.append(part.path("refusal").asText(""));
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
                } else if ("file_search_call".equals(type) || "code_interpreter_call".equals(type)) {
                    String toolOutput = responsesToolOutputText(item);
                    if (!toolOutput.isBlank()) {
                        if (!text.isEmpty()) {
                            text.append("\n\n");
                        }
                        text.append(toolOutput);
                    }
                }
            }
        }
        if (!text.isEmpty()) {
            message.put("content", text.toString());
        } else if (!refusal.isEmpty()) {
            message.put("content", refusal.toString());
        } else {
            message.putNull("content");
        }
        if (!refusal.isEmpty()) {
            message.put("refusal", refusal.toString());
        }
        if (!annotations.isEmpty()) {
            message.set("annotations", annotations);
        }
        if (!reasoning.isEmpty()) {
            message.put("reasoning_content", reasoning.toString());
        }
        if (!toolCalls.isEmpty()) {
            message.set("tool_calls", toolCalls);
        }
        choice.set("message", message);
        choice.put("finish_reason", responsesFinishReasonToChat(source, toolCalls));
        if (responseLogprobs != null) {
            choice.set("logprobs", json.objectNode().set("content", responseLogprobs));
        }
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
            if ("content_filter".equals(source.path("incomplete_details").path("reason").asText(""))) {
                return "content_filter";
            }
            return "length";
        }
        return "stop";
    }

    private String responsesToolOutputText(JsonNode item) {
        for (String field : List.of("result", "logs", "output", "results")) {
            JsonNode value = item.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual()) {
                return value.asText("");
            }
            return value.toString();
        }
        return "";
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
        return ResponsesProtocolConstants.isCompactionType(type);
    }

    private ObjectNode responsesReasoningToClaude(JsonNode item) {
        JsonNode bridgedBlock = ClaudeThinkingStateBridge.decode(
                json.objectMapper(), item.path("encrypted_content").asText("")).orElse(null);
        if (bridgedBlock != null) {
            // Round trip: restore the native Claude thinking/redacted_thinking block
            // that was tunneled through reasoning.encrypted_content.
            return (ObjectNode) bridgedBlock;
        }
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

    private ObjectNode outputMessage(String value) {
        ObjectNode message = json.objectNode();
        message.put("type", "message");
        message.put("role", "assistant");
        ArrayNode content = json.arrayNode();
        ObjectNode text = json.objectNode();
        text.put("type", "output_text");
        text.put("text", value == null ? "" : value);
        text.set("annotations", json.arrayNode());
        content.add(text);
        message.set("content", content);
        return message;
    }

    private record RawTokenUsage(long input, long output, long cacheRead, long cacheWrite) {
        static RawTokenUsage fromClaude(JsonNode usage) {
            long cacheCreation = usage.path("cache_creation_input_tokens").asLong(0);
            long cacheRead = usage.path("cache_read_input_tokens").asLong(0);
            long input = usage.path("input_tokens").asLong(0) + cacheCreation + cacheRead;
            long output = usage.path("output_tokens").asLong(0);
            return new RawTokenUsage(input, output, cacheRead, cacheCreation);
        }

        static RawTokenUsage fromChat(JsonNode usage) {
            JsonNode details = usage.path("prompt_tokens_details");
            long cached = details.path("cached_tokens").asLong(0);
            long cacheWrite = OpenAIChatCompletionsUsageExtractor.cacheWriteTokens(details);
            long input = usage.path("prompt_tokens").asLong(0);
            long output = usage.path("completion_tokens").asLong(0);
            return new RawTokenUsage(input, output, cached, cacheWrite);
        }

        static RawTokenUsage fromResponses(JsonNode usage) {
            long cached = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
            long cacheWrite = usage.path("input_tokens_details").path("cache_write_tokens").asLong(0);
            long input = usage.path("input_tokens").asLong(0);
            long output = usage.path("output_tokens").asLong(0);
            return new RawTokenUsage(input, output, cached, cacheWrite);
        }
    }

    private ObjectNode toChatUsage(RawTokenUsage raw, boolean includeCacheWrite) {
        ObjectNode target = json.objectNode();
        target.put("prompt_tokens", raw.input());
        target.put("completion_tokens", raw.output());
        target.put("total_tokens", raw.input() + raw.output());
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", raw.cacheRead());
        if (includeCacheWrite) {
            details.put("cache_write_tokens", raw.cacheWrite());
        }
        target.set("prompt_tokens_details", details);
        return target;
    }

    private ObjectNode toResponsesUsage(RawTokenUsage raw) {
        ObjectNode target = json.objectNode();
        target.put("input_tokens", raw.input());
        target.put("output_tokens", raw.output());
        target.put("total_tokens", raw.input() + raw.output());
        ObjectNode details = json.objectNode();
        details.put("cached_tokens", raw.cacheRead());
        if (raw.cacheWrite() > 0) {
            details.put("cache_write_tokens", raw.cacheWrite());
        }
        target.set("input_tokens_details", details);
        return target;
    }

    private ObjectNode toClaudeUsage(RawTokenUsage raw) {
        ObjectNode target = json.objectNode();
        target.put("input_tokens", Math.max(0, raw.input() - raw.cacheRead() - raw.cacheWrite()));
        target.put("output_tokens", raw.output());
        target.put("cache_creation_input_tokens", raw.cacheWrite());
        target.put("cache_read_input_tokens", raw.cacheRead());
        return target;
    }

    private ObjectNode chatUsageFromClaude(JsonNode usage) {
        return toChatUsage(RawTokenUsage.fromClaude(usage), true);
    }

    private ObjectNode responsesUsageFromClaude(JsonNode usage) {
        return toResponsesUsage(RawTokenUsage.fromClaude(usage));
    }

    private ObjectNode claudeUsageFromChat(JsonNode usage) {
        return toClaudeUsage(RawTokenUsage.fromChat(usage));
    }

    private ObjectNode responsesUsageFromChat(JsonNode usage) {
        return toResponsesUsage(RawTokenUsage.fromChat(usage));
    }

    private ObjectNode chatUsageFromResponses(JsonNode usage) {
        return toChatUsage(RawTokenUsage.fromResponses(usage), false);
    }

    private ObjectNode claudeUsageFromResponses(JsonNode usage) {
        return toClaudeUsage(RawTokenUsage.fromResponses(usage));
    }
}
