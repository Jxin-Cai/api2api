package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Supplies the schemas Anthropic normally injects for its client-executed tools. */
final class ClaudeClientToolBridge {

    private static final Logger log = LoggerFactory.getLogger(ClaudeClientToolBridge.class);

    private enum ClientTool {
        BASH("bash"), LEGACY_EDITOR("str_replace_editor"),
        EDITOR("str_replace_based_edit_tool"), MEMORY("memory");

        private final String toolName;

        ClientTool(String toolName) {
            this.toolName = toolName;
        }
    }

    private ClaudeClientToolBridge() {}

    static Optional<ObjectNode> toCustomTool(ProtocolJsonSupport json, JsonNode tool) {
        ClientTool kind = switch (tool.path("type").asText("")) {
            case "bash_20241022", "bash_20250124" -> ClientTool.BASH;
            case "text_editor_20241022", "text_editor_20250124" -> ClientTool.LEGACY_EDITOR;
            case "text_editor_20250429", "text_editor_20250728" -> ClientTool.EDITOR;
            case "memory_20250818" -> ClientTool.MEMORY;
            default -> null;
        };
        if (kind == null) {
            return Optional.empty();
        }
        if (!kind.toolName.equals(tool.path("name").asText(""))) {
            throw new ProtocolConversionException("CLAUDE_RESPONSES_INVALID_CLIENT_TOOL_NAME");
        }
        ObjectNode mapped = tool.deepCopy();
        mapped.put("type", "custom");
        mapped.set("input_schema", kind == ClientTool.BASH ? bashSchema(json) : editorSchema(json, kind));
        mapped.put("description", description(kind, tool));
        // The native schemas have command-dependent optional fields. Responses strict
        // mode requires every property, which changes the client's input contract.
        // Keep the executable contract and explicitly report this approximation.
        if (tool.path("strict").asBoolean(false)) {
            log.warn("event=claude_responses_client_tool_strict_approximated toolKind={}", kind);
        }
        mapped.put("strict", false);
        return Optional.of(mapped);
    }

    private static ObjectNode bashSchema(ProtocolJsonSupport json) {
        return (ObjectNode) json.parse("""
                {"type":"object","properties":{
                  "command":{"type":"string","description":"Shell command to execute; required unless restart is true."},
                  "restart":{"type":"boolean","description":"Restart the persistent shell session."}
                },"additionalProperties":false}
                """, "Claude bash tool schema");
    }

    private static ObjectNode editorSchema(ProtocolJsonSupport json, ClientTool kind) {
        ObjectNode schema = (ObjectNode) json.parse("""
                {"type":"object","properties":{
                  "command":{"type":"string"},
                  "path":{"type":"string","description":"File or directory path. Required except for memory rename."},
                  "view_range":{"type":"array","items":{"type":"integer"},"minItems":2,"maxItems":2,
                    "description":"Optional view range [start, end], 1-based; end=-1 means end of file."},
                  "file_text":{"type":"string","description":"Complete file content, required for create."},
                  "old_str":{"type":"string","description":"Unique exact text to replace, required for str_replace."},
                  "new_str":{"type":"string","description":"Replacement text for str_replace; omit to delete old_str."},
                  "insert_line":{"type":"integer","minimum":0,"description":"Insert after this line; 0 means beginning of file."},
                  "insert_text":{"type":"string","description":"Text to insert, required for insert."}
                },"required":["command"],"additionalProperties":false}
                """, "Claude editor tool schema");
        ObjectNode properties = (ObjectNode) schema.path("properties");
        ArrayNode commands = (ArrayNode) json.valueToTree(List.of("view", "create", "str_replace", "insert"));
        if (kind == ClientTool.LEGACY_EDITOR) {
            commands.add("undo_edit");
        }
        if (kind == ClientTool.MEMORY) {
            commands.add("delete").add("rename");
            properties.set("old_path", json.objectNode().put("type", "string")
                    .put("description", "Existing path, required for rename instead of path."));
            properties.set("new_path", json.objectNode().put("type", "string")
                    .put("description", "Destination path, required for rename."));
        } else {
            ((ArrayNode) schema.path("required")).add("path");
        }
        ((ObjectNode) properties.path("command")).set("enum", commands);
        return schema;
    }

    private static String description(ClientTool kind, JsonNode tool) {
        String contract = switch (kind) {
            case BASH -> "Run shell commands in the client's persistent bash session. "
                    + "Use command to execute, or restart=true to reset the session. "
                    + "Calls share working directory and environment; run dependent commands in order.";
            case EDITOR, LEGACY_EDITOR -> "View and edit files in the client's filesystem. "
                    + "view lists directories or reads files; create requires file_text; "
                    + "str_replace requires a unique, exact old_str and replacement new_str; "
                    + "insert requires insert_line and insert_text. Read the file before editing."
                    + (kind == ClientTool.LEGACY_EDITOR ? " undo_edit reverts the last edit to path." : "");
            case MEMORY -> "Read and maintain persistent memory files in the client's /memories directory. "
                    + "Check /memories with view before starting work. Use path for view, create, "
                    + "str_replace, insert and delete; rename requires old_path and new_path instead. "
                    + "create requires file_text; str_replace requires unique exact old_str, with optional "
                    + "new_str (omitting it deletes the match); insert requires insert_line and insert_text. "
                    + "Keep all paths within /memories; never traverse outside it.";
        };
        if (tool.hasNonNull("max_characters")) {
            contract += " View output is limited to " + tool.path("max_characters").asInt()
                    + " characters; use view_range to page through longer files.";
        }
        String description = tool.path("description").asText("");
        return description.isBlank() ? contract : description + "\n\n" + contract;
    }
}
