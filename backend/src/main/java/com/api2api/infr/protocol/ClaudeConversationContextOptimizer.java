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
final class ClaudeConversationContextOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeConversationContextOptimizer.class);
    private static final int DEFAULT_TOOL_CLEAR_TRIGGER_TOKENS = 100_000;
    private static final int DEFAULT_TOOL_USES_TO_KEEP = 3;
    private static final int REPEATED_SUCCESSFUL_TOOL_CALL_LIMIT = 3;
    private static final String CLEARED_TOOL_RESULT = "[Tool result cleared by context management]";
    private static final String CLEARED_THINKING = "[Thinking cleared by context management]";
    private static final String REPEATED_TOOL_CALL_WARNING =
            "Gateway safety notice: this exact tool call already succeeded twice. "
                    + "Do not repeat it; consume the result and choose the next distinct action.";

    private ClaudeConversationContextOptimizer() {
    }

    static JsonNode optimize(JsonNode messages, JsonNode contextManagement) {
        RepeatedToolCall repeatedToolCall = detectRepeatedSuccessfulToolCall(messages);
        failWhenRepeatedSuccessfulToolCallDetected(repeatedToolCall);
        if (messages == null || !messages.isArray()) {
            return messages;
        }
        ArrayNode optimized = repeatedToolCall.repetitions() == REPEATED_SUCCESSFUL_TOOL_CALL_LIMIT - 1
                ? ((ArrayNode) messages).deepCopy()
                : null;
        if (optimized != null) {
            appendRepeatedToolCallWarning(optimized);
        }
        if (contextManagement == null || contextManagement.isNull()) {
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
        if (!toolClearingConfigured) {
            applyDefaultToolResultSafetyPolicy(optimized);
        }
        return optimized;
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
        Map<String, String> fingerprintsById = new HashMap<>();
        List<String> successfulFingerprints = new ArrayList<>();
        for (JsonNode message : messages) {
            JsonNode content = message.path("content");
            if ("user".equals(message.path("role").asText("")) && containsNewUserInstruction(content)) {
                fingerprintsById.clear();
                successfulFingerprints.clear();
            }
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode block : content) {
                String type = block.path("type").asText("");
                if (isToolUse(type)) {
                    String toolName = block.path("name").asText("unknown");
                    fingerprintsById.put(block.path("id").asText(""), toolName + ":" + digest(block.path("input")));
                } else if (isToolResult(type)) {
                    String fingerprint = fingerprintsById.get(block.path("tool_use_id").asText(""));
                    if (fingerprint != null && !block.path("is_error").asBoolean(false)) {
                        successfulFingerprints.add(fingerprint);
                    } else {
                        successfulFingerprints.add("");
                    }
                }
            }
        }
        return repeatedTail(successfulFingerprints);
    }

    private static RepeatedToolCall repeatedTail(List<String> successfulFingerprints) {
        if (successfulFingerprints.isEmpty()) {
            return RepeatedToolCall.none();
        }
        String latest = successfulFingerprints.get(successfulFingerprints.size() - 1);
        int repeated = 0;
        for (int index = successfulFingerprints.size() - 1; index >= 0; index--) {
            if (!latest.equals(successfulFingerprints.get(index))) {
                break;
            }
            repeated++;
        }
        if (latest.isBlank()) {
            return RepeatedToolCall.none();
        }
        return new RepeatedToolCall(latest.substring(0, latest.indexOf(':')), repeated);
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

    private record ToolInteraction(ObjectNode toolUse, ObjectNode toolResult, String toolName) {
    }

    private record RepeatedToolCall(String toolName, int repetitions) {

        private static RepeatedToolCall none() {
            return new RepeatedToolCall("unknown", 0);
        }
    }
}
