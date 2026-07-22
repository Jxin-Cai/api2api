package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies Claude context-editing semantics that upstream protocols cannot execute themselves.
 */
public final class ClaudeConversationContextOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeConversationContextOptimizer.class);
    private static final int DEFAULT_TOOL_CLEAR_TRIGGER_TOKENS = 100_000;
    private static final int DEFAULT_TOOL_USES_TO_KEEP = 3;
    private static final int REPEATED_SUCCESSFUL_TOOL_CALL_LIMIT = 3;
    private static final int MINIMUM_TOOL_INTENT_LENGTH = 24;
    private static final double REPEATED_TOOL_INTENT_SIMILARITY = 0.78D;
    private static final String BASH_TOOL_NAME = "Bash";
    private static final String BASH_DESCRIPTION_FIELD = "description";
    private static final String CLEARED_TOOL_RESULT = "[Tool result cleared by context management]";
    private static final String CLEARED_THINKING = "[Thinking cleared by context management]";
    private static final String REPEATED_TOOL_CALL_WARNING =
            "Gateway safety notice: this tool operation already succeeded twice. "
                    + "Do not repeat it or merely rephrase its description; consume the result and execute "
                    + "the next distinct action required by the user.";

    private ClaudeConversationContextOptimizer() {
    }

    static JsonNode optimize(JsonNode messages, JsonNode contextManagement) {
        return optimize(messages, contextManagement, true);
    }

    static JsonNode applyRequestedContextManagement(JsonNode messages, JsonNode contextManagement) {
        return optimize(messages, contextManagement, false);
    }

    /**
     * Bedrock protects the thinking blocks from the assistant turn immediately preceding a tool-result
     * continuation. Those blocks must be replayed byte-for-byte, while thinking from completed turns may be
     * omitted and can be rejected if it is replayed as though it belonged to the latest assistant response.
     */
    static JsonNode retainThinkingForLatestToolContinuation(JsonNode messages) {
        if (messages == null || !messages.isArray() || messages.isEmpty()) {
            return messages;
        }

        Set<Integer> protectedAssistantIndexes = protectedAssistantIndexes(messages);
        ArrayNode normalized = null;
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"assistant".equals(message.path("role").asText(""))
                    || protectedAssistantIndexes.contains(index)
                    || !containsThinking(message.path("content"))) {
                continue;
            }
            if (normalized == null) {
                normalized = ((ArrayNode) messages).deepCopy();
            }
            ObjectNode assistantMessage = (ObjectNode) normalized.get(index);
            removeThinking(assistantMessage);
            if (assistantMessage.path("content").isArray() && assistantMessage.path("content").isEmpty()) {
                normalized.remove(index);
            }
        }
        return normalized == null ? messages : normalized;
    }

    private static Set<Integer> protectedAssistantIndexes(JsonNode messages) {
        int trailingUserIndex = messages.size() - 1;
        JsonNode trailingUser = messages.get(trailingUserIndex);
        if (!"user".equals(trailingUser.path("role").asText(""))
                || !containsOnlyToolResults(trailingUser.path("content"))) {
            return Set.of();
        }

        Set<Integer> protectedIndexes = new HashSet<>();
        for (int index = trailingUserIndex - 1; index >= 0; index--) {
            if (!"assistant".equals(messages.get(index).path("role").asText(""))) {
                break;
            }
            protectedIndexes.add(index);
        }
        return protectedIndexes;
    }

    private static boolean containsOnlyToolResults(JsonNode content) {
        if (!content.isArray() || content.isEmpty()) {
            return false;
        }
        for (JsonNode block : content) {
            String type = block.path("type").asText("");
            if (!"tool_result".equals(type) && !"mcp_tool_result".equals(type)) {
                return false;
            }
        }
        return true;
    }

    private static JsonNode optimize(
            JsonNode messages,
            JsonNode contextManagement,
            boolean applyGatewaySafetyPolicy
    ) {
        messages = sanitizeCompactionHistory(messages, contextManagement);
        JsonNode protectedMessages = applyGatewaySafetyPolicy
                ? protectAgainstRepeatedToolCalls(messages)
                : messages;
        if (messages == null || !messages.isArray()) {
            return messages;
        }
        ArrayNode optimized = protectedMessages == messages ? null : (ArrayNode) protectedMessages;
        if (contextManagement == null || contextManagement.isNull()) {
            if (!applyGatewaySafetyPolicy) {
                return messages;
            }
            if (optimized == null && estimateTokens(messages) <= DEFAULT_TOOL_CLEAR_TRIGGER_TOKENS) {
                return messages;
            }
            if (optimized == null) {
                optimized = ((ArrayNode) messages).deepCopy();
            }
            return applyDefaultToolResultSafetyPolicy(optimized);
        }
        JsonNode edits = contextManagement.isArray() ? contextManagement : contextManagement.path("edits");
        if (!edits.isArray()) {
            throw new ProtocolConversionException("CLAUDE_INVALID_CONTEXT_MANAGEMENT");
        }
        if (optimized == null) {
            optimized = ((ArrayNode) messages).deepCopy();
        }
        boolean toolClearingConfigured = false;
        for (JsonNode edit : edits) {
            String type = edit.path("type").asText("");
            switch (type) {
                case "clear_thinking_20251015" -> clearThinking(optimized, edit);
                case "clear_tool_uses_20250919" -> {
                    toolClearingConfigured = true;
                    clearToolUses(optimized, edit);
                }
                default -> {
                    // Compaction is handled by the target-specific converter.
                    if (!type.startsWith("compact_")) {
                        throw new ProtocolConversionException(
                                "CLAUDE_CONTEXT_MANAGEMENT_NOT_SUPPORTED: " + (type.isBlank() ? "unknown" : type));
                    }
                }
            }
        }
        if (applyGatewaySafetyPolicy && !toolClearingConfigured) {
            applyDefaultToolResultSafetyPolicy(optimized);
        }
        return optimized;
    }

    /**
     * Applies provider-independent protection against a model repeating a successful client tool operation.
     * The returned node is the original instance when no correction is needed and a copy when a warning is added.
     */
    public static JsonNode protectAgainstRepeatedToolCalls(JsonNode messages) {
        RepeatedToolCall repeatedToolCall = detectRepeatedSuccessfulToolCall(messages);
        failWhenRepeatedSuccessfulToolCallDetected(repeatedToolCall);
        if (messages == null || !messages.isArray()
                || repeatedToolCall.repetitions() != REPEATED_SUCCESSFUL_TOOL_CALL_LIMIT - 1) {
            return messages;
        }
        ArrayNode protectedMessages = ((ArrayNode) messages).deepCopy();
        appendRepeatedToolCallWarning(protectedMessages);
        return protectedMessages;
    }

    static boolean hasRepeatedToolCallWarning(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return false;
        }
        for (JsonNode message : messages) {
            JsonNode content = message.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText(""))
                        && block.path("text").asText("").contains(REPEATED_TOOL_CALL_WARNING)) {
                    return true;
                }
            }
        }
        return false;
    }

    static JsonNode sanitizeCompactionHistory(JsonNode messages, JsonNode contextManagement) {
        if (!hasCompactionEdit(contextManagement) || messages == null || !messages.isArray()) {
            return messages;
        }
        ArrayNode sanitized = null;
        for (int index = 0; index < messages.size(); index++) {
            JsonNode message = messages.get(index);
            if (!"assistant".equals(message.path("role").asText(""))
                    || !containsThinking(message.path("content"))) {
                continue;
            }
            if (sanitized == null) {
                sanitized = ((ArrayNode) messages).deepCopy();
            }
            stripThinking((ObjectNode) sanitized.get(index));
        }
        return sanitized == null ? messages : sanitized;
    }

    private static boolean hasCompactionEdit(JsonNode contextManagement) {
        if (contextManagement == null || contextManagement.isNull()) {
            return false;
        }
        JsonNode edits = contextManagement.isArray() ? contextManagement : contextManagement.path("edits");
        if (!edits.isArray()) {
            return false;
        }
        for (JsonNode edit : edits) {
            if (edit.path("type").asText("").startsWith("compact_")) {
                return true;
            }
        }
        return false;
    }

    private static ArrayNode applyDefaultToolResultSafetyPolicy(ArrayNode messages) {
        if (estimateTokens(messages) <= DEFAULT_TOOL_CLEAR_TRIGGER_TOKENS) {
            return messages;
        }
        ObjectNode policy = messages.objectNode();
        policy.put("type", "clear_tool_uses_20250919");
        policy.set("trigger", messages.objectNode()
                .put("type", "input_tokens")
                .put("value", DEFAULT_TOOL_CLEAR_TRIGGER_TOKENS));
        policy.set("keep", messages.objectNode()
                .put("type", "tool_uses")
                .put("value", DEFAULT_TOOL_USES_TO_KEEP));
        policy.set("exclude_tools", messages.arrayNode().add("Memory").add("memory"));
        clearToolUses(messages, policy);
        return messages;
    }

    private static void clearThinking(ArrayNode messages, JsonNode edit) {
        JsonNode keep = edit.get("keep");
        if (keep == null || keep.isNull() || (keep.isTextual() && "all".equals(keep.asText()))) {
            return;
        }
        if (!keep.isObject() || !"thinking_turns".equals(keep.path("type").asText(""))) {
            throw new ProtocolConversionException("CLAUDE_INVALID_CLEAR_THINKING_KEEP");
        }
        int keepTurns = keep.path("value").asInt(0);
        if (keepTurns <= 0) {
            throw new ProtocolConversionException("CLAUDE_INVALID_CLEAR_THINKING_KEEP");
        }
        List<Integer> thinkingMessageIndexes = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            JsonNode message = messages.get(index);
            if ("assistant".equals(message.path("role").asText("")) && containsThinking(message.path("content"))) {
                thinkingMessageIndexes.add(index);
            }
        }
        int clearBefore = Math.max(0, thinkingMessageIndexes.size() - keepTurns);
        for (int index = 0; index < clearBefore; index++) {
            stripThinking((ObjectNode) messages.get(thinkingMessageIndexes.get(index)));
        }
    }

    private static void clearToolUses(ArrayNode messages, JsonNode edit) {
        List<ToolInteraction> interactions = collectToolInteractions(messages);
        int triggerValue = edit.path("trigger").path("value").asInt(DEFAULT_TOOL_CLEAR_TRIGGER_TOKENS);
        String triggerType = edit.path("trigger").path("type").asText("input_tokens");
        boolean triggered = switch (triggerType) {
            case "input_tokens" -> estimateTokens(messages) > triggerValue;
            case "tool_uses" -> interactions.size() > triggerValue;
            default -> throw new ProtocolConversionException("CLAUDE_INVALID_CLEAR_TOOL_USES_TRIGGER");
        };
        if (!triggered) {
            return;
        }
        if (interactions.isEmpty()) {
            return;
        }

        int keep = edit.path("keep").path("value").asInt(DEFAULT_TOOL_USES_TO_KEEP);
        String keepType = edit.path("keep").path("type").asText("tool_uses");
        if (!"tool_uses".equals(keepType) || keep < 0) {
            throw new ProtocolConversionException("CLAUDE_INVALID_CLEAR_TOOL_USES_KEEP");
        }
        Set<String> excludedTools = new HashSet<>();
        JsonNode excludeTools = edit.path("exclude_tools");
        if (excludeTools.isArray()) {
            excludeTools.forEach(tool -> excludedTools.add(tool.asText("")));
        }
        int clearableEnd = Math.max(0, interactions.size() - keep);
        List<ToolInteraction> clearable = interactions.subList(0, clearableEnd).stream()
                .filter(interaction -> !excludedTools.contains(interaction.toolName()))
                .toList();
        int minimumTokens = edit.path("clear_at_least").path("value").asInt(0);
        if (estimatedClearableTokens(clearable) < minimumTokens) {
            return;
        }
        boolean clearToolInputs = edit.path("clear_tool_inputs").asBoolean(false);
        for (ToolInteraction interaction : clearable) {
            ObjectNode result = interaction.toolResult();
            result.put("content", CLEARED_TOOL_RESULT);
            if (clearToolInputs) {
                interaction.toolUse().set("input", interaction.toolUse().objectNode());
            }
        }
    }

    private static List<ToolInteraction> collectToolInteractions(ArrayNode messages) {
        Map<String, ToolUse> toolUses = new HashMap<>();
        List<ToolInteraction> interactions = new ArrayList<>();
        for (JsonNode message : messages) {
            JsonNode content = message.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                String type = block.path("type").asText("");
                if (isToolUse(type) && block.isObject()) {
                    toolUses.put(block.path("id").asText(""), new ToolUse((ObjectNode) block, block.path("name").asText("")));
                } else if (isToolResult(type) && block.isObject()) {
                    ToolUse toolUse = toolUses.get(block.path("tool_use_id").asText(""));
                    if (toolUse != null) {
                        interactions.add(new ToolInteraction(toolUse.block(), (ObjectNode) block, toolUse.name()));
                    }
                }
            }
        }
        return interactions;
    }

    private static RepeatedToolCall detectRepeatedSuccessfulToolCall(JsonNode messages) {
        if (messages == null || !messages.isArray()) {
            return RepeatedToolCall.none();
        }
        Map<String, SuccessfulToolCall> toolCallsById = new HashMap<>();
        List<SuccessfulToolCall> successfulToolCalls = new ArrayList<>();
        int assistantTurn = 0;
        for (JsonNode message : messages) {
            JsonNode content = message.path("content");
            if ("user".equals(message.path("role").asText("")) && containsNewUserInstruction(content)) {
                toolCallsById.clear();
                successfulToolCalls.clear();
            }
            if (!content.isArray()) {
                continue;
            }
            boolean assistantMessage = "assistant".equals(message.path("role").asText(""));
            if (assistantMessage) {
                assistantTurn++;
            }
            String assistantIntent = assistantMessage
                    ? assistantToolIntent(content)
                    : "";
            for (JsonNode block : content) {
                String type = block.path("type").asText("");
                if (isToolUse(type)) {
                    String toolName = block.path("name").asText("unknown");
                    toolCallsById.put(block.path("id").asText(""), new SuccessfulToolCall(
                            toolName,
                            toolName + ":" + digest(fingerprintInput(toolName, block)),
                            assistantIntent,
                            assistantTurn
                    ));
                } else if (isToolResult(type)) {
                    SuccessfulToolCall toolCall = toolCallsById.get(block.path("tool_use_id").asText(""));
                    if (toolCall != null && !block.path("is_error").asBoolean(false)) {
                        successfulToolCalls.add(toolCall);
                    } else {
                        successfulToolCalls.add(SuccessfulToolCall.none());
                    }
                }
            }
        }
        RepeatedToolCall exactRepeat = repeatedExactToolCallTail(successfulToolCalls);
        if (exactRepeat.repetitions() > 1) {
            return exactRepeat;
        }
        return repeatedToolIntentTail(successfulToolCalls);
    }

    private static JsonNode fingerprintInput(String toolName, JsonNode toolUse) {
        JsonNode input = toolUse.path("input");
        if (!BASH_TOOL_NAME.equals(toolName) || !input.isObject()) {
            return input;
        }
        ObjectNode executionInput = ((ObjectNode) input).deepCopy();
        executionInput.remove(BASH_DESCRIPTION_FIELD);
        return executionInput;
    }

    private static RepeatedToolCall repeatedExactToolCallTail(List<SuccessfulToolCall> successfulToolCalls) {
        if (successfulToolCalls.isEmpty()) {
            return RepeatedToolCall.none();
        }
        SuccessfulToolCall latest = successfulToolCalls.get(successfulToolCalls.size() - 1);
        int repeated = 0;
        for (int index = successfulToolCalls.size() - 1; index >= 0; index--) {
            if (!latest.fingerprint().equals(successfulToolCalls.get(index).fingerprint())) {
                break;
            }
            repeated++;
        }
        if (latest.fingerprint().isBlank()) {
            return RepeatedToolCall.none();
        }
        return new RepeatedToolCall(latest.toolName(), repeated);
    }

    private static RepeatedToolCall repeatedToolIntentTail(List<SuccessfulToolCall> successfulToolCalls) {
        if (successfulToolCalls.isEmpty()) {
            return RepeatedToolCall.none();
        }
        SuccessfulToolCall latest = successfulToolCalls.get(successfulToolCalls.size() - 1);
        if (latest.intent().length() < MINIMUM_TOOL_INTENT_LENGTH) {
            return RepeatedToolCall.none();
        }
        int repeated = 0;
        int lastCountedTurn = -1;
        for (int index = successfulToolCalls.size() - 1; index >= 0; index--) {
            SuccessfulToolCall candidate = successfulToolCalls.get(index);
            if (candidate.assistantTurn() == lastCountedTurn) {
                continue;
            }
            if (!latest.toolName().equals(candidate.toolName())
                    || toolIntentSimilarity(latest.intent(), candidate.intent()) < REPEATED_TOOL_INTENT_SIMILARITY) {
                break;
            }
            repeated++;
            lastCountedTurn = candidate.assistantTurn();
        }
        return new RepeatedToolCall(latest.toolName(), repeated);
    }

    private static String assistantToolIntent(JsonNode content) {
        StringBuilder intent = new StringBuilder();
        for (JsonNode block : content) {
            if (!"text".equals(block.path("type").asText(""))) {
                continue;
            }
            if (!intent.isEmpty()) {
                intent.append(' ');
            }
            intent.append(block.path("text").asText(""));
        }
        return normalizeToolIntent(intent.toString());
    }

    private static String normalizeToolIntent(String intent) {
        return intent.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}_]+", "")
                .strip();
    }

    private static double toolIntentSimilarity(String left, String right) {
        Set<String> leftTrigrams = characterNgrams(left, 3);
        Set<String> rightTrigrams = characterNgrams(right, 3);
        if (leftTrigrams.isEmpty() || rightTrigrams.isEmpty()) {
            return 0D;
        }
        long overlap = leftTrigrams.stream().filter(rightTrigrams::contains).count();
        return (double) overlap / Math.min(leftTrigrams.size(), rightTrigrams.size());
    }

    private static Set<String> characterNgrams(String value, int size) {
        Set<String> ngrams = new HashSet<>();
        for (int index = 0; index <= value.length() - size; index++) {
            ngrams.add(value.substring(index, index + size));
        }
        return ngrams;
    }

    private static void failWhenRepeatedSuccessfulToolCallDetected(RepeatedToolCall repeatedToolCall) {
        if (repeatedToolCall.repetitions() >= REPEATED_SUCCESSFUL_TOOL_CALL_LIMIT) {
            LOGGER.warn("Rejected repeated successful Claude tool call, toolName: {}, repetitions: {}",
                    repeatedToolCall.toolName(), repeatedToolCall.repetitions());
            throw new ProtocolConversionException(
                    "CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL: " + repeatedToolCall.toolName()
                            + " repeated " + repeatedToolCall.repetitions() + " times");
        }
    }

    private static void appendRepeatedToolCallWarning(ArrayNode messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            JsonNode message = messages.get(index);
            if (!"user".equals(message.path("role").asText("")) || !message.path("content").isArray()) {
                continue;
            }
            ((ArrayNode) message.path("content")).add(((ObjectNode) message).objectNode()
                    .put("type", "text")
                    .put("text", REPEATED_TOOL_CALL_WARNING));
            return;
        }
    }

    private static boolean containsNewUserInstruction(JsonNode content) {
        if (!content.isArray()) {
            return !content.asText("").isBlank();
        }
        for (JsonNode block : content) {
            if (!isToolResult(block.path("type").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsThinking(JsonNode content) {
        if (!content.isArray()) {
            return false;
        }
        for (JsonNode block : content) {
            if (isThinking(block.path("type").asText(""))) {
                return true;
            }
        }
        return false;
    }

    private static void stripThinking(ObjectNode message) {
        ArrayNode filtered = message.arrayNode();
        for (JsonNode block : message.path("content")) {
            if (!isThinking(block.path("type").asText(""))) {
                filtered.add(block);
            }
        }
        if (filtered.isEmpty()) {
            filtered.add(message.objectNode().put("type", "text").put("text", CLEARED_THINKING));
        }
        message.set("content", filtered);
    }

    private static void removeThinking(ObjectNode message) {
        ArrayNode filtered = message.arrayNode();
        for (JsonNode block : message.path("content")) {
            if (!isThinking(block.path("type").asText(""))) {
                filtered.add(block);
            }
        }
        message.set("content", filtered);
    }

    private static boolean isThinking(String type) {
        return "thinking".equals(type) || "reasoning".equals(type) || "redacted_thinking".equals(type);
    }

    private static boolean isToolUse(String type) {
        return "tool_use".equals(type) || "mcp_tool_use".equals(type) || "server_tool_use".equals(type);
    }

    private static boolean isToolResult(String type) {
        return type.endsWith("tool_result") || "tool_result".equals(type);
    }

    private static long estimateTokens(JsonNode node) {
        return node.toString().length() / 4L;
    }

    private static long estimatedClearableTokens(List<ToolInteraction> interactions) {
        return interactions.stream()
                .mapToLong(interaction -> Math.max(0, estimateTokens(interaction.toolResult().path("content"))
                        - CLEARED_TOOL_RESULT.length() / 4L))
                .sum();
    }

    private static String digest(JsonNode input) {
        try {
            byte[] bytes = input.toString().getBytes(StandardCharsets.UTF_8);
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private record ToolUse(ObjectNode block, String name) {
    }

    private record SuccessfulToolCall(String toolName, String fingerprint, String intent, int assistantTurn) {

        private static SuccessfulToolCall none() {
            return new SuccessfulToolCall("", "", "", -1);
        }
    }

    private record ToolInteraction(ObjectNode toolUse, ObjectNode toolResult, String toolName) {
    }

    private record RepeatedToolCall(String toolName, int repetitions) {

        private static RepeatedToolCall none() {
            return new RepeatedToolCall("unknown", 0);
        }
    }
}
