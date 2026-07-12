package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProtocolConverterConfiguration {

    private static final Set<String> CLAUDE_RESPONSES_REQUEST_FIELDS = Set.of(
            "model", "messages", "max_tokens", "system", "stream", "temperature", "top_p", "top_k",
            "stop_sequences", "metadata", "service_tier", "speed", "thinking", "reasoning", "tool_choice",
            "tools", "cache_control", "output_config", "context_management", "container", "mcp_servers",
            "inference_geo", "diagnostics"
    );

    private static final String RESPONSES_OPAQUE_STATE_PLACEHOLDER = "Thinking...";
    private static final String RESPONSES_COMPACTION_PLACEHOLDER = "Context compacted.";

    @Bean
    ProtocolMessageConverter claudeMessagesToOpenAIResponsesRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return converter(json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter claudeMessagesToOpenAIResponsesResponse(ProtocolJsonSupport json, ClaudeMessagesUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return converter(json, usageExtractor, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter claudeMessagesToOpenAIChatRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return converter(json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter claudeMessagesToOpenAIChatResponse(ProtocolJsonSupport json, ClaudeMessagesUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return converter(json, usageExtractor, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIResponsesToClaudeMessagesRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return converter(json, null, ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIResponsesToClaudeMessagesResponse(ProtocolJsonSupport json, OpenAIResponsesUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return converter(json, usageExtractor, ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIResponsesToOpenAIChatRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return converter(json, null, ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIResponsesToOpenAIChatResponse(ProtocolJsonSupport json, OpenAIResponsesUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return converter(json, usageExtractor, ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIChatToClaudeMessagesRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return converter(json, null, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIChatToClaudeMessagesResponse(ProtocolJsonSupport json, OpenAIChatCompletionsUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return converter(json, usageExtractor, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIChatToOpenAIResponsesRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return converter(json, null, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIChatToOpenAIResponsesResponse(ProtocolJsonSupport json, OpenAIChatCompletionsUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return converter(json, usageExtractor, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    // ==================== Bedrock Converse Converters ====================

    @Bean
    ProtocolMessageConverter claudeMessagesToBedrockConverseRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return bedrockConverter(json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter bedrockConverseToClaudeMessagesResponse(ProtocolJsonSupport json, BedrockConverseUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return bedrockConverter(json, usageExtractor, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIChatToBedrockConverseRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return bedrockConverter(json, null, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter bedrockConverseToOpenAIChatResponse(ProtocolJsonSupport json, BedrockConverseUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return bedrockConverter(json, usageExtractor, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter openAIResponsesToBedrockConverseRequest(ProtocolJsonSupport json, SseEventTransformer sseEventTransformer) {
        return bedrockConverter(json, null, ProtocolType.OPENAI_RESPONSES, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolConversionDirection.REQUEST, sseEventTransformer);
    }

    @Bean
    ProtocolMessageConverter bedrockConverseToOpenAIResponsesResponse(ProtocolJsonSupport json, BedrockConverseUsageExtractor usageExtractor, SseEventTransformer sseEventTransformer) {
        return bedrockConverter(json, usageExtractor, ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_RESPONSES, ProtocolConversionDirection.RESPONSE, sseEventTransformer);
    }

    private ProtocolMessageConverter bedrockConverter(
            ProtocolJsonSupport json,
            UnifiedUsageExtractor usageExtractor,
            ProtocolType source,
            ProtocolType target,
            ProtocolConversionDirection direction,
            SseEventTransformer sseEventTransformer
    ) {
        return new BedrockConverseProtocolMessageConverter(json, usageExtractor, source, target, direction, sseEventTransformer);
    }

    private ProtocolMessageConverter converter(
            ProtocolJsonSupport json,
            UnifiedUsageExtractor usageExtractor,
            ProtocolType source,
            ProtocolType target,
            ProtocolConversionDirection direction,
            SseEventTransformer sseEventTransformer
    ) {
        return new GenericProtocolMessageConverter(json, usageExtractor, source, target, direction, sseEventTransformer);
    }

    private static final class GenericProtocolMessageConverter extends AbstractProtocolMessageConverter {

        private GenericProtocolMessageConverter(
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
            if (direction() == ProtocolConversionDirection.RESPONSE
                    && sourceProtocol() == ProtocolType.OPENAI_RESPONSES
                    && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
                return super.supports(requirement);
            }
            return !requirement.streaming() && !requirement.toolCallingRequired() && super.supports(requirement);
        }

        @Override
        protected JsonNode convertRequestJson(JsonNode source) {
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
        protected JsonNode convertResponseJson(JsonNode source) {
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
            copyIfPresent(source, target, "model");
            copyIfPresent(source, target, "max_tokens");
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
            copyIfPresent(source, target, "stream");
            ArrayNode messages = json.arrayNode();
            JsonNode system = source.get("system");
            if (system != null && !system.isNull()) {
                ObjectNode systemMessage = json.objectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", system.isTextual() ? system.asText() : extractOpenAiContentText(system));
                messages.add(systemMessage);
            }
            messages.addAll(claudeMessagesToOpenAiInput(source.get("messages")));
            target.set("messages", messages);
            return target;
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
                target.put("service_tier", "standard_only".equals(source.path("service_tier").asText()) ? "default" : "auto");
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
            if (outputConfig != null && outputConfig.isObject()) {
                JsonNode format = outputConfig.get("format");
                if (format != null && !format.isNull()) {
                    text.set("format", ensureResponseTextFormat(format));
                }
            }
            if (!text.isEmpty()) {
                target.set("text", text);
            }
            ArrayNode input = json.arrayNode();
            input.addAll(claudeSystemToResponsesInput(source.get("system"), model));
            input.addAll(claudeMessagesToResponsesInput(source.get("messages"), model));
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
            if (supportsExplicitCacheBreakpoints(model) && containsResponsesCacheBreakpoint(input)) {
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
                String assistantPhase = "assistant".equals(role)
                        ? (containsClaudeToolUse(content) ? "commentary" : "final_answer")
                        : null;
                for (JsonNode block : content) {
                    switch (block.path("type").asText("")) {
                        case "text" -> addClaudeTextPart(messageContent, block.path("text").asText(""),
                                "assistant".equals(role) ? "output_text" : "input_text", block, model);
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
                            ObjectNode mappedThinking = claudeThinkingToResponses(block);
                            if ("compaction".equals(mappedThinking.path("type").asText(""))) {
                                input.removeAll();
                            }
                            input.add(mappedThinking);
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
            if (!supportsExplicitCacheBreakpoints(model) || !isResponsesCacheablePart(target)) {
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
            if (!supportsExplicitCacheBreakpoints(model)) {
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

        private ObjectNode claudeThinkingToResponses(JsonNode block) {
            String signature = block.path("signature").asText("");
            Optional<JsonNode> hostedItem = ResponsesReasoningBridge.decodeItem(json.objectMapper(), signature);
            if (hostedItem.isPresent()) {
                return (ObjectNode) hostedItem.get();
            }
            JsonNode state = ResponsesReasoningBridge.decode(json.objectMapper(), signature)
                    .orElseThrow(() -> new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_THINKING_SIGNATURE"));
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
            return reasoning;
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
            if (!custom) {
                result.put("status", block.path("is_error").asBoolean(false) ? "incomplete" : "completed");
            }
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
                    if (allowedCallers.values() != null) {
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
            if (!format.isObject() || !"json_schema".equals(format.path("type").asText("")) || format.hasNonNull("name")) {
                return format;
            }
            ObjectNode copy = (ObjectNode) format.deepCopy();
            copy.put("name", "json_response");
            return copy;
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

        private boolean supportsExplicitCacheBreakpoints(String model) {
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
            copyIfPresent(source, target, "max_tokens");
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
            ArrayNode messages = json.arrayNode();
            StringBuilder system = new StringBuilder();
            JsonNode chatMessages = source.get("messages");
            if (chatMessages != null && chatMessages.isArray()) {
                for (JsonNode message : chatMessages) {
                    String role = message.path("role").asText("user");
                    String content = message.path("content").asText("");
                    if ("system".equals(role)) {
                        if (!system.isEmpty()) {
                            system.append('\n');
                        }
                        system.append(content);
                        continue;
                    }
                    ObjectNode mapped = json.objectNode();
                    mapped.put("role", "assistant".equals(role) ? "assistant" : "user");
                    ArrayNode contentItems = json.arrayNode();
                    ObjectNode text = json.objectNode();
                    text.put("type", "text");
                    text.put("text", content);
                    contentItems.add(text);
                    mapped.set("content", contentItems);
                    messages.add(mapped);
                }
            }
            if (!system.isEmpty()) {
                target.put("system", system.toString());
            }
            target.set("messages", messages);
            return target;
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
            message.put("content", firstTextFromClaudeContent(source.get("content")));
            choice.set("message", message);
            choice.put("finish_reason", mapStopToFinishReason(source.path("stop_reason").asText("end_turn")));
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
            target.set("output", outputMessage(firstTextFromClaudeContent(source.get("content"))));
            target.set("usage", responsesUsageFromClaude(source.path("usage")));
            return target;
        }

        private ObjectNode chatResponseToClaude(JsonNode source) {
            ObjectNode target = json.objectNode();
            JsonNode choice = source.path("choices").path(0);
            target.put("id", source.path("id").asText("msg_api2api"));
            target.put("type", "message");
            target.put("role", "assistant");
            target.put("model", source.path("model").asText(""));
            ArrayNode content = json.arrayNode();
            ObjectNode text = json.objectNode();
            text.put("type", "text");
            text.put("text", choice.path("message").path("content").asText(""));
            content.add(text);
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
            target.set("output", outputMessage(choice.path("message").path("content").asText("")));
            target.set("usage", responsesUsageFromChat(source.path("usage")));
            return target;
        }

        private ObjectNode responsesResponseToChat(JsonNode source) {
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
            message.put("content", firstOutputText(source.get("output")));
            choice.set("message", message);
            choice.put("finish_reason", "stop");
            choices.add(choice);
            target.set("choices", choices);
            target.set("usage", chatUsageFromResponses(source.path("usage")));
            return target;
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
                if ("compaction".equals(type)) {
                    content.add(responsesOpaqueItemToClaude(item, RESPONSES_COMPACTION_PLACEHOLDER));
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
            long cached = usage.path("prompt_tokens_details").path("cached_tokens").asLong(0);
            target.put("input_tokens", Math.max(0, usage.path("prompt_tokens").asLong(0) - cached));
            target.put("output_tokens", usage.path("completion_tokens").asLong(0));
            target.put("cache_creation_input_tokens", 0);
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
}
