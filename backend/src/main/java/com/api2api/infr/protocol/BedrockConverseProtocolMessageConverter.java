package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionRouteContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class BedrockConverseProtocolMessageConverter extends AbstractProtocolMessageConverter {

    private static final Set<String> CLAUDE_REQUEST_FIELDS = Set.of(
            "model", "messages", "max_tokens", "system", "stream", "temperature", "top_p", "top_k",
            "stop_sequences", "metadata", "service_tier", "speed", "thinking", "reasoning", "tool_choice",
            "tools", "cache_control", "output_config", "output_format", "context_management",
            "additionalModelRequestFields"
    );
    private static final Set<String> BEDROCK_CLAUDE_ADDITIONAL_REQUEST_FIELDS = Set.of(
            "thinking", "output_config", "top_k"
    );
    private static final Pattern BEDROCK_REQUEST_METADATA_VALUE_PATTERN = Pattern.compile("[a-zA-Z0-9\\s:_@$#=/+,.\\-]{0,256}");
    private static final String ASK_USER_QUESTION_TOOL = "AskUserQuestion";
    private static final String ENTER_PLAN_MODE_TOOL = "EnterPlanMode";
    private static final String EXIT_PLAN_MODE_TOOL = "ExitPlanMode";
    private static final String WORKFLOW_TOOL = "Workflow";
    private static final String WORKFLOW_PROMPT_PREFIX = "Run the \"";
    private static final String WORKFLOW_INVOKE_DIRECTIVE = "Invoke: Workflow(";
    private static final String SEND_MESSAGE_TOOL = "SendMessage";
    private static final String AGENT_TOOL = "Agent";
    private static final String LEGACY_AGENT_TOOL = "Task";
    private static final String TASK_NOTIFICATION_TAG = "<task-notification>";
    private static final String TASK_COMPLETED_TAG = "<status>completed</status>";
    private static final String TASK_ID_TAG = "<task-id>";
    private static final String WORKFLOW_INVOCATION_INSTRUCTION =
            "The latest user message is a Claude Code workflow dispatch instruction. "
                    + "The workflow has not started merely because the Skill tool returned 'Launching skill'. "
                    + "Invoke the Workflow tool exactly as requested before claiming that it is running.";
    private static final String BACKGROUND_TASK_COMPLETION_INSTRUCTION =
            "A Claude Code task-notification status of 'completed' only means the background agent stopped. "
                    + "Inspect its result and verify that the requested outcome or artifact was actually produced. "
                    + "If the result is progress-only or incomplete, resume the same agent with SendMessage using "
                    + "the task-id; do not report success or duplicate the task in the main agent.";
    private static final String AGENT_SELECTION_INSTRUCTION =
            "Claude Code tool-selection rule: use direct Read, Glob, Grep, or Bash only when the target is "
                    + "already known and the operation is local. For open-ended investigation spanning multiple "
                    + "files, independent verification, or parallelizable work, invoke the %s tool and consume "
                    + "its result instead of repeatedly performing the same investigation in the main agent.";
    private static final String EXPLICIT_AGENT_REQUEST_INSTRUCTION =
            "The user explicitly requested delegation, a subagent, or parallel agent work. Invoke the %s tool "
                    + "for the delegated work before attempting to reproduce that work with direct tools.";
    private static final Pattern EXPLICIT_AGENT_REQUEST_PATTERN = Pattern.compile(
            "(?iu)(sub[ -]?agent|\\bagent\\b|delegate|delegation|parallel agent|"
                    + "子\\s*(?:agent|代理)|委派|代理执行|并行(?:调查|分析|处理|执行|agent))");
    private static final Pattern CLAUDE_SYSTEM_REMINDER_PATTERN = Pattern.compile(
            "(?is)<system-reminder>.*?</system-reminder>");
    private static final String MID_CONVERSATION_SYSTEM_PREFIX =
            "<claude-mid-conversation-system>\n";
    private static final String MID_CONVERSATION_SYSTEM_SUFFIX =
            "\n</claude-mid-conversation-system>";

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
        if (isClaudeBedrockPair()) {
            return super.supports(requirement);
        }
        if (isOpenAIResponsesBedrockPair()) {
            return !requirement.toolCallingRequired() && super.supports(requirement);
        }
        return !requirement.streaming()
                && !requirement.toolCallingRequired()
                && !requirement.reasoningRequired()
                && super.supports(requirement);
    }

    private boolean isClaudeBedrockPair() {
        return (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE)
                || (sourceProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE && targetProtocol() == ProtocolType.CLAUDE_MESSAGES);
    }

    private boolean isOpenAIResponsesBedrockPair() {
        return (sourceProtocol() == ProtocolType.OPENAI_RESPONSES && targetProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE)
                || (sourceProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE && targetProtocol() == ProtocolType.OPENAI_RESPONSES);
    }

    @Override
    protected JsonNode convertRequestJson(JsonNode source, ProtocolConversionRequest requirement) {
        if (sourceProtocol() == ProtocolType.CLAUDE_MESSAGES && targetProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE) {
            return claudeRequestToBedrock(source, requirement.routeContext());
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
    protected JsonNode convertResponseJson(JsonNode source, ProtocolConversionRequest requirement) {
        if (sourceProtocol() == ProtocolType.AWS_BEDROCK_CONVERSE && targetProtocol() == ProtocolType.CLAUDE_MESSAGES) {
            return bedrockResponseToClaude(source, requirement.routeContext());
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

    private ObjectNode claudeRequestToBedrock(JsonNode source, ProtocolConversionRouteContext routeContext) {
        validateClaudeRequestFields(source);
        validateClaudeToolHistory(source.get("messages"));
        if (source.path("max_tokens").isNumber() && source.path("max_tokens").asInt() == 0) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_CACHE_ONLY_REQUEST_NOT_SUPPORTED_BY_CONVERSE");
        }
        ObjectNode target = json.objectNode();

        validateContextManagementForConverse(source.get("context_management"));

        ArrayNode bedrockMessages = json.arrayNode();
        JsonNode messages = ClaudeConversationContextOptimizer.applyRequestedContextManagement(
                source.get("messages"), source.get("context_management"));
        if (messages != null && messages.isArray()) {
            for (JsonNode msg : messages) {
                String role = msg.path("role").asText("user");
                appendClaudeMessage(bedrockMessages, role, msg.get("content"), routeContext);
            }
        }
        target.set("messages", bedrockMessages);

        boolean workflowInvocationRequired = workflowInvocationRequired(source.get("tools"), source.get("messages"));
        boolean backgroundTaskCompletionRequiresValidation = backgroundTaskCompletionRequiresValidation(
                source.get("tools"), source.get("messages"));
        String agentSelectionInstruction = agentSelectionInstruction(source.get("tools"), source.get("messages"));
        JsonNode system = source.get("system");
        ArrayNode systemBlocks = json.arrayNode();
        if (system != null && !system.isNull()) {
            systemBlocks.addAll(claudeSystemToBedrock(system));
        }
        if (workflowInvocationRequired) {
            systemBlocks.add(textBlock(WORKFLOW_INVOCATION_INSTRUCTION));
        }
        if (backgroundTaskCompletionRequiresValidation) {
            systemBlocks.add(textBlock(BACKGROUND_TASK_COMPLETION_INSTRUCTION));
        }
        if (!agentSelectionInstruction.isBlank()) {
            systemBlocks.add(textBlock(agentSelectionInstruction));
        }
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
        JsonNode stopSequences = source.get("stop_sequences");
        if (stopSequences != null && stopSequences.isArray() && !stopSequences.isEmpty()) {
            inferenceConfig.set("stopSequences", stopSequences);
        }
        if (!inferenceConfig.isEmpty()) {
            target.set("inferenceConfig", inferenceConfig);
        }

        JsonNode outputConfig = source.hasNonNull("output_config") ? source.get("output_config") : source.get("output_format");
        ObjectNode bedrockOutputConfig = toBedrockOutputConfig(outputConfig);
        if (!bedrockOutputConfig.isEmpty()) {
            target.set("outputConfig", bedrockOutputConfig);
        }

        ClaudeCodeToolState toolState = inferClaudeCodeToolState(source.get("messages"));
        ObjectNode toolConfig = toBedrockToolConfig(source.get("tools"), source.get("tool_choice"), toolState);
        if (!toolConfig.isEmpty()) {
            target.set("toolConfig", toolConfig);
        }

        ObjectNode additionalFields = json.objectNode();
        JsonNode existingAdditionalFields = source.get("additionalModelRequestFields");
        if (existingAdditionalFields != null && existingAdditionalFields.isObject()) {
            existingAdditionalFields.fields().forEachRemaining(entry -> {
                if (!BEDROCK_CLAUDE_ADDITIONAL_REQUEST_FIELDS.contains(entry.getKey())) {
                    throw new ProtocolConversionException(
                            "CLAUDE_BEDROCK_UNSUPPORTED_ADDITIONAL_MODEL_REQUEST_FIELD: " + entry.getKey());
                }
                additionalFields.set(entry.getKey(), entry.getValue());
            });
        }
        JsonNode thinking = source.get("thinking");
        JsonNode reasoning = source.get("reasoning");
        if (thinking != null && !thinking.isNull()) {
            additionalFields.set("thinking", thinking);
        } else if (reasoning != null && !reasoning.isNull()) {
            additionalFields.set("thinking", reasoning);
        }
        if (outputConfig != null && outputConfig.isObject() && outputConfig.hasNonNull("effort")) {
            ObjectNode modelOutputConfig = json.objectNode();
            modelOutputConfig.set("effort", outputConfig.get("effort"));
            additionalFields.set("output_config", modelOutputConfig);
        }
        copyIfPresent(source, additionalFields, "top_k");
        if (!additionalFields.isEmpty()) {
            target.set("additionalModelRequestFields", additionalFields);
        }

        target.set("additionalModelResponseFieldPaths", json.arrayNode()
                .add("/stop_sequence")
                .add("/context_management")
                .add("/stop_details"));
        ObjectNode requestMetadata = toBedrockRequestMetadata(source.get("metadata"));
        if (!requestMetadata.isEmpty()) {
            target.set("requestMetadata", requestMetadata);
        }
        if (source.hasNonNull("service_tier")) {
            target.set("serviceTier", json.objectNode().put(
                    "type", mapClaudeServiceTierToBedrock(source.path("service_tier").asText(""))));
        }
        if (source.hasNonNull("speed")) {
            JsonNode speed = source.get("speed");
            String speedType = speed.isTextual() ? speed.asText("standard") : speed.path("type").asText("standard");
            target.set("performanceConfig", json.objectNode().put(
                    "latency", mapClaudeSpeedToBedrockLatency(speedType)));
        }
        applyTopLevelCacheControl(target, source.get("cache_control"));

        return target;
    }

    private String mapClaudeServiceTierToBedrock(String serviceTier) {
        return switch (serviceTier) {
            case "priority" -> "priority";
            case "flex" -> "flex";
            case "reserved" -> "reserved";
            case "standard_only", "auto", "default" -> "default";
            case "batch" -> throw new ProtocolConversionException(
                    "CLAUDE_BEDROCK_SERVICE_TIER_NOT_SUPPORTED: batch");
            default -> throw new ProtocolConversionException(
                    "CLAUDE_BEDROCK_SERVICE_TIER_NOT_SUPPORTED: " + serviceTier);
        };
    }

    private String mapClaudeSpeedToBedrockLatency(String speed) {
        return switch (speed) {
            case "fast" -> "optimized";
            case "standard" -> "standard";
            default -> throw new ProtocolConversionException(
                    "CLAUDE_BEDROCK_SPEED_NOT_SUPPORTED: " + speed);
        };
    }

    // ==================== Request: OpenAI Chat -> Bedrock Converse ====================

    private ObjectNode chatRequestToBedrock(JsonNode source) {
        ObjectNode target = json.objectNode();

        ArrayNode bedrockMessages = json.arrayNode();
        ArrayNode systemBlocks = json.arrayNode();
        JsonNode messages = source.get("messages");
        if (messages != null && messages.isArray()) {
            for (JsonNode msg : messages) {
                String role = msg.path("role").asText("user");
                if ("system".equals(role)) {
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("text", msg.path("content").asText(""));
                    systemBlocks.add(textBlock);
                    continue;
                }
                String bedrockRole = "assistant".equals(role) ? "assistant" : "user";
                ObjectNode bedrockMsg = json.objectNode();
                bedrockMsg.put("role", bedrockRole);
                ArrayNode content = json.arrayNode();
                ObjectNode textBlock = json.objectNode();
                textBlock.put("text", msg.path("content").asText(""));
                content.add(textBlock);
                bedrockMsg.set("content", content);
                addOrMergeBedrockMessage(bedrockMessages, bedrockMsg);
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
                for (JsonNode item : input) {
                    String role = item.path("role").asText("user");
                    String bedrockRole = "assistant".equals(role) ? "assistant" : "user";
                    ObjectNode msg = json.objectNode();
                    msg.put("role", bedrockRole);
                    ArrayNode content = json.arrayNode();
                    ObjectNode textBlock = json.objectNode();
                    textBlock.put("text", extractOpenAiContentText(item.get("content")));
                    content.add(textBlock);
                    msg.set("content", content);
                    addOrMergeBedrockMessage(bedrockMessages, msg);
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
        Integer reasoningBudgetTokens = reasoningBudgetTokens(source.get("reasoning"));
        if (maxTokens != null && !maxTokens.isNull()) {
            inferenceConfig.put("maxTokens", maxTokens.asInt());
        } else if (reasoningBudgetTokens != null) {
            inferenceConfig.put("maxTokens", reasoningBudgetTokens + 1024);
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

        ObjectNode additionalFields = json.objectNode();
        if (reasoningBudgetTokens != null) {
            ObjectNode thinking = json.objectNode();
            thinking.put("type", "enabled");
            thinking.put("budget_tokens", reasoningBudgetTokens);
            additionalFields.set("thinking", thinking);
        }
        if (!additionalFields.isEmpty()) {
            target.set("additionalModelRequestFields", additionalFields);
        }

        return target;
    }

    // ==================== Response: Bedrock Converse -> Claude Messages ====================

    private ObjectNode bedrockResponseToClaude(JsonNode source, ProtocolConversionRouteContext routeContext) {
        ObjectNode target = json.objectNode();
        target.put("id", "msg_api2api_bedrock");
        target.put("type", "message");
        target.put("role", "assistant");
        target.put("model", "bedrock");

        ArrayNode content = json.arrayNode();
        JsonNode output = source.path("output").path("message").path("content");
        if (output.isArray()) {
            for (JsonNode block : output) {
                appendBedrockBlockAsClaudeContent(content, block, routeContext);
            }
        }
        if (content.isEmpty()) {
            ObjectNode emptyText = json.objectNode();
            emptyText.put("type", "text");
            emptyText.put("text", "");
            content.add(emptyText);
        }
        target.set("content", content);

        String stopReason = mapBedrockStopToClaudeStop(requiredBedrockStopReason(source));
        target.put("stop_reason", stopReason);
        if ("stop_sequence".equals(stopReason)) {
            JsonNode stopSequence = source.path("additionalModelResponseFields").get("stop_sequence");
            if (stopSequence != null && stopSequence.isTextual()) {
                target.put("stop_sequence", stopSequence.asText());
            } else {
                target.putNull("stop_sequence");
            }
        }
        JsonNode additionalFields = source.path("additionalModelResponseFields");
        if (additionalFields.isObject()) {
            copyResponseField(additionalFields, target, "context_management");
            copyResponseField(additionalFields, target, "stop_details");
        }
        JsonNode serviceTier = source.get("serviceTier");
        if (serviceTier != null && serviceTier.isObject() && serviceTier.hasNonNull("type")) {
            target.put("service_tier", serviceTier.path("type").asText());
        }
        JsonNode performanceConfig = source.get("performanceConfig");
        if (performanceConfig != null && performanceConfig.isObject()
                && performanceConfig.hasNonNull("latency")) {
            target.put("speed", "optimized".equals(performanceConfig.path("latency").asText())
                    ? "fast" : "standard");
        }

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

    private void copyResponseField(JsonNode source, ObjectNode target, String field) {
        JsonNode value = source.get(field);
        if (value != null && !value.isNull()) {
            target.set(field, value.deepCopy());
        }
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
        choice.put("finish_reason", mapBedrockStopToFinishReason(requiredBedrockStopReason(source)));
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
        target.put("status", mapBedrockStopToResponsesStatus(requiredBedrockStopReason(source)));

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

    private void validateClaudeRequestFields(JsonNode source) {
        if (source == null || !source.isObject()) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_REQUEST_MUST_BE_OBJECT");
        }
        source.fieldNames().forEachRemaining(field -> {
            if (!CLAUDE_REQUEST_FIELDS.contains(field)) {
                throw new ProtocolConversionException("CLAUDE_BEDROCK_UNSUPPORTED_REQUEST_FIELD: " + field);
            }
        });
        JsonNode outputConfig = source.get("output_config");
        if (outputConfig != null && outputConfig.isObject() && outputConfig.hasNonNull("task_budget")) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_TASK_BUDGET_NOT_SUPPORTED_BY_CONVERSE");
        }
        JsonNode toolChoice = source.get("tool_choice");
        if (toolChoice != null && toolChoice.path("disable_parallel_tool_use").asBoolean(false)) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_DISABLE_PARALLEL_TOOL_USE_NOT_SUPPORTED_BY_CONVERSE");
        }
    }

    private void validateClaudeToolHistory(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return;
        }
        Set<String> knownToolUseIds = new HashSet<>();
        Set<String> completedToolUseIds = new HashSet<>();
        for (JsonNode message : messages) {
            JsonNode content = message.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                String type = block.path("type").asText("");
                if ("server_tool_use".equals(type) || (type.endsWith("_tool_result")
                        && !Set.of("tool_result", "mcp_tool_result").contains(type))) {
                    throw new ProtocolConversionException(
                            "CLAUDE_BEDROCK_SERVER_TOOL_NOT_SUPPORTED_BY_CONVERSE: " + type);
                }
                if ("tool_use".equals(type) || "mcp_tool_use".equals(type)) {
                    if (!"assistant".equals(message.path("role").asText(""))) {
                        throw new ProtocolConversionException(
                                "CLAUDE_BEDROCK_TOOL_USE_REQUIRES_ASSISTANT_ROLE");
                    }
                    String id = block.path("id").asText("");
                    if (id.isBlank()) {
                        throw new ProtocolConversionException("CLAUDE_BEDROCK_TOOL_USE_ID_REQUIRED");
                    }
                    if (!knownToolUseIds.add(id)) {
                        throw new ProtocolConversionException(
                                "CLAUDE_BEDROCK_DUPLICATE_TOOL_USE_ID: " + id);
                    }
                    continue;
                }
                if ("tool_result".equals(type) || "mcp_tool_result".equals(type)) {
                    if (!"user".equals(message.path("role").asText(""))) {
                        throw new ProtocolConversionException(
                                "CLAUDE_BEDROCK_TOOL_RESULT_REQUIRES_USER_ROLE");
                    }
                    String id = block.path("tool_use_id").asText("");
                    if (id.isBlank() || !knownToolUseIds.contains(id)) {
                        throw new ProtocolConversionException(
                                "CLAUDE_BEDROCK_TOOL_RESULT_WITHOUT_TOOL_USE: " + id);
                    }
                    if (!completedToolUseIds.add(id)) {
                        throw new ProtocolConversionException(
                                "CLAUDE_BEDROCK_DUPLICATE_TOOL_RESULT: " + id);
                    }
                }
            }
        }
    }

    private void validateContextManagementForConverse(JsonNode contextManagement) {
        if (contextManagement == null || contextManagement.isNull()) {
            return;
        }
        JsonNode edits = contextManagement.path("edits");
        if (!contextManagement.isObject() || !edits.isArray()) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_INVALID_CONTEXT_MANAGEMENT");
        }
        for (JsonNode edit : edits) {
            String type = edit.path("type").asText("");
            if ("clear_thinking_20251015".equals(type) || "clear_tool_uses_20250919".equals(type)) {
                continue;
            }
            if (type.startsWith("compact_")) {
                throw new ProtocolConversionException(
                        "CLAUDE_BEDROCK_COMPACTION_REQUIRES_NATIVE_MESSAGES_INVOKE: " + type);
            }
            throw new ProtocolConversionException(
                    "CLAUDE_BEDROCK_CONTEXT_MANAGEMENT_NOT_SUPPORTED_BY_CONVERSE: "
                            + (type.isBlank() ? "unknown" : type)
            );
        }
    }

    private void appendClaudeMessage(
            ArrayNode messages,
            String role,
            JsonNode content,
            ProtocolConversionRouteContext routeContext
    ) {
        if (content == null || !content.isArray()) {
            addOrMergeBedrockMessage(messages, toBedrockMessage(role, content, routeContext));
            return;
        }
        ArrayNode pending = json.arrayNode();
        for (JsonNode block : content) {
            String type = block.path("type").asText("");
            if ("fallback".equals(type) || "compaction".equals(type)) {
                continue;
            }
            if (!"mid_conv_system".equals(type)) {
                pending.add(block);
                continue;
            }
            if (!pending.isEmpty()) {
                addOrMergeBedrockMessage(messages, toBedrockMessage(role, pending, routeContext));
                pending = json.arrayNode();
            }
            JsonNode systemContent = block.get("content");
            if (systemContent == null || (!systemContent.isArray() && !systemContent.isTextual())) {
                throw new ProtocolConversionException("CLAUDE_BEDROCK_INVALID_MID_CONVERSATION_SYSTEM_CONTENT");
            }
            ObjectNode systemMessage = toMidConversationSystemMessage(systemContent);
            if (hasEphemeralCacheControl(block)) {
                ((ArrayNode) systemMessage.path("content")).add(cachePointBlock(block.get("cache_control")));
            }
            // Claude combines consecutive turns with the same role. The explicit
            // delimiter keeps the mid-conversation instruction visible after that
            // normalization without emitting Converse's unsupported message role.
            addOrMergeBedrockMessage(messages, systemMessage);
        }
        if (!pending.isEmpty()) {
            addOrMergeBedrockMessage(messages, toBedrockMessage(role, pending, routeContext));
        }
    }

    private void addOrMergeBedrockMessage(ArrayNode messages, ObjectNode message) {
        if (!messages.isEmpty()) {
            JsonNode previous = messages.get(messages.size() - 1);
            if (previous.path("role").asText().equals(message.path("role").asText())) {
                ((ArrayNode) previous.path("content")).addAll((ArrayNode) message.path("content"));
                return;
            }
        }
        messages.add(message);
    }

    private ObjectNode toMidConversationSystemMessage(JsonNode content) {
        ObjectNode message = json.objectNode();
        message.put("role", "user");
        ArrayNode blocks = json.arrayNode();
        String text = firstTextFromClaudeContent(content);
        blocks.add(textBlock(MID_CONVERSATION_SYSTEM_PREFIX + text + MID_CONVERSATION_SYSTEM_SUFFIX));
        message.set("content", blocks);
        return message;
    }

    private ObjectNode toBedrockOutputConfig(JsonNode outputConfig) {
        ObjectNode target = json.objectNode();
        if (outputConfig == null || outputConfig.isNull()) {
            return target;
        }
        JsonNode format = outputConfig.isObject() && outputConfig.has("format") ? outputConfig.get("format") : outputConfig;
        if (format == null || format.isNull()) {
            return target;
        }
        if (!format.isObject() || !"json_schema".equals(format.path("type").asText(""))) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_UNSUPPORTED_OUTPUT_FORMAT");
        }
        JsonNode schema = format.get("schema");
        if (schema == null || !schema.isObject()) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_JSON_SCHEMA_REQUIRED");
        }
        ObjectNode jsonSchema = json.objectNode();
        jsonSchema.put("name", format.path("name").asText("claude_code_output"));
        if (format.hasNonNull("description")) {
            jsonSchema.put("description", format.path("description").asText());
        }
        jsonSchema.put("schema", json.stringify(schema, "Claude output_config.format.schema"));
        ObjectNode structure = json.objectNode();
        structure.set("jsonSchema", jsonSchema);
        ObjectNode textFormat = json.objectNode();
        textFormat.put("type", "json_schema");
        textFormat.set("structure", structure);
        target.set("textFormat", textFormat);
        return target;
    }

    private ObjectNode toBedrockRequestMetadata(JsonNode metadata) {
        ObjectNode target = json.objectNode();
        if (metadata == null || !metadata.isObject()) {
            return target;
        }
        metadata.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value == null || !value.isValueNode() || value.isNull()) {
                return;
            }
            String text = value.asText();
            if (BEDROCK_REQUEST_METADATA_VALUE_PATTERN.matcher(text).matches()) {
                target.put(entry.getKey(), text);
            }
        });
        return target;
    }

    private void applyTopLevelCacheControl(ObjectNode target, JsonNode cacheControl) {
        if (!isEphemeralCacheControl(cacheControl)) {
            return;
        }
        if (countCachePoints(target) >= 4) {
            return;
        }
        ObjectNode cachePoint = cachePointBlock(cacheControl);
        JsonNode tools = target.path("toolConfig").get("tools");
        if (tools != null && tools.isArray() && !tools.isEmpty()) {
            ((ArrayNode) tools).add(cachePoint);
            return;
        }
        JsonNode system = target.get("system");
        if (system != null && system.isArray() && !system.isEmpty()) {
            ((ArrayNode) system).add(cachePoint);
            return;
        }
        JsonNode messages = target.get("messages");
        if (messages != null && messages.isArray() && !messages.isEmpty()) {
            ((ArrayNode) messages.get(messages.size() - 1).path("content")).add(cachePoint);
        }
    }

    private int countCachePoints(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        int count = node.has("cachePoint") ? 1 : 0;
        for (JsonNode child : node) {
            count += countCachePoints(child);
        }
        return count;
    }

    private ArrayNode claudeSystemToBedrock(JsonNode system) {
        ArrayNode systemBlocks = json.arrayNode();
        if (system.isTextual()) {
            systemBlocks.add(textBlock(system.asText()));
            return systemBlocks;
        }
        if (!system.isArray()) {
            systemBlocks.add(textBlock(firstTextFromClaudeContent(system)));
            return systemBlocks;
        }
        for (JsonNode item : system) {
            if (!"text".equals(item.path("type").asText(""))) {
                continue;
            }
            ObjectNode textBlock = textBlock(item.path("text").asText(""));
            systemBlocks.add(textBlock);
            if (hasEphemeralCacheControl(item)) {
                systemBlocks.add(cachePointBlock(item.get("cache_control")));
            }
        }
        if (systemBlocks.isEmpty()) {
            String text = firstTextFromClaudeContent(system);
            if (!text.isBlank()) {
                systemBlocks.add(textBlock(text));
            }
        }
        return systemBlocks;
    }

    private boolean hasEphemeralCacheControl(JsonNode item) {
        JsonNode cacheControl = item.get("cache_control");
        return isEphemeralCacheControl(cacheControl);
    }

    private boolean isEphemeralCacheControl(JsonNode cacheControl) {
        return cacheControl != null
                && cacheControl.isObject()
                && "ephemeral".equals(cacheControl.path("type").asText(""));
    }

    private ObjectNode cachePointBlock(JsonNode cacheControl) {
        ObjectNode cachePointBlock = json.objectNode();
        ObjectNode cachePoint = json.objectNode().put("type", "default");
        String ttl = cacheControl == null ? "" : cacheControl.path("ttl").asText("");
        if ("5m".equals(ttl) || "1h".equals(ttl)) {
            cachePoint.put("ttl", ttl);
        }
        cachePointBlock.set("cachePoint", cachePoint);
        return cachePointBlock;
    }

    private ObjectNode toBedrockMessage(
            String role,
            JsonNode claudeContent,
            ProtocolConversionRouteContext routeContext
    ) {
        ObjectNode msg = json.objectNode();
        msg.put("role", switch (role) {
            case "assistant" -> "assistant";
            case "system" -> "system";
            default -> "user";
        });
        ArrayNode contentBlocks = json.arrayNode();

        if (claudeContent == null || claudeContent.isNull()) {
            contentBlocks.add(textBlock(""));
        } else if (claudeContent.isTextual()) {
            contentBlocks.add(textBlock(claudeContent.asText()));
        } else if (claudeContent.isArray()) {
            for (JsonNode item : claudeContent) {
                String type = item.path("type").asText("");
                boolean blockMapped = true;
                switch (type) {
                    case "text" -> contentBlocks.add(textBlock(item.path("text").asText("")));
                    case "image" -> contentBlocks.add(toBedrockImage(item));
                    case "document" -> contentBlocks.add(toBedrockDocument(item));
                    case "search_result" -> contentBlocks.add(toBedrockSearchResult(item));
                    case "tool_use", "mcp_tool_use" -> contentBlocks.add(toBedrockToolUse(item));
                    case "server_tool_use", "web_search_tool_result", "web_fetch_tool_result",
                         "code_execution_tool_result", "bash_code_execution_tool_result",
                         "text_editor_code_execution_tool_result", "tool_search_tool_result" ->
                            throw new ProtocolConversionException(
                                    "CLAUDE_BEDROCK_SERVER_TOOL_NOT_SUPPORTED_BY_CONVERSE: " + type);
                    case "tool_result", "mcp_tool_result" -> contentBlocks.add(toBedrockToolResult(item));
                    case "thinking", "reasoning" -> {
                        String signature = item.path("signature").asText("");
                        Optional<String> bedrockSignature = BedrockReasoningBridge.decode(signature, routeContext);
                        if (bedrockSignature.isEmpty()) {
                            // Bedrock signatures are bound to the originating route and model.
                            // After failover or a route change the old thinking block cannot be
                            // replayed, but the visible assistant content remains valid history.
                            blockMapped = false;
                        } else {
                            contentBlocks.add(toBedrockReasoningContent(item, bedrockSignature.orElseThrow()));
                        }
                    }
                    case "redacted_thinking" -> contentBlocks.add(toBedrockRedactedReasoningContent(item));
                    case "compaction" -> { continue; }
                    default -> throw new ProtocolConversionException("CLAUDE_BEDROCK_UNSUPPORTED_CONTENT_BLOCK: " + type);
                }
                if (blockMapped && hasEphemeralCacheControl(item)) {
                    contentBlocks.add(cachePointBlock(item.get("cache_control")));
                }
            }
            if (contentBlocks.isEmpty()) {
                contentBlocks.add(textBlock(""));
            }
        }

        msg.set("content", contentBlocks);
        return msg;
    }

    private ObjectNode toBedrockImage(JsonNode item) {
        JsonNode source = item.path("source");
        if (!"base64".equals(source.path("type").asText("")) || source.path("data").asText("").isBlank()) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_IMAGE_SOURCE_REQUIRES_BASE64");
        }
        String mediaType = source.path("media_type").asText("image/png");
        String format = mediaType.substring(mediaType.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        if ("jpg".equals(format)) {
            format = "jpeg";
        }
        if (!Set.of("png", "jpeg", "gif", "webp").contains(format)) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_UNSUPPORTED_IMAGE_FORMAT: " + format);
        }
        ObjectNode image = json.objectNode();
        image.put("format", format);
        image.set("source", json.objectNode().put("bytes", source.path("data").asText()));
        return json.objectNode().set("image", image);
    }

    private ObjectNode toBedrockDocument(JsonNode item) {
        JsonNode source = item.path("source");
        ObjectNode document = json.objectNode();
        String name = sanitizeDocumentName(item.path("title").asText("document"));
        document.put("name", name);
        String sourceType = source.path("type").asText("");
        ObjectNode bedrockSource = json.objectNode();
        if ("base64".equals(sourceType)) {
            String data = source.path("data").asText("");
            if (data.isBlank()) {
                throw new ProtocolConversionException("CLAUDE_BEDROCK_DOCUMENT_DATA_REQUIRED");
            }
            document.put("format", documentFormat(source.path("media_type").asText("application/pdf")));
            bedrockSource.put("bytes", data);
        } else if ("text".equals(sourceType)) {
            document.put("format", "txt");
            bedrockSource.put("text", source.path("data").asText(""));
        } else if ("content".equals(sourceType)) {
            document.put("format", "txt");
            bedrockSource.put("text", firstTextFromClaudeContent(source.get("content")));
        } else {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_DOCUMENT_SOURCE_NOT_SUPPORTED: " + sourceType);
        }
        document.set("source", bedrockSource);
        if (item.hasNonNull("context")) {
            document.put("context", item.path("context").asText());
        }
        if (item.path("citations").has("enabled")) {
            document.set("citations", json.objectNode().put("enabled", item.path("citations").path("enabled").asBoolean()));
        }
        return json.objectNode().set("document", document);
    }

    private String sanitizeDocumentName(String name) {
        String sanitized = (name == null || name.isBlank() ? "document" : name)
                .replaceAll("[^a-zA-Z0-9\\s()\\[\\]-]", "-")
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.isBlank()) {
            sanitized = "document";
        }
        return sanitized.length() > 200 ? sanitized.substring(0, 200) : sanitized;
    }

    private String documentFormat(String mediaType) {
        return switch (mediaType) {
            case "application/pdf" -> "pdf";
            case "text/csv" -> "csv";
            case "text/html" -> "html";
            case "text/markdown" -> "md";
            case "text/plain" -> "txt";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            default -> throw new ProtocolConversionException("CLAUDE_BEDROCK_UNSUPPORTED_DOCUMENT_MEDIA_TYPE: " + mediaType);
        };
    }

    private ObjectNode toBedrockSearchResult(JsonNode item) {
        ObjectNode searchResult = json.objectNode();
        searchResult.put("source", item.path("source").asText(""));
        searchResult.put("title", item.path("title").asText(item.path("source").asText("search result")));
        ArrayNode content = json.arrayNode();
        JsonNode sourceContent = item.get("content");
        if (sourceContent != null && sourceContent.isArray()) {
            for (JsonNode block : sourceContent) {
                if (!"text".equals(block.path("type").asText(""))) {
                    throw new ProtocolConversionException("CLAUDE_BEDROCK_SEARCH_RESULT_REQUIRES_TEXT_CONTENT");
                }
                content.add(textBlock(block.path("text").asText("")));
            }
        }
        if (content.isEmpty()) {
            content.add(textBlock(firstTextFromClaudeContent(sourceContent)));
        }
        searchResult.set("content", content);
        if (item.path("citations").has("enabled")) {
            searchResult.set("citations", json.objectNode().put("enabled", item.path("citations").path("enabled").asBoolean()));
        }
        return json.objectNode().set("searchResult", searchResult);
    }

    private ObjectNode textBlock(String text) {
        ObjectNode textBlock = json.objectNode();
        textBlock.put("text", text == null ? "" : text);
        return textBlock;
    }

    private ObjectNode toBedrockToolUse(JsonNode item) {
        ObjectNode block = json.objectNode();
        ObjectNode toolUse = json.objectNode();
        toolUse.put("toolUseId", item.path("id").asText(""));
        toolUse.put("name", item.path("name").asText(""));
        JsonNode input = item.get("input");
        toolUse.set("input", input == null || input.isNull() ? json.objectNode() : input);
        block.set("toolUse", toolUse);
        return block;
    }

    private ObjectNode toBedrockToolResult(JsonNode item) {
        ObjectNode block = json.objectNode();
        ObjectNode toolResult = json.objectNode();
        toolResult.put("toolUseId", item.path("tool_use_id").asText(""));
        JsonNode isError = item.get("is_error");
        toolResult.put("status", isError != null && isError.asBoolean(false) ? "error" : "success");
        ArrayNode content = json.arrayNode();
        JsonNode resultContent = item.get("content");
        if (resultContent == null || resultContent.isNull()) {
            content.add(textBlock(""));
        } else if (resultContent.isTextual()) {
            content.add(textBlock(resultContent.asText()));
        } else if (resultContent.isArray()) {
            for (JsonNode contentItem : resultContent) {
                if (contentItem.isTextual()) {
                    content.add(textBlock(contentItem.asText()));
                    continue;
                }
                String type = contentItem.path("type").asText("");
                switch (type) {
                    case "text" -> content.add(textBlock(contentItem.path("text").asText("")));
                    case "image" -> content.add(toBedrockImage(contentItem));
                    case "document" -> content.add(toBedrockDocument(contentItem));
                    case "search_result" -> content.add(toBedrockSearchResult(contentItem));
                    case "json" -> content.add(json.objectNode().set("json", contentItem.path("json")));
                    default -> {
                        if (type.isBlank()) {
                            content.add(json.objectNode().set("json", contentItem));
                        } else {
                            throw new ProtocolConversionException("CLAUDE_BEDROCK_UNSUPPORTED_TOOL_RESULT_CONTENT: " + type);
                        }
                    }
                }
            }
        } else {
            content.add(json.objectNode().set("json", resultContent));
        }
        if (content.isEmpty()) {
            content.add(textBlock(""));
        }
        toolResult.set("content", content);
        block.set("toolResult", toolResult);
        return block;
    }

    private ObjectNode toBedrockReasoningContent(JsonNode item, String signature) {
        ObjectNode block = json.objectNode();
        ObjectNode reasoningContent = json.objectNode();
        ObjectNode reasoningText = json.objectNode();
        reasoningText.put("text", item.path("thinking").asText(item.path("text").asText("")));
        reasoningText.put("signature", signature);
        reasoningContent.set("reasoningText", reasoningText);
        block.set("reasoningContent", reasoningContent);
        return block;
    }

    private ObjectNode toBedrockRedactedReasoningContent(JsonNode item) {
        String data = item.path("data").asText("");
        if (data.isBlank()) {
            throw new ProtocolConversionException("CLAUDE_BEDROCK_REDACTED_THINKING_DATA_REQUIRED");
        }
        ObjectNode block = json.objectNode();
        block.set("reasoningContent", json.objectNode().put("redactedContent", data));
        return block;
    }

    private Integer reasoningBudgetTokens(JsonNode reasoning) {
        if (reasoning == null || reasoning.isMissingNode() || reasoning.isNull()) {
            return null;
        }
        JsonNode budgetTokens = reasoning.get("budget_tokens");
        if (budgetTokens != null && budgetTokens.canConvertToInt() && budgetTokens.asInt() > 0) {
            return Math.max(1024, budgetTokens.asInt());
        }
        String effort = reasoning.path("effort").asText("medium");
        return switch (effort) {
            case "low" -> 1024;
            case "high" -> 4096;
            default -> 2048;
        };
    }

    private ObjectNode toBedrockToolConfig(
            JsonNode tools,
            JsonNode toolChoice,
            ClaudeCodeToolState toolState
    ) {
        ObjectNode toolConfig = json.objectNode();
        String choiceType = toolChoice == null || toolChoice.isNull()
                ? "auto"
                : (toolChoice.isTextual() ? toolChoice.asText("auto") : toolChoice.path("type").asText("auto"));
        if ("none".equalsIgnoreCase(choiceType)) {
            return toolConfig;
        }
        if (tools != null && tools.isArray() && !tools.isEmpty()) {
            ArrayNode bedrockTools = json.arrayNode();
            Set<String> exposedToolNames = new HashSet<>();
            for (JsonNode tool : tools) {
                String toolName = tool.path("name").asText("");
                if (!isToolAvailableInCurrentState(toolName, toolState)) {
                    continue;
                }
                String type = tool.path("type").asText("custom");
                if (type.startsWith("tool_search_tool")) {
                    // Converse cannot run Anthropic's server-side tool search.
                    // Deferred custom tools are expanded below as ordinary
                    // tool specs so their functional capability is retained.
                    continue;
                }
                if (!"custom".equals(type) && !type.isBlank()) {
                    throw new ProtocolConversionException("CLAUDE_BEDROCK_SERVER_TOOL_NOT_SUPPORTED_BY_CONVERSE: " + type);
                }
                if (!hasOnlyDirectAllowedCallers(tool.get("allowed_callers"))) {
                    throw new ProtocolConversionException(
                            "CLAUDE_BEDROCK_PROGRAMMATIC_TOOL_CALLING_NOT_SUPPORTED_BY_CONVERSE: "
                                    + tool.path("name").asText("unnamed")
                    );
                }
                ObjectNode toolWrapper = json.objectNode();
                ObjectNode toolSpec = json.objectNode();
                toolSpec.put("name", toolName);
                String description = tool.path("description").asText("");
                if (tool.path("input_examples").isArray() && !tool.path("input_examples").isEmpty()) {
                    description = description + (description.isBlank() ? "" : "\n\n")
                            + "Input examples: " + tool.path("input_examples");
                }
                if (!description.isBlank()) {
                    toolSpec.put("description", description);
                }
                ObjectNode inputSchema = json.objectNode();
                JsonNode schema = tool.get("input_schema");
                if (schema == null || schema.isNull()) {
                    schema = tool.get("inputSchema");
                }
                inputSchema.set("json", schema == null || schema.isNull() ? json.objectNode() : schema);
                toolSpec.set("inputSchema", inputSchema);
                if (tool.hasNonNull("strict")) {
                    toolSpec.put("strict", tool.path("strict").asBoolean());
                }
                toolWrapper.set("toolSpec", toolSpec);
                bedrockTools.add(toolWrapper);
                exposedToolNames.add(toolName);
                if (hasEphemeralCacheControl(tool)) {
                    bedrockTools.add(cachePointBlock(tool.get("cache_control")));
                }
            }
            if (exposedToolNames.isEmpty()) {
                return toolConfig;
            }
            toolConfig.set("tools", bedrockTools);
            ObjectNode mappedChoice = toBedrockToolChoice(toolChoice, exposedToolNames);
            if (!mappedChoice.isEmpty()) {
                toolConfig.set("toolChoice", mappedChoice);
            }
            return toolConfig;
        }
        ObjectNode mappedChoice = toBedrockToolChoice(toolChoice, Set.of());
        if (!mappedChoice.isEmpty()) {
            toolConfig.set("toolChoice", mappedChoice);
        }
        return toolConfig;
    }

    /**
     * Infers the current Claude Code plan-mode state by scanning the conversation history.
     *
     * <h3>State machine rules:</h3>
     * <ul>
     *   <li>A successful EnterPlanMode transitions to ACTIVE</li>
     *   <li>A successful ExitPlanMode transitions to INACTIVE</li>
     *   <li>A failed ExitPlanMode containing "not in plan mode" in its result
     *       also transitions to INACTIVE (client sent exit while already inactive)</li>
     *   <li>When state is UNKNOWN, a successful AskUserQuestion implies ACTIVE</li>
     * </ul>
     *
     * <h3>Tool name normalization:</h3>
     * Tool names are compared case-insensitively with underscores/hyphens removed
     * to accommodate Claude Code clients that use snake_case or kebab-case variants.
     *
     * <h3>AskUserQuestion suppression:</h3>
     * When the most recent completed tool is a successful AskUserQuestion, the tool
     * is hidden from the next turn to prevent the model from asking again immediately.
     *
     * @param messages the full conversation message array (Bedrock format)
     * @return inferred tool state containing plan-mode and suppression flags
     */
    private ClaudeCodeToolState inferClaudeCodeToolState(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return ClaudeCodeToolState.inactive();
        }
        Map<String, String> toolUses = new HashMap<>();
        ClaudeCodePlanState planState = ClaudeCodePlanState.UNKNOWN;
        String lastCompletedTool = "";
        boolean lastToolSucceeded = false;
        for (JsonNode message : messages) {
            JsonNode content = message.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                String blockType = block.path("type").asText("");
                JsonNode toolUse = "toolUse".equals(blockType) && block.path("toolUse").isObject()
                        ? block.path("toolUse")
                        : block;
                if ("tool_use".equals(blockType) || "toolUse".equals(blockType) || block.has("toolUse")) {
                    String name = canonicalPlanToolName(toolUse.path("name").asText(""));
                    String id = toolUse.has("id")
                            ? toolUse.path("id").asText("")
                            : toolUse.path("toolUseId").asText("");
                    toolUses.put(id, name);
                    continue;
                }
                JsonNode toolResult = "toolResult".equals(blockType) && block.path("toolResult").isObject()
                        ? block.path("toolResult")
                        : block;
                if (!"tool_result".equals(blockType) && !"toolResult".equals(blockType)
                        && !block.has("toolResult")) {
                    continue;
                }
                String resultId = toolResult.has("tool_use_id")
                        ? toolResult.path("tool_use_id").asText("")
                        : toolResult.path("toolUseId").asText("");
                String completedTool = toolUses.get(resultId);
                if (completedTool == null) {
                    continue;
                }
                boolean failed = toolResult.path("is_error").asBoolean(false)
                        || "error".equalsIgnoreCase(toolResult.path("status").asText(""));
                lastCompletedTool = completedTool;
                lastToolSucceeded = !failed;
                if (!failed && isPlanTool(completedTool, ENTER_PLAN_MODE_TOOL)) {
                    planState = ClaudeCodePlanState.ACTIVE;
                } else if (!failed && isPlanTool(completedTool, EXIT_PLAN_MODE_TOOL)) {
                    planState = ClaudeCodePlanState.INACTIVE;
                } else if (failed && isPlanTool(completedTool, EXIT_PLAN_MODE_TOOL)
                        && firstTextFromClaudeContent(toolResult.get("content")).toLowerCase(Locale.ROOT)
                        .contains("not in plan mode")) {
                    planState = ClaudeCodePlanState.INACTIVE;
                }
            }
        }
        boolean suppressAskUserQuestion = lastToolSucceeded
                && isPlanTool(lastCompletedTool, ASK_USER_QUESTION_TOOL);
        if (planState == ClaudeCodePlanState.UNKNOWN) {
            planState = suppressAskUserQuestion
                    ? ClaudeCodePlanState.ACTIVE
                    : ClaudeCodePlanState.INACTIVE;
        }
        return new ClaudeCodeToolState(planState, suppressAskUserQuestion);
    }

    private boolean isToolAvailableInCurrentState(
            String toolName,
            ClaudeCodeToolState state
    ) {
        if (state.suppressAskUserQuestion() && isPlanTool(toolName, ASK_USER_QUESTION_TOOL)) {
            return false;
        }
        if (state.planState() == ClaudeCodePlanState.ACTIVE) {
            return !isPlanTool(toolName, ENTER_PLAN_MODE_TOOL);
        }
        if (state.planState() == ClaudeCodePlanState.INACTIVE) {
            return !isPlanTool(toolName, EXIT_PLAN_MODE_TOOL);
        }
        return true;
    }

    private boolean isPlanTool(String actualName, String expectedName) {
        return canonicalPlanToolName(actualName).equals(expectedName);
    }

    private String canonicalPlanToolName(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        if (normalized.equals("enterplanmode")) {
            return ENTER_PLAN_MODE_TOOL;
        }
        if (normalized.equals("exitplanmode")) {
            return EXIT_PLAN_MODE_TOOL;
        }
        if (normalized.equals("askuserquestion")) {
            return ASK_USER_QUESTION_TOOL;
        }
        return name;
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

    private boolean workflowInvocationRequired(JsonNode tools, JsonNode messages) {
        return containsToolNamed(tools, WORKFLOW_TOOL) && lastUserMessageRequiresWorkflow(messages);
    }

    private boolean backgroundTaskCompletionRequiresValidation(JsonNode tools, JsonNode messages) {
        if (!containsToolNamed(tools, SEND_MESSAGE_TOOL)) {
            return false;
        }
        String text = lastUserMessageText(messages);
        return text.contains(TASK_NOTIFICATION_TAG)
                && text.contains(TASK_COMPLETED_TAG)
                && text.contains(TASK_ID_TAG);
    }

    private boolean containsToolNamed(JsonNode tools, String expectedName) {
        if (tools == null || !tools.isArray()) {
            return false;
        }
        for (JsonNode tool : tools) {
            if (expectedName.equals(tool.path("name").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private String agentSelectionInstruction(JsonNode tools, JsonNode originalMessages) {
        JsonNode agentTool = findAgentTool(tools);
        if (agentTool == null) {
            return "";
        }
        String toolName = agentTool.path("name").asText(AGENT_TOOL);
        String latestInstruction = CLAUDE_SYSTEM_REMINDER_PATTERN
                .matcher(lastUserInstructionText(originalMessages)).replaceAll(" ");
        boolean explicitlyRequested = EXPLICIT_AGENT_REQUEST_PATTERN.matcher(latestInstruction).find();
        String description = agentTool.path("description").asText("").toLowerCase(Locale.ROOT);
        boolean proactiveUseProhibited = description.contains("do not spawn agents unless the user asks")
                || description.contains("only use this tool when the user explicitly")
                || description.contains("only use this tool when the user asks");
        if (proactiveUseProhibited && !explicitlyRequested) {
            return "";
        }
        if (explicitlyRequested) {
            return EXPLICIT_AGENT_REQUEST_INSTRUCTION.formatted(toolName);
        }
        return AGENT_SELECTION_INSTRUCTION.formatted(toolName);
    }

    private JsonNode findAgentTool(JsonNode tools) {
        if (tools == null || !tools.isArray()) {
            return null;
        }
        for (JsonNode tool : tools) {
            String name = tool.path("name").asText("");
            if (AGENT_TOOL.equals(name)) {
                return tool;
            }
            JsonNode schema = tool.hasNonNull("input_schema") ? tool.get("input_schema") : tool.get("inputSchema");
            if (LEGACY_AGENT_TOOL.equals(name) && schema != null
                    && schema.path("properties").has("prompt")
                    && schema.path("properties").has("subagent_type")) {
                return tool;
            }
        }
        return null;
    }

    private String lastUserInstructionText(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return "";
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"user".equals(message.path("role").asText(""))) {
                continue;
            }
            JsonNode content = message.get("content");
            if (content == null || content.isNull()) {
                continue;
            }
            if (content.isTextual()) {
                return content.asText("");
            }
            if (content.isArray()) {
                StringBuilder instruction = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText(""))) {
                        if (!instruction.isEmpty()) {
                            instruction.append('\n');
                        }
                        instruction.append(block.path("text").asText(""));
                    }
                }
                if (!instruction.isEmpty()) {
                    return instruction.toString();
                }
            }
        }
        return "";
    }

    private boolean lastUserMessageRequiresWorkflow(JsonNode messages) {
        String text = lastUserMessageText(messages).stripLeading();
        return text.startsWith(WORKFLOW_PROMPT_PREFIX)
                && text.contains("\" workflow.")
                && text.contains(WORKFLOW_INVOKE_DIRECTIVE);
    }

    private String lastUserMessageText(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return "";
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"user".equals(message.path("role").asText(""))) {
                continue;
            }
            return firstTextFromClaudeContent(message.get("content"));
        }
        return "";
    }

    private ObjectNode toBedrockToolChoice(JsonNode toolChoice, Set<String> exposedToolNames) {
        ObjectNode mappedChoice = json.objectNode();
        if (toolChoice == null || toolChoice.isNull()) {
            return mappedChoice;
        }
        if (toolChoice.isTextual()) {
            putTextualToolChoice(mappedChoice, toolChoice.asText("auto"));
            return mappedChoice;
        }
        String type = toolChoice.path("type").asText("auto");
        if ("tool".equals(type)) {
            String name = toolChoice.path("name").asText("");
            if (!exposedToolNames.contains(name)) {
                mappedChoice.set("auto", json.objectNode());
                return mappedChoice;
            }
            ObjectNode tool = json.objectNode();
            tool.put("name", name);
            mappedChoice.set("tool", tool);
            return mappedChoice;
        }
        putTextualToolChoice(mappedChoice, type);
        return mappedChoice;
    }

    private enum ClaudeCodePlanState {
        UNKNOWN,
        ACTIVE,
        INACTIVE
    }

    private record ClaudeCodeToolState(
            ClaudeCodePlanState planState,
            boolean suppressAskUserQuestion
    ) {
        private static ClaudeCodeToolState unknown() {
            return new ClaudeCodeToolState(ClaudeCodePlanState.UNKNOWN, false);
        }

        private static ClaudeCodeToolState inactive() {
            return new ClaudeCodeToolState(ClaudeCodePlanState.INACTIVE, false);
        }
    }

    private void putTextualToolChoice(ObjectNode mappedChoice, String type) {
        if ("any".equalsIgnoreCase(type)) {
            mappedChoice.set("any", json.objectNode());
        } else if (!"none".equalsIgnoreCase(type)) {
            mappedChoice.set("auto", json.objectNode());
        }
    }

    private void appendBedrockBlockAsClaudeContent(
            ArrayNode content,
            JsonNode block,
            ProtocolConversionRouteContext routeContext
    ) {
        if (block.has("text")) {
            ObjectNode textBlock = json.objectNode();
            textBlock.put("type", "text");
            textBlock.put("text", block.path("text").asText(""));
            content.add(textBlock);
            return;
        }
        JsonNode toolUse = block.get("toolUse");
        if (toolUse != null && toolUse.isObject()) {
            ObjectNode toolBlock = json.objectNode();
            toolBlock.put("type", "tool_use");
            toolBlock.put("id", toolUse.path("toolUseId").asText(""));
            toolBlock.put("name", toolUse.path("name").asText(""));
            JsonNode input = toolUse.get("input");
            toolBlock.set("input", input == null || input.isNull() ? json.objectNode() : input);
            content.add(toolBlock);
            return;
        }
        JsonNode reasoningText = reasoningTextNode(block);
        if (reasoningText != null && !reasoningText.isMissingNode() && !reasoningText.isNull()) {
            ObjectNode thinkingBlock = json.objectNode();
            thinkingBlock.put("type", "thinking");
            thinkingBlock.put("thinking", reasoningText.path("text").asText(reasoningText.asText("")));
            JsonNode signature = reasoningText.get("signature");
            if (signature != null && signature.isTextual() && !signature.asText().isBlank()) {
                thinkingBlock.put("signature", BedrockReasoningBridge.encode(signature.asText(), routeContext));
            }
            content.add(thinkingBlock);
            return;
        }
        JsonNode reasoningContent = block.get("reasoningContent");
        if (reasoningContent != null && reasoningContent.hasNonNull("redactedContent")) {
            ObjectNode redacted = json.objectNode();
            redacted.put("type", "redacted_thinking");
            redacted.put("data", reasoningContent.path("redactedContent").asText());
            content.add(redacted);
            return;
        }
        JsonNode citationsContent = block.get("citationsContent");
        if (citationsContent != null && citationsContent.isObject()) {
            StringBuilder text = new StringBuilder();
            JsonNode generated = citationsContent.get("content");
            if (generated != null && generated.isArray()) {
                for (JsonNode part : generated) {
                    if (part.hasNonNull("text")) {
                        text.append(part.path("text").asText());
                    }
                }
            }
            ObjectNode textBlock = json.objectNode();
            textBlock.put("type", "text");
            textBlock.put("text", text.toString());
            content.add(textBlock);
            return;
        }
        JsonNode searchResult = block.get("searchResult");
        if (searchResult != null && searchResult.isObject()) {
            ObjectNode mapped = json.objectNode();
            mapped.put("type", "search_result");
            mapped.put("source", searchResult.path("source").asText(""));
            mapped.put("title", searchResult.path("title").asText(""));
            ArrayNode mappedContent = json.arrayNode();
            JsonNode resultContent = searchResult.get("content");
            if (resultContent != null && resultContent.isArray()) {
                for (JsonNode part : resultContent) {
                    if (part.hasNonNull("text")) {
                        ObjectNode textPart = json.objectNode();
                        textPart.put("type", "text");
                        textPart.put("text", part.path("text").asText());
                        mappedContent.add(textPart);
                    }
                }
            }
            mapped.set("content", mappedContent);
            content.add(mapped);
        }
    }

    private JsonNode reasoningTextNode(JsonNode block) {
        return BedrockConverseContentSupport.reasoningTextNode(block);
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
        return BedrockConverseContentSupport.toClaudeStop(stopReason);
    }

    private String requiredBedrockStopReason(JsonNode source) {
        return BedrockConverseContentSupport.requiredStopReason(source);
    }

    private String mapBedrockStopToFinishReason(String stopReason) {
        return switch (stopReason) {
            case "end_turn", "stop_sequence" -> "stop";
            case "max_tokens" -> "length";
            case "tool_use" -> "tool_calls";
            case "content_filtered", "guardrail_intervened" -> "content_filter";
            case "malformed_model_output", "malformed_tool_use" ->
                    throw new ProtocolConversionException("BEDROCK_CONVERSE_INVALID_MODEL_OUTPUT: " + stopReason);
            default -> throw new ProtocolConversionException("BEDROCK_CONVERSE_UNSUPPORTED_STOP_REASON: " + stopReason);
        };
    }

    private String mapBedrockStopToResponsesStatus(String stopReason) {
        return BedrockConverseContentSupport.toResponsesStatus(stopReason);
    }
}
