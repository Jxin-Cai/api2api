package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;

/** Resolves conversation configuration for stateless, cross-provider requests. */
final class ConversationEffortBridge {

    private static final Set<String> CLAUDE_EFFORTS = Set.of("low", "medium", "high", "xhigh", "max");
    private static final Set<String> RESPONSES_EFFORTS = Set.of(
            "none", "minimal", "low", "medium", "high", "xhigh", "max");
    private static final Set<String> TOOL_RESULTS = Set.of("tool_result", "mcp_tool_result");

    private ConversationEffortBridge() {
    }

    /**
     * Claude changes take effect after fresh user input, never in the middle of a tool loop.
     * Resolve before context optimization, which can remove the activation boundary.
     * Folding into the request preserves the current effort, but not historical cache prefixes.
     */
    static JsonNode resolveClaudeRequest(JsonNode source) {
        JsonNode messages = source.path("messages");
        if (!messages.isArray()) {
            return source;
        }
        String activeEffort = null;
        String pendingEffort = null;
        boolean followsNewUserInput = false;
        for (JsonNode message : messages) {
            String role = message.path("role").asText("");
            if ("user".equals(role)) {
                followsNewUserInput = hasNewUserInput(message.path("content"));
                if (followsNewUserInput && pendingEffort != null) {
                    activeEffort = pendingEffort;
                    pendingEffort = null;
                }
            } else if ("assistant".equals(role)) {
                followsNewUserInput = false;
            }
            JsonNode config = message.get("output_config");
            if (config == null || config.isNull()) {
                continue;
            }
            if (!"system".equals(role)) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_MESSAGE_OUTPUT_CONFIG_REQUIRES_SYSTEM_ROLE");
            }
            validateFields(config, Set.of("effort"), "CLAUDE_RESPONSES_MESSAGE_OUTPUT_CONFIG");
            if ("next_user_message".equals(message.path("clear_at").asText())) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_MESSAGE_OUTPUT_CONFIG_REQUIRES_PERSISTENT_SYSTEM");
            }
            if (!config.hasNonNull("effort")) {
                continue;
            }
            String effort = requireEffort(config.get("effort"), CLAUDE_EFFORTS,
                    "CLAUDE_RESPONSES_INVALID_REASONING_EFFORT");
            if (followsNewUserInput) {
                activeEffort = effort;
            } else {
                pendingEffort = effort;
            }
        }
        if (activeEffort == null) {
            return source;
        }
        ObjectNode resolved = source.deepCopy();
        JsonNode config = resolved.get("output_config");
        if (config != null && !config.isNull() && !config.isObject()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_OUTPUT_CONFIG_MUST_BE_OBJECT");
        }
        ObjectNode outputConfig = config != null && config.isObject()
                ? (ObjectNode) config : resolved.putObject("output_config");
        outputConfig.put("effort", activeEffort);
        return resolved;
    }

    static JsonNode resolveResponsesReasoning(ProtocolJsonSupport json, JsonNode source) {
        JsonNode base = source.get("reasoning");
        ObjectNode resolved = base != null && base.isObject() ? base.deepCopy() : json.objectNode();
        JsonNode input = source.path("input");
        if (!input.isArray()) {
            return base;
        }
        boolean updated = false;
        for (JsonNode item : input) {
            if (!isResponsesUpdate(item)) {
                continue;
            }
            validateFields(item, Set.of("type", "id", "reasoning"), "RESPONSES_CLAUDE_CONFIGURATION_UPDATE");
            JsonNode reasoning = item.path("reasoning");
            validateFields(reasoning, Set.of("effort"), "RESPONSES_CLAUDE_CONFIGURATION_REASONING");
            resolved.put("effort", requireEffort(reasoning.path("effort"), RESPONSES_EFFORTS,
                    "RESPONSES_CLAUDE_REASONING_EFFORT_NOT_SUPPORTED"));
            updated = true;
        }
        return updated ? resolved : base;
    }

    static boolean isResponsesUpdate(JsonNode item) {
        return "configuration_update".equals(item.path("type").asText());
    }

    private static boolean hasNewUserInput(JsonNode content) {
        if (content.isTextual()) {
            return !content.asText().isEmpty();
        }
        if (!content.isArray()) {
            return false;
        }
        for (JsonNode block : content) {
            if (!TOOL_RESULTS.contains(block.path("type").asText())) {
                return true;
            }
        }
        return false;
    }

    private static String requireEffort(JsonNode effort, Set<String> supported, String error) {
        if (!effort.isTextual() || !supported.contains(effort.asText())) {
            throw new ProtocolConversionException(error);
        }
        return effort.asText();
    }

    private static void validateFields(JsonNode object, Set<String> supported, String error) {
        if (!object.isObject()) {
            throw new ProtocolConversionException(error + "_MUST_BE_OBJECT");
        }
        object.fieldNames().forEachRemaining(field -> {
            if (!supported.contains(field)) {
                throw new ProtocolConversionException(error + "_UNSUPPORTED_FIELD: " + field);
            }
        });
    }
}
