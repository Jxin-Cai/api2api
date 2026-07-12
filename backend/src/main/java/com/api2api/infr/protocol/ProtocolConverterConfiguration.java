package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProtocolConverterConfiguration {

    private static final Set<String> CLAUDE_RESPONSES_REQUEST_FIELDS = Set.of(
            "model", "messages", "max_tokens", "system", "stream", "temperature", "top_p", "top_k",
            "stop_sequences", "metadata", "service_tier", "speed", "thinking", "reasoning", "tool_choice",
            "tools", "cache_control", "output_config", "context_management", "container", "mcp_servers"
    );

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
            ObjectNode target = json.objectNode();
            copyIfPresent(source, target, "model");
            copyIfPresent(source, target, "stream");
            copyIfPresent(source, target, "max_tokens", "max_output_tokens");
            if (!isReasoningModel(source.path("model").asText(""))) {
                copyIfPresent(source, target, "temperature");
                copyIfPresent(source, target, "top_p");
            }
            copyIfPresent(source, target, "metadata");
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
            input.addAll(claudeSystemToResponsesInput(source.get("system")));
            input.addAll(claudeMessagesToResponsesInput(source.get("messages")));
            target.set("input", input);
            ArrayNode mappedTools = claudeToolsToResponses(
                    source.get("tools"),
                    source.get("mcp_servers"),
                    source.get("container"),
                    source.path("model").asText("")
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
                if (!isReasoningModel(source.path("model").asText(""))) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_TARGET_MODEL_DOES_NOT_SUPPORT_REASONING");
                }
                target.set("reasoning", reasoning);
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
        }

        private ArrayNode claudeSystemToResponsesInput(JsonNode system) {
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
                        addClaudeTextPart(content, block.path("text").asText(""), "input_text");
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

        private ArrayNode claudeMessagesToResponsesInput(JsonNode messages) {
            ArrayNode input = json.arrayNode();
            if (messages == null || !messages.isArray()) {
                return input;
            }
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
                        case "text" -> addClaudeTextPart(messageContent, block.path("text").asText(""), "assistant".equals(role) ? "output_text" : "input_text");
                        case "image" -> addClaudeImagePart(messageContent, block);
                        case "document" -> addClaudeDocumentPart(messageContent, block);
                        case "search_result" -> addClaudeSearchResultPart(messageContent, block, "assistant".equals(role) ? "output_text" : "input_text");
                        case "tool_use" -> {
                            flushResponsesMessage(input, role, messageContent, assistantPhase);
                            input.add(claudeToolUseToResponses(block));
                        }
                        case "tool_result" -> {
                            flushResponsesMessage(input, role, messageContent, assistantPhase);
                            input.add(claudeToolResultToResponses(block));
                        }
                        case "mcp_tool_use", "mcp_tool_result", "server_tool_use", "web_search_tool_result",
                             "web_fetch_tool_result", "code_execution_tool_result", "bash_code_execution_tool_result",
                             "text_editor_code_execution_tool_result", "tool_search_tool_result" ->
                                throw new ProtocolConversionException("CLAUDE_RESPONSES_SERVER_TOOL_HISTORY_NOT_LOSSLESS: "
                                        + block.path("type").asText(""));
                        case "thinking" -> {
                            flushResponsesMessage(input, role, messageContent, assistantPhase);
                            input.add(claudeThinkingToResponses(block));
                        }
                        case "compaction" -> {
                            flushResponsesMessage(input, role, messageContent, assistantPhase);
                            input.add(claudeCompactionToResponses(block));
                        }
                        case "mid_conv_system" -> {
                            flushResponsesMessage(input, role, messageContent, assistantPhase);
                            input.addAll(claudeSystemToResponsesInput(block.get("content")));
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

        private void addClaudeImagePart(ArrayNode content, JsonNode block) {
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
            content.add(image);
        }

        private void addClaudeDocumentPart(ArrayNode content, JsonNode block) {
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
            content.add(file);
        }

        private void addClaudeSearchResultPart(ArrayNode content, JsonNode block, String textType) {
            String text = "Source: " + block.path("source").asText("") + "\nTitle: "
                    + block.path("title").asText("") + "\n" + extractOpenAiContentText(block.get("content"));
            addClaudeTextPart(content, text, textType);
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
            String encryptedContent = block.path("encrypted_content").asText("");
            if (encryptedContent.isBlank()) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_COMPACTION_ENCRYPTED_CONTENT_REQUIRED");
            }
            ObjectNode compaction = json.objectNode();
            compaction.put("type", "compaction");
            compaction.put("encrypted_content", encryptedContent);
            return compaction;
        }

        private ObjectNode claudeToolUseToResponses(JsonNode block) {
            ObjectNode call = json.objectNode();
            call.put("type", "function_call");
            call.put("call_id", block.path("id").asText(""));
            call.put("name", block.path("name").asText(""));
            JsonNode arguments = block.get("input");
            call.put("arguments", arguments == null || arguments.isNull() ? "{}" : arguments.toString());
            return call;
        }

        private ObjectNode claudeToolResultToResponses(JsonNode block) {
            ObjectNode result = json.objectNode();
            result.put("type", "function_call_output");
            result.put("call_id", block.path("tool_use_id").asText(""));
            result.set("output", claudeToolResultOutputToResponses(block.get("content")));
            result.put("status", block.path("is_error").asBoolean(false) ? "incomplete" : "completed");
            return result;
        }

        private JsonNode claudeToolResultOutputToResponses(JsonNode content) {
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
                        output.add(text);
                    }
                    case "image" -> addClaudeImagePart(output, block);
                    case "document" -> addClaudeDocumentPart(output, block);
                    default -> throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_TOOL_RESULT_CONTENT: " + type);
                }
            }
            return output.isEmpty() ? json.valueToTree("") : output;
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
                    if (!hasOnlyDirectAllowedCallers(tool.get("allowed_callers"))) {
                        throw new ProtocolConversionException("CLAUDE_RESPONSES_ADVANCED_TOOL_FIELDS_NOT_SUPPORTED: "
                                + tool.path("name").asText("unnamed"));
                    }
                    ObjectNode mapped = json.objectNode();
                    mapped.put("type", "function");
                    mapped.put("name", tool.path("name").asText(""));
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
            return mappedTools;
        }

        private boolean hasOnlyDirectAllowedCallers(JsonNode allowedCallers) {
            if (allowedCallers == null || allowedCallers.isNull() || !allowedCallers.isArray()
                    || allowedCallers.isEmpty()) {
                return true;
            }
            for (JsonNode caller : allowedCallers) {
                if (!"direct".equals(caller.asText(""))) {
                    return false;
                }
            }
            return true;
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
            ObjectNode target = json.objectNode();
            target.put("id", source.path("id").asText("msg_api2api"));
            target.put("type", "message");
            target.put("role", "assistant");
            target.put("model", source.path("model").asText(""));
            ArrayNode content = responsesOutputToClaudeContent(source.get("output"));
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
                if ("function_call".equals(type)) {
                    ObjectNode toolUse = json.objectNode();
                    toolUse.put("type", "tool_use");
                    toolUse.put("id", item.path("call_id").asText(item.path("id").asText("")));
                    toolUse.put("name", item.path("name").asText(""));
                    String arguments = item.path("arguments").asText("{}");
                    toolUse.set("input", json.parse(arguments.isBlank() ? "{}" : arguments, "OpenAI Responses function arguments"));
                    content.add(toolUse);
                    continue;
                }
                if ("reasoning".equals(type)) {
                    content.add(responsesReasoningToClaude(item));
                    continue;
                }
                if ("compaction".equals(type)) {
                    ObjectNode compaction = json.objectNode();
                    compaction.put("type", "compaction");
                    compaction.putNull("content");
                    compaction.put("encrypted_content", item.path("encrypted_content").asText(""));
                    content.add(compaction);
                    continue;
                }
                content.add(responsesOpaqueItemToClaude(item));
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
            thinking.put("thinking", summaryText.toString());
            String signature = ResponsesReasoningBridge.encode(json.objectMapper(), item)
                    .orElseThrow(() -> new ProtocolConversionException("RESPONSES_CLAUDE_REASONING_STATE_MISSING"));
            thinking.put("signature", signature);
            return thinking;
        }

        private ObjectNode responsesOpaqueItemToClaude(JsonNode item) {
            ObjectNode thinking = json.objectNode();
            thinking.put("type", "thinking");
            thinking.put("thinking", "");
            String signature = ResponsesReasoningBridge.encodeItem(json.objectMapper(), item)
                    .orElseThrow(() -> new ProtocolConversionException("RESPONSES_CLAUDE_OUTPUT_ITEM_STATE_MISSING"));
            thinking.put("signature", signature);
            return thinking;
        }

        private String responsesStopReason(JsonNode source) {
            JsonNode output = source.get("output");
            if (output != null && output.isArray()) {
                for (JsonNode item : output) {
                    if ("function_call".equals(item.path("type").asText())) {
                        return "tool_use";
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
            return "end_turn";
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
            target.put("input_tokens", Math.max(0, usage.path("input_tokens").asLong(0) - cached));
            target.put("output_tokens", usage.path("output_tokens").asLong(0));
            target.put("cache_creation_input_tokens", 0);
            target.put("cache_read_input_tokens", cached);
            return target;
        }
    }
}
