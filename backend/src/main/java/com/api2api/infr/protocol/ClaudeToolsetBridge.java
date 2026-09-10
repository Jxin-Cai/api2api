package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Reconstructs dated Anthropic client toolsets as Responses function namespaces. */
final class ClaudeToolsetBridge {
    private static final JsonNode DEFINITIONS = loadDefinitions();
    private static final Set<String> ENTRY_FIELDS = Set.of("type", "configs", "cache_control", "allowed_callers");
    private static final Set<String> CONFIG_FIELDS = Set.of("enabled", "defer_loading");

    private ClaudeToolsetBridge() {}

    static Optional<ObjectNode> toNamespace(ProtocolJsonSupport json, JsonNode tool) {
        String name = switch (tool.path("type").asText()) {
            case "computer_toolset_20260801" -> "computer";
            case "browser_toolset_20260801" -> "browser";
            default -> null;
        };
        if (name == null) {
            return Optional.empty();
        }
        validateFields(tool, ENTRY_FIELDS);
        JsonNode callers = tool.path("allowed_callers");
        if (!callers.isMissingNode() && !callers.isNull()
                && (!callers.isArray() || callers.size() != 1 || !"direct".equals(callers.get(0).asText()))) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_REQUIRES_DIRECT_CALLER");
        }
        JsonNode definition = DEFINITIONS.path(name);
        JsonNode configs = tool.path("configs");
        if (!configs.isMissingNode() && !configs.isNull() && !configs.isObject()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_CONFIGS_MUST_BE_OBJECT");
        }
        Set<String> members = new HashSet<>();
        definition.path("members").forEach(member -> members.add(member.path("name").asText()));
        validateFields(configs, members);
        ObjectNode namespace = json.objectNode().put("type", "namespace").put("name", name);
        namespace.set("description", definition.path("description"));
        ArrayNode functions = namespace.putArray("tools");
        Boolean deferred = null;
        for (JsonNode member : definition.path("members")) {
            String memberName = member.path("name").asText();
            JsonNode config = configs.path(memberName);
            validateFields(config, CONFIG_FIELDS);
            for (String field : CONFIG_FIELDS) {
                if (config.hasNonNull(field) && !config.path(field).isBoolean()) {
                    throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_CONFIG_MUST_BE_BOOLEAN: " + field);
                }
            }
            if (!config.path("enabled").asBoolean(member.path("enabled").asBoolean(true))) {
                continue;
            }
            boolean memberDeferred = config.path("defer_loading").asBoolean(false);
            if (deferred != null && deferred != memberDeferred) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_MIXED_DEFER_LOADING");
            }
            deferred = memberDeferred;
            ObjectNode function = functions.addObject().put("type", "function").put("name", memberName);
            function.set("description", member.path("description"));
            function.set("parameters", member.path("parameters").deepCopy());
            // Optional fields retain their original semantics, e.g. click at the current cursor.
            function.put("strict", false);
            function.putArray("allowed_callers").add("direct");
            if (memberDeferred) {
                function.put("defer_loading", true);
            }
        }
        if (functions.isEmpty()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_HAS_NO_ENABLED_MEMBERS");
        }
        if (Boolean.TRUE.equals(deferred) && tool.hasNonNull("cache_control")) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_DEFERRED_TOOLSET_CACHE_NOT_SUPPORTED");
        }
        return Optional.of(namespace);
    }

    static boolean hasDeferredMembers(JsonNode namespace) {
        for (JsonNode member : namespace.path("tools")) {
            if (member.path("defer_loading").asBoolean(false)) {
                return true;
            }
        }
        return false;
    }

    static void validateNamespaceNames(ArrayNode tools) {
        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            if ("namespace".equals(tool.path("type").asText()) && !names.add(tool.path("name").asText())) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_DUPLICATE_TOOLSET");
            }
        }
        for (JsonNode tool : tools) {
            if (!"namespace".equals(tool.path("type").asText()) && names.contains(tool.path("name").asText())) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_NAME_CONFLICT");
            }
        }
    }

    private static void validateFields(JsonNode node, Set<String> allowed) {
        if (!node.isMissingNode() && !node.isNull() && !node.isObject()) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_TOOLSET_CONFIG_MUST_BE_OBJECT");
        }
        node.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw new ProtocolConversionException("CLAUDE_RESPONSES_UNSUPPORTED_TOOLSET_FIELD: " + field);
            }
        });
    }

    private static JsonNode loadDefinitions() {
        try (InputStream input = ClaudeToolsetBridge.class.getResourceAsStream(
                "/protocol/claude-client-toolsets-20260801.json")) {
            if (input == null) {
                throw new IllegalStateException("Claude client toolset definitions are missing");
            }
            return new ObjectMapper().readTree(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Claude client toolset definitions", exception);
        }
    }
}
