package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProtocolConverterConfiguration {

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
            ObjectNode target = json.objectNode();
            copyIfPresent(source, target, "model");
            copyIfPresent(source, target, "stream");
            copyIfPresent(source, target, "max_tokens", "max_output_tokens");
            copyIfPresent(source, target, "temperature");
            copyIfPresent(source, target, "top_p");
            JsonNode system = source.get("system");
            if (system != null && !system.isNull()) {
                target.put("instructions", system.isTextual() ? system.asText() : extractOpenAiContentText(system));
            }
            target.set("input", claudeMessagesToResponsesInput(source.get("messages")));
            JsonNode tools = source.get("tools");
            if (tools != null && tools.isArray() && !tools.isEmpty()) {
                ArrayNode mappedTools = json.arrayNode();
                for (JsonNode tool : tools) {
                    ObjectNode mapped = json.objectNode();
                    mapped.put("type", "function");
                    mapped.put("name", tool.path("name").asText(""));
                    copyIfPresent(tool, mapped, "description");
                    JsonNode schema = tool.get("input_schema");
                    mapped.set("parameters", schema == null || schema.isNull() ? json.objectNode() : schema);
                    mappedTools.add(mapped);
                }
                target.set("tools", mappedTools);
            }
            JsonNode toolChoice = source.get("tool_choice");
            if (toolChoice != null && !toolChoice.isNull()) {
                target.set("tool_choice", claudeToolChoiceToResponses(toolChoice));
            }
            JsonNode thinking = source.get("thinking");
            if (thinking != null && thinking.isObject() && "enabled".equals(thinking.path("type").asText())) {
                ObjectNode reasoning = json.objectNode();
                reasoning.put("effort", reasoningEffort(thinking.path("budget_tokens").asInt(0)));
                target.set("reasoning", reasoning);
            }
            return target;
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
                    mapped.put("role", role);
                    mapped.put("content", content == null ? "" : content.asText(""));
                    input.add(mapped);
                    continue;
                }
                if (!content.isArray()) {
                    continue;
                }
                ArrayNode textContent = json.arrayNode();
                for (JsonNode block : content) {
                    switch (block.path("type").asText("")) {
                        case "text" -> {
                            ObjectNode text = json.objectNode();
                            text.put("type", "input_text");
                            text.put("text", block.path("text").asText(""));
                            textContent.add(text);
                        }
                        case "tool_use" -> {
                            ObjectNode call = json.objectNode();
                            call.put("type", "function_call");
                            call.put("call_id", block.path("id").asText(""));
                            call.put("name", block.path("name").asText(""));
                            JsonNode arguments = block.get("input");
                            call.put("arguments", arguments == null || arguments.isNull() ? "{}" : arguments.toString());
                            input.add(call);
                        }
                        case "tool_result" -> {
                            ObjectNode result = json.objectNode();
                            result.put("type", "function_call_output");
                            result.put("call_id", block.path("tool_use_id").asText(""));
                            result.put("output", extractOpenAiContentText(block.get("content")));
                            input.add(result);
                        }
                        default -> {
                        }
                    }
                }
                if (!textContent.isEmpty()) {
                    ObjectNode mapped = json.objectNode();
                    mapped.put("role", role);
                    mapped.set("content", textContent);
                    input.add(mapped);
                }
            }
            return input;
        }

        private JsonNode claudeToolChoiceToResponses(JsonNode toolChoice) {
            String type = toolChoice.isTextual() ? toolChoice.asText("auto") : toolChoice.path("type").asText("auto");
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

        private String reasoningEffort(int budgetTokens) {
            if (budgetTokens >= 4096) {
                return "high";
            }
            if (budgetTokens > 0 && budgetTokens <= 1024) {
                return "low";
            }
            return "medium";
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
                }
            }
            return content;
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
