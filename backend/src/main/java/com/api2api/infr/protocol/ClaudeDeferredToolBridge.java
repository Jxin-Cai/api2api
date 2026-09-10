package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;

/** Makes client-discovered tools available even though Responses has no tool_reference content block. */
final class ClaudeDeferredToolBridge {

    private ClaudeDeferredToolBridge() {}

    static void activateReferencedTools(ArrayNode tools, JsonNode messages, JsonNode toolChoice) {
        Set<String> referenced = new HashSet<>();
        Set<String> referencedNamespaces = new HashSet<>();
        if (messages != null && messages.isArray()) {
            for (JsonNode message : messages) {
                for (JsonNode block : message.path("content")) {
                    if ("tool_use".equals(block.path("type").asText())) {
                        if (block.hasNonNull("toolset_name")) {
                            referencedNamespaces.add(block.path("toolset_name").asText());
                        } else {
                            referenced.add(block.path("name").asText());
                        }
                    } else if ("tool_result".equals(block.path("type").asText())
                            && !block.path("is_error").asBoolean(false)) {
                        for (JsonNode part : block.path("content")) {
                            if ("tool_reference".equals(part.path("type").asText())) {
                                referenced.add(part.path("tool_name").asText());
                            }
                        }
                    }
                }
            }
        }
        if (toolChoice != null && "tool".equals(toolChoice.path("type").asText())) {
            referenced.add(toolChoice.path("name").asText());
        }
        for (JsonNode tool : tools) {
            if ("function".equals(tool.path("type").asText())
                    && referenced.contains(tool.path("name").asText())) {
                ((ObjectNode) tool).remove("defer_loading");
            } else if ("namespace".equals(tool.path("type").asText())
                    && (referencedNamespaces.contains(tool.path("name").asText())
                        || referenced.contains(tool.path("name").asText()))) {
                eagerlyLoadTools((ArrayNode) tool.path("tools"));
            }
        }
    }

    static void eagerlyLoadTools(ArrayNode tools) {
        for (JsonNode tool : tools) {
            ((ObjectNode) tool).remove("defer_loading");
            if ("namespace".equals(tool.path("type").asText())) {
                eagerlyLoadTools((ArrayNode) tool.path("tools"));
            }
        }
    }

    static ObjectNode namedChoice(ProtocolJsonSupport json, String name, ArrayNode tools, JsonNode claudeTools) {
        for (JsonNode tool : tools) {
            if ("function".equals(tool.path("type").asText()) && name.equals(tool.path("name").asText())) {
                return json.objectNode().put("type", "function").put("name", name);
            }
        }
        String hostedType = hostedType(name, claudeTools);
        for (JsonNode tool : tools) {
            if (!hostedType.isEmpty() && hostedType.equals(tool.path("type").asText())) {
                ObjectNode choice = json.objectNode().put("type", "allowed_tools").put("mode", "required");
                choice.set("tools", json.arrayNode().add(json.objectNode().put("type", hostedType)));
                return choice;
            }
        }
        throw new ProtocolConversionException("CLAUDE_RESPONSES_NAMED_TOOL_CHOICE_NOT_AVAILABLE");
    }

    private static String hostedType(String name, JsonNode claudeTools) {
        if (claudeTools != null && claudeTools.isArray()) {
            for (JsonNode tool : claudeTools) {
                if (!name.equals(tool.path("name").asText())) {
                    continue;
                }
                String type = tool.path("type").asText("");
                if (type.startsWith("web_search")) {
                    return "web_search";
                }
                if (type.startsWith("code_execution")) {
                    return "code_interpreter";
                }
                if (type.startsWith("tool_search_tool")) {
                    return "tool_search";
                }
            }
        }
        return "";
    }
}
