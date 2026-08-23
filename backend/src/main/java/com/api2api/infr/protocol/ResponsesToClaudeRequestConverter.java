package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Direct OpenAI Responses -> Claude Messages request conversion.
 *
 * <p>Replaces the previous lossy route through Chat Completions, preserving
 * structured input items (function calls, tool outputs, reasoning state,
 * compaction), tool definitions, tool choice, reasoning effort, output format
 * and metadata. Bridged Claude thinking state produced by
 * {@link ClaudeThinkingStateBridge} is restored to native signed thinking
 * blocks so multi-turn tool use keeps working against a Claude upstream.</p>
 *
 * <p>Message repair (tool pairing and consecutive-role merging) follows the
 * sub2api reference implementation: Responses clients such as codex re-send
 * the whole history each turn and freely interleave items between a
 * function_call and its output, which violates Anthropic's alternating-role
 * and tool_use/tool_result adjacency invariants.</p>
 */
final class ResponsesToClaudeRequestConverter {

    private static final int DEFAULT_MAX_TOKENS = 8192;
    private static final int MIN_THINKING_BUDGET_TOKENS = 1024;
    private static final String EMPTY_TOOL_RESULT = "(empty)";
    private static final String CLAUDE_WEB_SEARCH_TOOL_TYPE = "web_search_20250305";
    private static final String CLAUDE_CODE_EXECUTION_TOOL_TYPE = "code_execution_20250522";
    private static final String CLAUDE_CODE_EXECUTION_CALLER = "code_execution_20260521";

    /**
     * Provider-executed Responses history items with no Claude equivalent.
     * They are dropped instead of failing the whole conversation: the visible
     * conversation text already reflects their outcome, and Claude cannot
     * verify or replay another provider's hosted tool state.
     */
    private static final Set<String> HOSTED_HISTORY_ITEM_TYPES = Set.of(
            "web_search_call", "file_search_call", "code_interpreter_call",
            "image_generation_call", "local_shell_call", "computer_call",
            "computer_call_output", "mcp_call", "mcp_list_tools",
            "mcp_approval_request", "mcp_approval_response",
            "program", "program_output"
    );

    private final ProtocolJsonSupport json;

    ResponsesToClaudeRequestConverter(ProtocolJsonSupport json) {
        this.json = Objects.requireNonNull(json, "json must not be null");
    }

    ObjectNode convert(JsonNode source) {
        if (source == null || !source.isObject()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_REQUEST_MUST_BE_OBJECT");
        }
        validateStatefulFields(source);
        ObjectNode target = json.objectNode();
        copyIfPresent(source, target, "model");
        copyIfPresent(source, target, "stream");
        copyIfPresent(source, target, "temperature");
        copyIfPresent(source, target, "top_p");
        int maxTokens = resolveMaxTokens(source);
        target.put("max_tokens", maxTokens);

        List<String> systemParts = new ArrayList<>();
        String instructions = source.path("instructions").asText("");
        if (!instructions.isBlank()) {
            systemParts.add(instructions.strip());
        }
        List<ObjectNode> messages = responsesInputToClaudeMessages(source.get("input"), systemParts);
        messages = mergeConsecutiveMessages(messages);
        messages = normalizeToolPairing(messages);
        messages = mergeConsecutiveMessages(messages);
        ArrayNode mappedMessages = json.arrayNode();
        messages.forEach(mappedMessages::add);
        target.set("messages", mappedMessages);
        if (!systemParts.isEmpty()) {
            target.put("system", String.join("\n\n", systemParts));
        }

        applyTools(source.get("tools"), target);
        applyToolChoice(source, target);
        applyReasoning(source.get("reasoning"), target, maxTokens);
        applyTextFormat(source.get("text"), target);
        applyMetadata(source, target);
        applyServiceTier(source, target);
        return target;
    }

