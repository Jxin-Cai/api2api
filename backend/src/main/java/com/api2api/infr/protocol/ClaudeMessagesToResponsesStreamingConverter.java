package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** Converts Anthropic Messages SSE events into strict OpenAI Responses SSE events. */
final class ClaudeMessagesToResponsesStreamingConverter {

    private final ObjectMapper objectMapper;
    private final ResponsesSseEmitter emitter;

    ClaudeMessagesToResponsesStreamingConverter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
        this.emitter = new ResponsesSseEmitter(objectMapper);
    }

    UnifiedTokenUsage transform(
            String clientModel,
            InputStream upstreamBody,
            OutputStream clientBody
    ) throws IOException {
        State state = new State(clientModel);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(upstreamBody, StandardCharsets.UTF_8))) {
            String eventName = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).trim();
                if (data.isEmpty()) {
                    continue;
                }
                JsonNode event;
                try {
                    event = objectMapper.readTree(data);
                } catch (JsonProcessingException exception) {
                    throw new IOException("Invalid Claude Messages SSE event", exception);
                }
                String type = eventName == null || eventName.isBlank()
                        ? event.path("type").asText("") : eventName;
                eventName = null;
                handleEvent(type, event, state, clientBody);
                if (state.messageStopped) {
                    break;
                }
            }
        }
        finalizeStream(state, clientBody);
        clientBody.flush();
        return state.usageKnown
                ? UnifiedTokenUsage.known(
                        state.inputTokens,
                        state.outputTokens,
                        state.cacheCreationInputTokens,
                        state.cacheReadInputTokens)
                : UnifiedTokenUsage.unknown();
    }

    private void handleEvent(String type, JsonNode event, State state, OutputStream output) throws IOException {
        switch (type) {
            case "message_start" -> handleMessageStart(event.path("message"), state, output);
            case "content_block_start" -> openBlock(
                    event.path("index").asInt(0), event.path("content_block"), state, output);
            case "content_block_delta" -> emitBlockDelta(
                    event.path("index").asInt(0), event.path("delta"), state, output);
            case "content_block_stop" -> closeBlock(event.path("index").asInt(0), state, output);
            case "message_delta" -> handleMessageDelta(event, state);
            case "message_stop" -> state.messageStopped = true;
            case "error" -> throw new IOException("Claude Messages stream failed: "
                    + event.path("error").path("message").asText("unknown error"));
            case "ping" -> {
            }
            default -> {
            }
        }
    }

    private void handleMessageStart(JsonNode message, State state, OutputStream output) throws IOException {
        if (message.path("id").isTextual() && !message.path("id").asText().isBlank()) {
            state.responseId = toResponseId(message.path("id").asText());
        }
        JsonNode usage = message.path("usage");
        state.inputTokens = usage.path("input_tokens").asLong(0);
        state.cacheCreationInputTokens = usage.path("cache_creation_input_tokens").asLong(0);
        state.cacheReadInputTokens = usage.path("cache_read_input_tokens").asLong(0);
        state.usageKnown = usage.isObject();
        ensureCreated(state, output);
    }

    private void handleMessageDelta(JsonNode event, State state) {
        String stopReason = event.path("delta").path("stop_reason").asText("");
        if (!stopReason.isBlank()) {
            state.stopReason = stopReason;
        }
        JsonNode usage = event.path("usage");
        if (usage.isObject()) {
            state.usageKnown = true;
            state.outputTokens = usage.path("output_tokens").asLong(state.outputTokens);
            state.inputTokens = usage.path("input_tokens").asLong(state.inputTokens);
            state.cacheCreationInputTokens = usage.path("cache_creation_input_tokens")
                    .asLong(state.cacheCreationInputTokens);
            state.cacheReadInputTokens = usage.path("cache_read_input_tokens")
                    .asLong(state.cacheReadInputTokens);
        }
    }

    private void openBlock(
            int claudeIndex,
            JsonNode contentBlock,
            State state,
            OutputStream output
    ) throws IOException {
        ensureCreated(state, output);
        if (state.blocks.containsKey(claudeIndex)) {
            return;
        }
        String type = contentBlock.path("type").asText("");
        BlockState block = new BlockState(state.nextOutputIndex++, type);
        state.blocks.put(claudeIndex, block);
        switch (type) {
            case "thinking" -> openReasoning(block, state, output);
            case "redacted_thinking" -> openRedactedThinking(block, contentBlock, state, output);
            case "text" -> openText(block, state, output);
            case "tool_use", "mcp_tool_use" -> {
                if ("mcp_tool_use".equals(type)) {
                    block.type = "tool_use";
                    block.namespace = contentBlock.path("server_name").asText("");
                    if (block.namespace.isBlank()) {
                        block.namespace = "mcp";
                    }
                }
                openTool(block, contentBlock, state, output);
            }
            default -> {
                state.blocks.remove(claudeIndex);
                throw new IOException("Unsupported Claude streaming content block: " + type);
            }
        }
    }

    private void openReasoning(BlockState block, State state, OutputStream output) throws IOException {
        block.itemId = itemId("rs");
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "reasoning");
        item.put("id", block.itemId);
        item.put("status", "in_progress");
        item.set("summary", objectMapper.createArrayNode());
        writeOutputItemEvent("response.output_item.added", block, item, state, output);

        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "summary_text");
        part.put("text", "");
        ObjectNode event = indexedEvent(block);
        event.put("summary_index", 0);
        event.set("part", part);
        writeEvent("response.reasoning_summary_part.added", event, state, output);
    }

    private void openRedactedThinking(
            BlockState block,
            JsonNode contentBlock,
            State state,
            OutputStream output
    ) throws IOException {
        block.itemId = itemId("rs");
        block.redactedData = contentBlock.path("data").asText("");
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "reasoning");
        item.put("id", block.itemId);
        item.put("status", "in_progress");
        item.set("summary", objectMapper.createArrayNode());
        writeOutputItemEvent("response.output_item.added", block, item, state, output);
    }

    private void openText(BlockState block, State state, OutputStream output) throws IOException {
        block.itemId = itemId("msg");
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "message");
        item.put("id", block.itemId);
        item.put("role", "assistant");
        item.put("status", "in_progress");
        item.set("content", objectMapper.createArrayNode());
        writeOutputItemEvent("response.output_item.added", block, item, state, output);

        ObjectNode event = indexedEvent(block);
        event.put("content_index", 0);
        event.set("part", outputTextPart(""));
        writeEvent("response.content_part.added", event, state, output);
    }

    private void openTool(
            BlockState block,
            JsonNode contentBlock,
            State state,
            OutputStream output
    ) throws IOException {
        block.itemId = itemId("fc");
        block.callId = contentBlock.path("id").asText(itemId("call"));
        block.name = contentBlock.path("name").asText("");
        ObjectNode item = toolItem(block, "in_progress", "");
        writeOutputItemEvent("response.output_item.added", block, item, state, output);
        JsonNode initialInput = contentBlock.get("input");
        if (initialInput != null && initialInput.isObject() && !initialInput.isEmpty()) {
            emitToolArguments(block, initialInput.toString(), state, output);
        }
    }

    private void emitBlockDelta(
            int claudeIndex,
            JsonNode delta,
            State state,
            OutputStream output
    ) throws IOException {
        BlockState block = state.blocks.get(claudeIndex);
        if (block == null) {
            throw new IOException("Claude content delta referenced an unopened block: " + claudeIndex);
        }
        switch (delta.path("type").asText("")) {
            case "thinking_delta" -> {
                String value = delta.path("thinking").asText("");
                block.value.append(value);
                ObjectNode event = indexedEvent(block);
                event.put("summary_index", 0);
                event.put("delta", value);
                writeEvent("response.reasoning_summary_text.delta", event, state, output);
            }
            case "text_delta" -> {
                String value = delta.path("text").asText("");
                block.value.append(value);
                ObjectNode event = indexedEvent(block);
                event.put("content_index", 0);
                event.put("delta", value);
                writeEvent("response.output_text.delta", event, state, output);
            }
            case "input_json_delta" -> emitToolArguments(
                    block, delta.path("partial_json").asText(""), state, output);
            // Responses has no signature event; the signature is tunneled through
            // reasoning.encrypted_content on the final output item instead.
            case "signature_delta" -> block.signature.append(delta.path("signature").asText(""));
            default -> {
            }
        }
    }

    private void emitToolArguments(
            BlockState block,
            String delta,
            State state,
            OutputStream output
    ) throws IOException {
        if (delta.isEmpty()) {
            return;
        }
        block.value.append(delta);
        ObjectNode event = indexedEvent(block);
        event.put("delta", delta);
        event.put("call_id", block.callId);
        event.put("name", block.name);
        writeEvent("response.function_call_arguments.delta", event, state, output);
    }

    private void closeBlock(int claudeIndex, State state, OutputStream output) throws IOException {
        BlockState block = state.blocks.get(claudeIndex);
        if (block == null || block.closed) {
            return;
        }
        switch (block.type) {
            case "thinking" -> closeReasoning(block, state, output);
            case "redacted_thinking" -> closeRedactedThinking(block, state, output);
            case "text" -> closeText(block, state, output);
            case "tool_use" -> closeTool(block, state, output);
            default -> throw new IOException("Unsupported Claude streaming content block: " + block.type);
        }
        block.closed = true;
    }

    private void closeReasoning(BlockState block, State state, OutputStream output) throws IOException {
        String text = block.value.toString();
        ObjectNode done = indexedEvent(block);
        done.put("summary_index", 0);
        done.put("text", text);
        writeEvent("response.reasoning_summary_text.done", done, state, output);

        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "summary_text");
        part.put("text", text);
        ObjectNode partDone = indexedEvent(block);
        partDone.put("summary_index", 0);
        partDone.set("part", part);
        writeEvent("response.reasoning_summary_part.done", partDone, state, output);

        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "reasoning");
        item.put("id", block.itemId);
        item.put("status", "completed");
        ArrayNode summary = objectMapper.createArrayNode();
        summary.add(part);
        item.set("summary", summary);
        String signature = block.signature.toString();
        if (!signature.isBlank()) {
            ObjectNode claudeBlock = objectMapper.createObjectNode();
            claudeBlock.put("type", "thinking");
            claudeBlock.put("thinking", text);
            claudeBlock.put("signature", signature);
            ClaudeThinkingStateBridge.encode(objectMapper, claudeBlock)
                    .ifPresent(bridged -> item.put("encrypted_content", bridged));
        }
        closeItem(block, item, state, output);
    }

    private void closeRedactedThinking(BlockState block, State state, OutputStream output) throws IOException {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "reasoning");
        item.put("id", block.itemId);
        item.put("status", "completed");
        item.set("summary", objectMapper.createArrayNode());
        ObjectNode claudeBlock = objectMapper.createObjectNode();
        claudeBlock.put("type", "redacted_thinking");
        claudeBlock.put("data", block.redactedData);
        ClaudeThinkingStateBridge.encode(objectMapper, claudeBlock)
                .ifPresent(bridged -> item.put("encrypted_content", bridged));
        closeItem(block, item, state, output);
    }

    private void closeText(BlockState block, State state, OutputStream output) throws IOException {
        String text = block.value.toString();
        ObjectNode done = indexedEvent(block);
        done.put("content_index", 0);
        done.put("text", text);
        writeEvent("response.output_text.done", done, state, output);

        ObjectNode part = outputTextPart(text);
        ObjectNode partDone = indexedEvent(block);
        partDone.put("content_index", 0);
        partDone.set("part", part);
        writeEvent("response.content_part.done", partDone, state, output);

        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "message");
        item.put("id", block.itemId);
        item.put("role", "assistant");
        item.put("status", "completed");
        ArrayNode content = objectMapper.createArrayNode();
        content.add(part);
        item.set("content", content);
        closeItem(block, item, state, output);
    }

    private void closeTool(BlockState block, State state, OutputStream output) throws IOException {
        String arguments = block.value.isEmpty() ? "{}" : block.value.toString();
        ObjectNode argsDone = indexedEvent(block);
        argsDone.put("arguments", arguments);
        argsDone.put("call_id", block.callId);
        argsDone.put("name", block.name);
        writeEvent("response.function_call_arguments.done", argsDone, state, output);
        closeItem(block, toolItem(block, "completed", arguments), state, output);
    }

    private void closeItem(
            BlockState block,
            ObjectNode item,
            State state,
            OutputStream output
    ) throws IOException {
        writeOutputItemEvent("response.output_item.done", block, item, state, output);
        state.completedOutput.put(block.outputIndex, item);
    }

    private void finalizeStream(State state, OutputStream output) throws IOException {
        ensureCreated(state, output);
        for (Map.Entry<Integer, BlockState> entry : new TreeMap<>(state.blocks).entrySet()) {
            closeBlock(entry.getKey(), state, output);
        }
        if (state.completedOutput.isEmpty()) {
            BlockState empty = new BlockState(state.nextOutputIndex++, "text");
            openText(empty, state, output);
            closeText(empty, state, output);
        }

        ObjectNode response = baseResponse(state);
        String status = switch (state.stopReason) {
            case "max_tokens", "model_context_window_exceeded" -> "incomplete";
            default -> "completed";
        };
        response.put("status", status);
        if ("incomplete".equals(status)) {
            String reason = "model_context_window_exceeded".equals(state.stopReason)
                    ? "model_context_window_exceeded" : "max_output_tokens";
            response.set("incomplete_details", objectMapper.createObjectNode().put("reason", reason));
        }
        ArrayNode outputItems = objectMapper.createArrayNode();
        state.completedOutput.values().forEach(outputItems::add);
        response.set("output", outputItems);
        response.put("output_text", collectOutputText(outputItems));
        if (state.usageKnown) {
            response.set("usage", responsesUsage(state));
        }
        String terminalEvent = "incomplete".equals(status) ? "response.incomplete" : "response.completed";
        writeEvent(terminalEvent,
                objectMapper.createObjectNode().set("response", response), state, output);
    }

    private void ensureCreated(State state, OutputStream output) throws IOException {
        if (state.createdSent) {
            return;
        }
        state.createdSent = true;
        ObjectNode response = baseResponse(state);
        response.put("status", "in_progress");
        response.set("output", objectMapper.createArrayNode());
        writeEvent("response.created", objectMapper.createObjectNode().set("response", response), state, output);
    }

    private ObjectNode baseResponse(State state) {
        return emitter.baseResponse(state.responseId, state.createdAt, state.model);
    }

    private ObjectNode responsesUsage(State state) {
        return emitter.responsesUsage(
                state.inputTokens, state.outputTokens,
                state.cacheCreationInputTokens, state.cacheReadInputTokens);
    }

    private ObjectNode outputTextPart(String text) {
        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "output_text");
        part.put("text", text);
        part.set("annotations", objectMapper.createArrayNode());
        return part;
    }

    private ObjectNode toolItem(BlockState block, String status, String arguments) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "function_call");
        item.put("id", block.itemId);
        item.put("call_id", block.callId);
        item.put("name", block.name);
        if (!block.namespace.isBlank()) {
            item.put("namespace", block.namespace);
        }
        item.put("arguments", arguments);
        item.put("status", status);
        return item;
    }

    private ObjectNode indexedEvent(BlockState block) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("output_index", block.outputIndex);
        event.put("item_id", block.itemId);
        return event;
    }

    private void writeOutputItemEvent(
            String type,
            BlockState block,
            ObjectNode item,
            State state,
            OutputStream output
    ) throws IOException {
        ObjectNode event = indexedEvent(block);
        event.set("item", item);
        writeEvent(type, event, state, output);
    }

    private void writeEvent(String type, ObjectNode event, State state, OutputStream output) throws IOException {
        emitter.writeEvent(type, event, state.sequenceHolder, output);
    }

    private String collectOutputText(ArrayNode output) {
        return emitter.collectOutputText(output);
    }

    private String toResponseId(String messageId) {
        if (messageId.startsWith("resp_")) {
            return messageId;
        }
        String suffix = messageId.startsWith("msg_")
                ? messageId.substring("msg_".length()) : messageId;
        return "resp_" + suffix;
    }

    private String itemId(String prefix) {
        return emitter.itemId(prefix);
    }

    private static final class State {
        private String responseId = "resp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        private final String model;
        private final long createdAt = Instant.now().getEpochSecond();
        private final int[] sequenceHolder = {0};
        private int nextOutputIndex;
        private boolean createdSent;
        private boolean messageStopped;
        private boolean usageKnown;
        private String stopReason = "end_turn";
        private long inputTokens;
        private long outputTokens;
        private long cacheCreationInputTokens;
        private long cacheReadInputTokens;
        private final Map<Integer, BlockState> blocks = new HashMap<>();
        private final Map<Integer, ObjectNode> completedOutput = new TreeMap<>();

        private State(String model) {
            this.model = model == null ? "" : model;
        }
    }

    private static final class BlockState {
        private final int outputIndex;
        private String type;
        private String itemId = "";
        private String callId = "";
        private String name = "";
        private String namespace = "";
        private String redactedData = "";
        private final StringBuilder value = new StringBuilder();
        private final StringBuilder signature = new StringBuilder();
        private boolean closed;

        private BlockState(int outputIndex, String type) {
            this.outputIndex = outputIndex;
            this.type = type;
        }
    }
}