    private void validateStatefulFields(JsonNode source) {
        if (source.hasNonNull("previous_response_id")) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_PREVIOUS_RESPONSE_ID_NOT_SUPPORTED");
        }
        if (source.hasNonNull("conversation")) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_CONVERSATION_STATE_NOT_SUPPORTED");
        }
        if (source.hasNonNull("prompt")) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_STORED_PROMPT_NOT_SUPPORTED");
        }
        if (source.path("background").asBoolean(false)) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_BACKGROUND_MODE_NOT_SUPPORTED");
        }
    }

    private int resolveMaxTokens(JsonNode source) {
        // Claude requires max_tokens while Responses treats it as optional.
        JsonNode maxOutputTokens = source.get("max_output_tokens");
        if (maxOutputTokens != null && maxOutputTokens.canConvertToInt() && maxOutputTokens.asInt() > 0) {
            return maxOutputTokens.asInt();
        }
        return DEFAULT_MAX_TOKENS;
    }

    // ---- input -> messages ----

    private List<ObjectNode> responsesInputToClaudeMessages(JsonNode input, List<String> systemParts) {
        List<ObjectNode> messages = new ArrayList<>();
        if (input == null || input.isNull() || input.isMissingNode()) {
            return messages;
        }
        if (input.isTextual()) {
            ArrayNode content = json.arrayNode();
            addTextBlock(content, input.asText(""));
            appendMessage(messages, "user", content);
            return messages;
        }
        if (!input.isArray()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_INPUT_MUST_BE_TEXT_OR_ARRAY");
        }
        for (JsonNode item : input) {
            String type = item.path("type").asText("");
            String role = item.path("role").asText("");
            if ("system".equals(role) || "developer".equals(role)) {
                String text = extractTextContent(item.get("content"));
                if (!text.isBlank()) {
                    systemParts.add(text);
                }
            } else if ("function_call".equals(type) || "custom_tool_call".equals(type)) {
                appendMessage(messages, "assistant", toolCallToClaudeContent(item));
            } else if ("function_call_output".equals(type) || "custom_tool_call_output".equals(type)) {
                appendMessage(messages, "user", toolOutputToClaudeContent(item, type));
            } else if ("reasoning".equals(type)) {
                reasoningItemToClaudeContent(item).ifPresent(
                        content -> appendMessage(messages, "assistant", content));
            } else if (ResponsesProtocolConstants.isCompactionType(type)) {
                appendMessage(messages, "assistant", compactionItemToClaudeContent(item));
            } else if ("item_reference".equals(type)) {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_ITEM_REFERENCE_NOT_SUPPORTED");
            } else if (HOSTED_HISTORY_ITEM_TYPES.contains(type)) {
                // Dropped: see HOSTED_HISTORY_ITEM_TYPES.
            } else if ("assistant".equals(role)) {
                ArrayNode content = assistantContentToClaude(item.get("content"));
                if (!isBlankTextOnly(content)) {
                    appendMessage(messages, "assistant", content);
                }
            } else if ("user".equals(role) || "message".equals(type) || type.isBlank()) {
                ArrayNode content = userContentToClaude(item.get("content"));
                if (!content.isEmpty()) {
                    appendMessage(messages, "user", content);
                }
            } else {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_UNSUPPORTED_INPUT_ITEM: " + type);
            }
        }
        return messages;
    }

    private void appendMessage(List<ObjectNode> messages, String role, ArrayNode content) {
        if (content.isEmpty()) {
            return;
        }
        ObjectNode message = json.objectNode();
        message.put("role", role);
        message.set("content", content);
        messages.add(message);
    }

    private ArrayNode toolCallToClaudeContent(JsonNode item) {
        ArrayNode content = json.arrayNode();
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
        return content;
    }

    private ArrayNode toolOutputToClaudeContent(JsonNode item, String type) {
        boolean custom = "custom_tool_call_output".equals(type);
        ObjectNode callItem = json.objectNode();
        callItem.put("type", custom ? "custom_tool_call" : "function_call");
        callItem.put("call_id", item.path("call_id").asText(item.path("id").asText("")));
        ObjectNode toolResult = json.objectNode();
        toolResult.put("type", "tool_result");
        toolResult.put("tool_use_id", ResponsesToolCallBridge.toClaudeToolUseId(callItem));
        JsonNode output = item.get("output");
        if (output == null || output.isNull() || output.isTextual()) {
            String text = output == null ? "" : output.asText("");
            toolResult.put("content", text.isBlank() ? EMPTY_TOOL_RESULT : text);
        } else if (output.isArray()) {
            ArrayNode blocks = json.arrayNode();
            for (JsonNode part : output) {
                String partType = part.path("type").asText("");
                switch (partType) {
                    case "input_text", "output_text", "text" -> addTextBlock(blocks, part.path("text").asText(""));
                    case "input_image" -> blocks.add(imagePartToClaude(part));
                    default -> throw new ProtocolConversionException(
                            "RESPONSES_CLAUDE_UNSUPPORTED_TOOL_OUTPUT_PART: " + partType);
                }
            }
            if (blocks.isEmpty()) {
                toolResult.put("content", EMPTY_TOOL_RESULT);
            } else {
                toolResult.set("content", blocks);
            }
        } else {
            toolResult.put("content", output.toString());
        }
        ArrayNode content = json.arrayNode();
        content.add(toolResult);
        return content;
    }

    private java.util.Optional<ArrayNode> reasoningItemToClaudeContent(JsonNode item) {
        String encryptedContent = item.path("encrypted_content").asText("");
        JsonNode bridgedBlock = ClaudeThinkingStateBridge.decode(json.objectMapper(), encryptedContent)
                .orElse(null);
        if (bridgedBlock == null) {
            // Foreign OpenAI reasoning state cannot be replayed to Claude: thinking
            // blocks require an Anthropic-issued signature, which cannot be forged.
            return java.util.Optional.empty();
        }
        ArrayNode content = json.arrayNode();
        content.add(bridgedBlock);
        return java.util.Optional.of(content);
    }

    private ArrayNode compactionItemToClaudeContent(JsonNode item) {
        String summaryText = firstSummaryText(item.get("summary"));
        if (summaryText.isBlank()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_COMPACTION_STATE_NOT_REPLAYABLE");
        }
        ObjectNode compaction = json.objectNode();
        compaction.put("type", "compaction");
        compaction.put("content", summaryText);
        ArrayNode content = json.arrayNode();
        content.add(compaction);
        return content;
    }

    private String firstSummaryText(JsonNode summary) {
        if (summary == null || !summary.isArray()) {
            return "";
        }
        for (JsonNode part : summary) {
            String text = part.path("text").asText("");
            if ("summary_text".equals(part.path("type").asText("")) && !text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String extractTextContent(JsonNode content) {
        if (content == null || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText("");
        }
        if (!content.isArray()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            if ("input_text".equals(type) || "output_text".equals(type) || "text".equals(type)) {
                String value = part.path("text").asText("");
                if (value.isBlank()) {
                    continue;
                }
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append(value);
            }
        }
        return text.toString();
    }

    private ArrayNode userContentToClaude(JsonNode content) {
        ArrayNode blocks = json.arrayNode();
        if (content == null || content.isNull()) {
            return blocks;
        }
        if (content.isTextual()) {
            addTextBlock(blocks, content.asText(""));
            return blocks;
        }
        if (!content.isArray()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_MESSAGE_CONTENT_MUST_BE_TEXT_OR_ARRAY");
        }
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            switch (type) {
                case "input_text", "text" -> addTextBlock(blocks, part.path("text").asText(""));
                case "input_image" -> blocks.add(imagePartToClaude(part));
                case "input_file" -> blocks.add(filePartToClaude(part));
                default -> throw new ProtocolConversionException(
                        "RESPONSES_CLAUDE_UNSUPPORTED_CONTENT_PART: " + type);
            }
        }
        return blocks;
    }

    private ArrayNode assistantContentToClaude(JsonNode content) {
        ArrayNode blocks = json.arrayNode();
        if (content == null || content.isNull()) {
            return blocks;
        }
        if (content.isTextual()) {
            addTextBlock(blocks, content.asText(""));
            return blocks;
        }
        if (!content.isArray()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_MESSAGE_CONTENT_MUST_BE_TEXT_OR_ARRAY");
        }
        for (JsonNode part : content) {
            String type = part.path("type").asText("");
            switch (type) {
                case "output_text", "text" -> addTextBlock(blocks, part.path("text").asText(""));
                case "refusal" -> addTextBlock(blocks, part.path("refusal").asText(""));
                default -> throw new ProtocolConversionException(
                        "RESPONSES_CLAUDE_UNSUPPORTED_CONTENT_PART: " + type);
            }
        }
        return blocks;
    }

    private void addTextBlock(ArrayNode blocks, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        ObjectNode block = json.objectNode();
        block.put("type", "text");
        block.put("text", text);
        blocks.add(block);
    }

    private ObjectNode imagePartToClaude(JsonNode part) {
        String imageUrl = part.path("image_url").asText("");
        if (imageUrl.isBlank()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_IMAGE_URL_REQUIRED");
        }
        ObjectNode image = json.objectNode();
        image.put("type", "image");
        ObjectNode source = json.objectNode();
        if (imageUrl.startsWith("data:")) {
            int separator = imageUrl.indexOf(";base64,");
            if (separator < 0) {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_IMAGE_DATA_URI_INVALID");
            }
            source.put("type", "base64");
            source.put("media_type", imageUrl.substring("data:".length(), separator));
            source.put("data", imageUrl.substring(separator + ";base64,".length()));
        } else {
            source.put("type", "url");
            source.put("url", imageUrl);
        }
        image.set("source", source);
        return image;
    }

    private ObjectNode filePartToClaude(JsonNode part) {
        ObjectNode document = json.objectNode();
        document.put("type", "document");
        ObjectNode source = json.objectNode();
        if (part.hasNonNull("file_id")) {
            source.put("type", "file");
            source.put("file_id", part.path("file_id").asText(""));
        } else if (part.hasNonNull("file_url")) {
            source.put("type", "url");
            source.put("url", part.path("file_url").asText(""));
        } else if (part.hasNonNull("file_data")) {
            // Responses input_file carries no MIME type; Claude base64 documents
            // only accept PDF, which is also the Responses file-upload default.
            source.put("type", "base64");
            source.put("media_type", "application/pdf");
            source.put("data", part.path("file_data").asText(""));
        } else {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_FILE_SOURCE_REQUIRED");
        }
        document.set("source", source);
        if (part.hasNonNull("filename")) {
            document.put("title", part.path("filename").asText(""));
        }
        return document;
    }

    private boolean isBlankTextOnly(ArrayNode content) {
        if (content.isEmpty()) {
            return true;
        }
        for (JsonNode block : content) {
            if (!"text".equals(block.path("type").asText(""))
                    || !block.path("text").asText("").isBlank()) {
                return false;
            }
        }
        return true;
    }

    // ---- message repair (tool pairing + role alternation) ----

    /**
     * Rebuilds the message sequence so it satisfies Anthropic's
     * tool_use/tool_result invariants: every tool_use must be answered by a
     * tool_result in the immediately following user message, and orphan
     * results or unanswered calls are dropped.
     */
    private List<ObjectNode> normalizeToolPairing(List<ObjectNode> messages) {
        Map<String, JsonNode> resultsByToolUseId = new LinkedHashMap<>();
        for (ObjectNode message : messages) {
            if (!"user".equals(message.path("role").asText(""))) {
                continue;
            }
            for (JsonNode block : message.path("content")) {
                String toolUseId = block.path("tool_use_id").asText("");
                if ("tool_result".equals(block.path("type").asText("")) && !toolUseId.isBlank()) {
                    resultsByToolUseId.put(toolUseId, block.deepCopy());
                }
            }
        }

        List<ObjectNode> repaired = new ArrayList<>(messages.size());
        for (ObjectNode message : messages) {
            String role = message.path("role").asText("");
            if ("assistant".equals(role)) {
                repairAssistantMessage(message, resultsByToolUseId, repaired);
            } else if ("user".equals(role)) {
                repairUserMessage(message, repaired);
            } else {
                repaired.add(message);
            }
        }
        return repaired;
    }

    private void repairAssistantMessage(
            ObjectNode message,
            Map<String, JsonNode> resultsByToolUseId,
            List<ObjectNode> repaired
    ) {
        ArrayNode toolUses = json.arrayNode();
        ArrayNode others = json.arrayNode();
        for (JsonNode block : message.path("content")) {
            if ("tool_use".equals(block.path("type").asText(""))) {
                toolUses.add(block.deepCopy());
            } else {
                others.add(block.deepCopy());
            }
        }
        if (toolUses.isEmpty()) {
            repaired.add(message);
            return;
        }
        ArrayNode answeredCalls = json.arrayNode();
        ArrayNode pairedResults = json.arrayNode();
        for (JsonNode toolUse : toolUses) {
            JsonNode result = resultsByToolUseId.get(toolUse.path("id").asText(""));
            if (result != null) {
                answeredCalls.add(toolUse);
                pairedResults.add(result.deepCopy());
            }
        }
        if (answeredCalls.isEmpty()) {
            appendRepairedMessage(repaired, "assistant", others);
            return;
        }
        ArrayNode assistantBlocks = json.arrayNode();
        assistantBlocks.addAll(others);
        assistantBlocks.addAll(answeredCalls);
        appendRepairedMessage(repaired, "assistant", assistantBlocks);
        appendRepairedMessage(repaired, "user", pairedResults);
    }

    private void repairUserMessage(ObjectNode message, List<ObjectNode> repaired) {
        ArrayNode nonResultBlocks = json.arrayNode();
        boolean hasResult = false;
        for (JsonNode block : message.path("content")) {
            if ("tool_result".equals(block.path("type").asText(""))) {
                hasResult = true;
            } else {
                nonResultBlocks.add(block.deepCopy());
            }
        }
        if (!hasResult) {
            repaired.add(message);
            return;
        }
        // tool_result blocks are re-emitted adjacent to their call by the
        // assistant repair pass; keep only the remaining user content in place.
        appendRepairedMessage(repaired, "user", nonResultBlocks);
    }

    private void appendRepairedMessage(List<ObjectNode> repaired, String role, ArrayNode content) {
        if (content.isEmpty()) {
            return;
        }
        ObjectNode message = json.objectNode();
        message.put("role", role);
        message.set("content", content);
        repaired.add(message);
    }

    /** Anthropic requires alternating roles; merge consecutive same-role turns. */
    private List<ObjectNode> mergeConsecutiveMessages(List<ObjectNode> messages) {
        List<ObjectNode> merged = new ArrayList<>(messages.size());
        for (ObjectNode message : messages) {
            if (merged.isEmpty()
                    || !merged.get(merged.size() - 1).path("role").asText("")
                    .equals(message.path("role").asText(""))) {
                merged.add(message);
                continue;
            }
            ObjectNode last = merged.get(merged.size() - 1);
            ((ArrayNode) last.path("content")).addAll((ArrayNode) message.path("content"));
        }
        return merged;
    }

    // ---- tools / tool_choice ----

    private void applyTools(JsonNode tools, ObjectNode target) {
        if (tools == null || !tools.isArray() || tools.isEmpty()) {
            return;
        }
        ArrayNode mappedTools = json.arrayNode();
        ArrayNode mcpServers = json.arrayNode();
        for (JsonNode tool : tools) {
            String type = tool.path("type").asText("function");
            if ("function".equals(type) || "custom".equals(type)) {
                mappedTools.add(functionToolToClaude(tool, "custom".equals(type)));
            } else if (type.startsWith("web_search")) {
                mappedTools.add(webSearchToolToClaude(tool));
            } else if ("code_interpreter".equals(type)) {
                mappedTools.add(codeInterpreterToolToClaude(tool, target));
            } else if ("mcp".equals(type)) {
                mcpToolToClaude(tool, mappedTools, mcpServers);
            } else if ("tool_search".equals(type) || "programmatic_tool_calling".equals(type)) {
                // Capability markers emitted by the reverse direction; the Claude
                // equivalents are expressed as per-tool attributes instead.
            } else {
                throw new ProtocolConversionException("RESPONSES_CLAUDE_TOOL_TYPE_NOT_SUPPORTED: " + type);
            }
        }
        if (!mappedTools.isEmpty()) {
            target.set("tools", mappedTools);
        }
        if (!mcpServers.isEmpty()) {
            target.set("mcp_servers", mcpServers);
        }
    }

    private ObjectNode functionToolToClaude(JsonNode tool, boolean custom) {
        String name = tool.path("name").asText("");
        if (name.isBlank()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_TOOL_NAME_REQUIRED");
        }
        ObjectNode mapped = json.objectNode();
        mapped.put("name", name);
        if (tool.hasNonNull("description")) {
            mapped.put("description", tool.path("description").asText(""));
        }
        mapped.set("input_schema", custom
                ? customToolInputSchema()
                : normalizeInputSchema(tool.get("parameters")));
        if (tool.hasNonNull("strict")) {
            mapped.put("strict", tool.path("strict").asBoolean());
        }
        if (tool.path("defer_loading").asBoolean(false)) {
            mapped.put("defer_loading", true);
        }
        ArrayNode allowedCallers = allowedCallersToClaude(tool.get("allowed_callers"));
        if (allowedCallers != null) {
            mapped.set("allowed_callers", allowedCallers);
        }
        return mapped;
    }

    /**
     * Responses custom tools accept free-form text; the tool-call bridge wraps
     * that text as {@code {"input": "..."}}, so the Claude tool declares the
     * matching single-field schema.
     */
    private ObjectNode customToolInputSchema() {
        ObjectNode schema = json.objectNode();
        schema.put("type", "object");
        ObjectNode properties = json.objectNode();
        ObjectNode inputProperty = json.objectNode();
        inputProperty.put("type", "string");
        properties.set("input", inputProperty);
        schema.set("properties", properties);
        ArrayNode required = json.arrayNode();
        required.add("input");
        schema.set("required", required);
        return schema;
    }

    private ObjectNode normalizeInputSchema(JsonNode parameters) {
        ObjectNode schema = parameters == null || !parameters.isObject()
                ? json.objectNode()
                : (ObjectNode) parameters.deepCopy();
        if (!schema.has("type")) {
            schema.put("type", "object");
        }
        if (!schema.has("properties")) {
            schema.set("properties", json.objectNode());
        }
        return schema;
    }

    private ArrayNode allowedCallersToClaude(JsonNode allowedCallers) {
        if (allowedCallers == null || !allowedCallers.isArray() || allowedCallers.isEmpty()) {
            return null;
        }
        ArrayNode mapped = json.arrayNode();
        for (JsonNode caller : allowedCallers) {
            String value = caller.asText("");
            switch (value) {
                case "direct" -> mapped.add("direct");
                case "programmatic" -> mapped.add(CLAUDE_CODE_EXECUTION_CALLER);
                default -> throw new ProtocolConversionException(
                        "RESPONSES_CLAUDE_UNSUPPORTED_ALLOWED_CALLER: " + value);
            }
        }
        return mapped;
    }

    private ObjectNode webSearchToolToClaude(JsonNode tool) {
        ObjectNode mapped = json.objectNode();
        mapped.put("type", CLAUDE_WEB_SEARCH_TOOL_TYPE);
        mapped.put("name", "web_search");
        JsonNode allowedDomains = tool.path("filters").path("allowed_domains");
        if (allowedDomains.isArray() && !allowedDomains.isEmpty()) {
            mapped.set("allowed_domains", allowedDomains.deepCopy());
        }
        if (tool.path("user_location").isObject()) {
            mapped.set("user_location", tool.path("user_location").deepCopy());
        }
        return mapped;
    }

    private ObjectNode codeInterpreterToolToClaude(JsonNode tool, ObjectNode target) {
        ObjectNode mapped = json.objectNode();
        mapped.put("type", CLAUDE_CODE_EXECUTION_TOOL_TYPE);
        mapped.put("name", "code_execution");
        JsonNode container = tool.get("container");
        if (container != null && container.isTextual() && !container.asText().isBlank()) {
            target.put("container", container.asText());
        }
        return mapped;
    }

    private void mcpToolToClaude(JsonNode tool, ArrayNode mappedTools, ArrayNode mcpServers) {
        String label = tool.path("server_label").asText("");
        String url = tool.path("server_url").asText("");
        if (url.isBlank()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_MCP_SERVER_URL_REQUIRED: " + label);
        }
        ObjectNode server = json.objectNode();
        server.put("type", "url");
        server.put("name", label.isBlank() ? "mcp" : label);
        server.put("url", url);
        if (tool.hasNonNull("authorization")) {
            server.put("authorization_token", tool.path("authorization").asText());
        }
        mcpServers.add(server);
        JsonNode allowedTools = tool.get("allowed_tools");
        boolean deferred = tool.path("defer_loading").asBoolean(false);
        if ((allowedTools != null && allowedTools.isArray()) || deferred) {
            ObjectNode toolset = json.objectNode();
            toolset.put("type", "mcp_toolset");
            toolset.put("mcp_server_name", server.path("name").asText());
            if (deferred) {
                toolset.put("defer_loading", true);
            }
            if (allowedTools != null && allowedTools.isArray()) {
                ObjectNode defaultConfig = json.objectNode();
                defaultConfig.put("enabled", false);
                toolset.set("default_config", defaultConfig);
                ObjectNode configs = json.objectNode();
                for (JsonNode allowed : allowedTools) {
                    ObjectNode config = json.objectNode();
                    config.put("enabled", true);
                    configs.set(allowed.asText(""), config);
                }
                toolset.set("configs", configs);
            }
            mappedTools.add(toolset);
        }
    }

    private void applyToolChoice(JsonNode source, ObjectNode target) {
        JsonNode toolChoice = source.get("tool_choice");
        boolean parallelDisabled = source.hasNonNull("parallel_tool_calls")
                && !source.path("parallel_tool_calls").asBoolean(true);
        ObjectNode mapped = null;
        if (toolChoice != null && !toolChoice.isNull()) {
            mapped = json.objectNode();
            if (toolChoice.isTextual()) {
                mapped.put("type", switch (toolChoice.asText("auto")) {
                    case "required" -> "any";
                    case "none" -> "none";
                    default -> "auto";
                });
            } else {
                String type = toolChoice.path("type").asText("");
                if ("function".equals(type) || "custom".equals(type)) {
                    String name = toolChoice.path("name").asText(
                            toolChoice.path("function").path("name").asText(""));
                    if (name.isBlank()) {
                        throw new ProtocolConversionException("RESPONSES_CLAUDE_TOOL_CHOICE_NAME_REQUIRED");
                    }
                    mapped.put("type", "tool");
                    mapped.put("name", name);
                } else {
                    throw new ProtocolConversionException(
                            "RESPONSES_CLAUDE_TOOL_CHOICE_NOT_SUPPORTED: " + type);
                }
            }
        }
        if (parallelDisabled) {
            if (mapped == null) {
                mapped = json.objectNode();
                mapped.put("type", "auto");
            }
            mapped.put("disable_parallel_tool_use", true);
        }
        if (mapped != null && !mapped.isEmpty()) {
            target.set("tool_choice", mapped);
        }
    }

    // ---- reasoning / output format / metadata ----

    private void applyReasoning(JsonNode reasoning, ObjectNode target, int maxTokens) {
        if (reasoning == null || !reasoning.isObject()) {
            return;
        }
        String effort = reasoning.path("effort").asText("");
        if (effort.isBlank()) {
            return;
        }
        if ("none".equals(effort) || "minimal".equals(effort)) {
            ObjectNode thinking = json.objectNode();
            thinking.put("type", "disabled");
            target.set("thinking", thinking);
            return;
        }
        String claudeEffort = switch (effort) {
            case "low" -> "low";
            case "medium" -> "medium";
            case "high" -> "high";
            case "xhigh", "max" -> "max";
            default -> throw new ProtocolConversionException(
                    "RESPONSES_CLAUDE_REASONING_EFFORT_NOT_SUPPORTED: " + effort);
        };
        ObjectNode outputConfig = target.hasNonNull("output_config")
                ? (ObjectNode) target.get("output_config")
                : json.objectNode();
        outputConfig.put("effort", claudeEffort);
        target.set("output_config", outputConfig);
        int budget = Math.min(thinkingBudgetForEffort(claudeEffort), maxTokens - 1);
        if (budget >= MIN_THINKING_BUDGET_TOKENS) {
            ObjectNode thinking = json.objectNode();
            thinking.put("type", "enabled");
            thinking.put("budget_tokens", budget);
            target.set("thinking", thinking);
        }
    }

    private int thinkingBudgetForEffort(String claudeEffort) {
        return switch (claudeEffort) {
            case "low" -> 1024;
            case "medium" -> 4096;
            case "high" -> 10240;
            default -> 32768;
        };
    }

    private void applyTextFormat(JsonNode text, ObjectNode target) {
        if (text == null || !text.isObject()) {
            return;
        }
        JsonNode format = text.get("format");
        if (format == null || !format.isObject()) {
            return;
        }
        String type = format.path("type").asText("");
        if ("text".equals(type) || type.isBlank()) {
            return;
        }
        if (!"json_schema".equals(type)) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_TEXT_FORMAT_NOT_SUPPORTED: " + type);
        }
        JsonNode schema = format.get("schema");
        if (schema == null || !schema.isObject()) {
            throw new ProtocolConversionException("RESPONSES_CLAUDE_TEXT_FORMAT_SCHEMA_REQUIRED");
        }
        ObjectNode mappedFormat = json.objectNode();
        mappedFormat.put("type", "json_schema");
        mappedFormat.set("schema", schema.deepCopy());
        ObjectNode outputConfig = target.hasNonNull("output_config")
                ? (ObjectNode) target.get("output_config")
                : json.objectNode();
        outputConfig.set("format", mappedFormat);
        target.set("output_config", outputConfig);
    }

    private void applyMetadata(JsonNode source, ObjectNode target) {
        String userId = source.path("metadata").path("user_id").asText("");
        if (userId.isBlank()) {
            userId = source.path("safety_identifier").asText("");
        }
        if (userId.isBlank()) {
            userId = source.path("user").asText("");
        }
        if (!userId.isBlank()) {
            ObjectNode metadata = json.objectNode();
            metadata.put("user_id", userId);
            target.set("metadata", metadata);
        }
    }

    private void applyServiceTier(JsonNode source, ObjectNode target) {
        if (!source.hasNonNull("service_tier")) {
            return;
        }
        String tier = source.path("service_tier").asText("");
        switch (tier) {
            case "auto" -> target.put("service_tier", "auto");
            case "default" -> target.put("service_tier", "standard_only");
            case "flex", "priority", "scale" -> {
                // Scheduling hints with no Claude request equivalent are dropped.
            }
            default -> throw new ProtocolConversionException(
                    "RESPONSES_CLAUDE_SERVICE_TIER_NOT_SUPPORTED: " + tier);
        }
    }

    private void copyIfPresent(JsonNode source, ObjectNode target, String field) {
        JsonNode value = source.get(field);
        if (value != null && !value.isNull() && !value.isMissingNode()) {
            target.set(field, value.deepCopy());
        }
    }
}
