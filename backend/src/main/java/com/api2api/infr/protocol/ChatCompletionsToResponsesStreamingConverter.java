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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stateful Chat Completions SSE to Responses SSE converter.
 *
 * <p>Responses clients require a complete item lifecycle: every delta must
 * reference an opened output item and every item must be closed before the
 * terminal response event is emitted.</p>
 */
final class ChatCompletionsToResponsesStreamingConverter {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletionsToResponsesStreamingConverter.class);

    private final ObjectMapper objectMapper;
    private final ResponsesSseEmitter emitter;

    ChatCompletionsToResponsesStreamingConverter(ObjectMapper objectMapper) {
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
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode chunk;
                try {
                    chunk = objectMapper.readTree(data);
                } catch (JsonProcessingException exception) {
                    throw new IOException("Invalid Chat Completions SSE event", exception);
                }
                handleChunk(chunk, state, clientBody);
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

    private void handleChunk(JsonNode chunk, State state, OutputStream output) throws IOException {
        if (chunk.path("id").isTextual() && !chunk.path("id").asText().isBlank()) {
            state.responseId = toResponseId(chunk.path("id").asText());
        }
        if (state.model.isBlank() && chunk.path("model").isTextual()) {
            state.model = chunk.path("model").asText("");
        }
        if (chunk.path("created").canConvertToLong()) {
            state.createdAt = chunk.path("created").asLong();
        }
        ensureCreated(state, output);
        updateUsage(chunk.path("usage"), state);

        JsonNode choices = chunk.path("choices");
        if (!choices.isArray()) {
            return;
        }
        for (JsonNode choice : choices) {
            if (choice.path("index").asInt(0) != 0) {
                continue;
            }
            JsonNode delta = choice.path("delta");
            String reasoning = delta.path("reasoning_content").asText("");
            if (!reasoning.isEmpty()) {
                emitReasoningDelta(reasoning, state, output);
            }
            String content = delta.path("content").asText("");
            if (!content.isEmpty()) {
                emitMessageDelta(content, false, state, output);
            }
            String refusal = delta.path("refusal").asText("");
            if (!refusal.isEmpty()) {
                emitMessageDelta(refusal, true, state, output);
            }
            emitToolCallDeltas(delta.path("tool_calls"), state, output);
            if (choice.path("finish_reason").isTextual()) {
                state.finishReason = choice.path("finish_reason").asText("");
            }
        }
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

    private void emitReasoningDelta(String delta, State state, OutputStream output) throws IOException {
        if (!state.reasoningOpen) {
            closeMessage(state, output);
            state.reasoningOpen = true;
            state.reasoningIndex = state.nextOutputIndex++;
            state.reasoningId = itemId("rs");
            ObjectNode item = objectMapper.createObjectNode();
            item.put("type", "reasoning");
            item.put("id", state.reasoningId);
            item.put("status", "in_progress");
            item.set("summary", objectMapper.createArrayNode());
            writeOutputItemEvent("response.output_item.added", state.reasoningIndex, item, state, output);

            ObjectNode part = objectMapper.createObjectNode();
            part.put("type", "summary_text");
            part.put("text", "");
            ObjectNode event = indexedEvent(state.reasoningIndex, state.reasoningId);
            event.put("summary_index", 0);
            event.set("part", part);
            writeEvent("response.reasoning_summary_part.added", event, state, output);
        }
        state.reasoning.append(delta);
        ObjectNode event = indexedEvent(state.reasoningIndex, state.reasoningId);
        event.put("summary_index", 0);
        event.put("delta", delta);
        writeEvent("response.reasoning_summary_text.delta", event, state, output);
    }

    private void closeReasoning(State state, OutputStream output) throws IOException {
        if (!state.reasoningOpen) {
            return;
        }
        String text = state.reasoning.toString();
        ObjectNode done = indexedEvent(state.reasoningIndex, state.reasoningId);
        done.put("summary_index", 0);
        done.put("text", text);
        writeEvent("response.reasoning_summary_text.done", done, state, output);

        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "summary_text");
        part.put("text", text);
        ObjectNode partDone = indexedEvent(state.reasoningIndex, state.reasoningId);
        partDone.put("summary_index", 0);
        partDone.set("part", part);
        writeEvent("response.reasoning_summary_part.done", partDone, state, output);

        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "reasoning");
        item.put("id", state.reasoningId);
        item.put("status", "completed");
        ArrayNode summary = objectMapper.createArrayNode();
        summary.add(part);
        item.set("summary", summary);
        writeOutputItemEvent("response.output_item.done", state.reasoningIndex, item, state, output);
        state.completedOutput.put(state.reasoningIndex, item);
        state.reasoningOpen = false;
    }

    private void emitMessageDelta(
            String delta,
            boolean refusal,
            State state,
            OutputStream output
    ) throws IOException {
        closeReasoning(state, output);
        ensureMessage(refusal, state, output);
        if (refusal != state.messageRefusal) {
            closeMessage(state, output);
            ensureMessage(refusal, state, output);
        }
        state.messageText.append(delta);
        ObjectNode event = indexedEvent(state.messageIndex, state.messageId);
        event.put("content_index", 0);
        event.put("delta", delta);
        if (refusal) {
            event.put("refusal", delta);
        }
        writeEvent(refusal ? "response.refusal.delta" : "response.output_text.delta", event, state, output);
    }

    private void ensureMessage(boolean refusal, State state, OutputStream output) throws IOException {
        if (state.messageOpen) {
            return;
        }
        state.messageOpen = true;
        state.messageRefusal = refusal;
        state.messageIndex = state.nextOutputIndex++;
        state.messageId = itemId("msg");
        state.messageText.setLength(0);

        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "message");
        item.put("id", state.messageId);
        item.put("role", "assistant");
        item.put("status", "in_progress");
        item.set("content", objectMapper.createArrayNode());
        writeOutputItemEvent("response.output_item.added", state.messageIndex, item, state, output);

        ObjectNode part = messagePart(refusal, "");
        ObjectNode event = indexedEvent(state.messageIndex, state.messageId);
        event.put("content_index", 0);
        event.set("part", part);
        writeEvent("response.content_part.added", event, state, output);
    }

    private void closeMessage(State state, OutputStream output) throws IOException {
        if (!state.messageOpen) {
            return;
        }
        String text = state.messageText.toString();
        ObjectNode textDone = indexedEvent(state.messageIndex, state.messageId);
        textDone.put("content_index", 0);
        textDone.put(state.messageRefusal ? "refusal" : "text", text);
        writeEvent(state.messageRefusal ? "response.refusal.done" : "response.output_text.done",
                textDone, state, output);

        ObjectNode part = messagePart(state.messageRefusal, text);
        ObjectNode partDone = indexedEvent(state.messageIndex, state.messageId);
        partDone.put("content_index", 0);
        partDone.set("part", part);
        writeEvent("response.content_part.done", partDone, state, output);

        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "message");
        item.put("id", state.messageId);
        item.put("role", "assistant");
        item.put("status", "completed");
        ArrayNode content = objectMapper.createArrayNode();
        content.add(part);
        item.set("content", content);
        writeOutputItemEvent("response.output_item.done", state.messageIndex, item, state, output);
        state.completedOutput.put(state.messageIndex, item);
        state.messageOpen = false;
    }

    private void emitToolCallDeltas(JsonNode toolCalls, State state, OutputStream output) throws IOException {
        if (!toolCalls.isArray()) {
            return;
        }
        closeReasoning(state, output);
        closeMessage(state, output);
        for (JsonNode toolCall : toolCalls) {
            int chatIndex = toolCall.path("index").asInt(0);
            ToolState tool = state.tools.computeIfAbsent(chatIndex, ignored -> {
                ToolState created = new ToolState();
                created.outputIndex = state.nextOutputIndex++;
                created.itemId = itemId("fc");
                return created;
            });
            if (toolCall.path("id").isTextual() && !toolCall.path("id").asText().isBlank()) {
                tool.callId = toolCall.path("id").asText();
            }
            JsonNode function = toolCall.path("function");
            if (function.path("name").isTextual() && !function.path("name").asText().isBlank()) {
                tool.name = function.path("name").asText();
            }
            String argumentsDelta = function.path("arguments").asText("");
            if (!tool.announced && !tool.name.isBlank()) {
                announceTool(tool, state, output);
            }
            if (!argumentsDelta.isEmpty()) {
                tool.arguments.append(argumentsDelta);
                if (tool.announced) {
                    ObjectNode event = indexedEvent(tool.outputIndex, tool.itemId);
                    event.put("delta", argumentsDelta);
                    event.put("call_id", tool.callId);
                    event.put("name", tool.name);
                    writeEvent("response.function_call_arguments.delta", event, state, output);
                }
            }
        }
    }

    private void announceTool(ToolState tool, State state, OutputStream output) throws IOException {
        if (tool.callId.isBlank()) {
            tool.callId = itemId("call");
        }
        ObjectNode item = toolItem(tool, "in_progress", "");
        writeOutputItemEvent("response.output_item.added", tool.outputIndex, item, state, output);
        tool.announced = true;
        if (!tool.arguments.isEmpty()) {
            ObjectNode delta = indexedEvent(tool.outputIndex, tool.itemId);
            delta.put("delta", tool.arguments.toString());
            delta.put("call_id", tool.callId);
            delta.put("name", tool.name);
            writeEvent("response.function_call_arguments.delta", delta, state, output);
        }
    }

    private void closeTools(State state, OutputStream output) throws IOException {
        for (ToolState tool : state.tools.values().stream()
                .sorted((left, right) -> Integer.compare(left.outputIndex, right.outputIndex)).toList()) {
            if (!tool.announced) {
                // 名字始终未到时按 function_call 兜底宣告，禁止断流
                if (tool.name.isBlank()) {
                    log.warn("Chat Completions tool call ended without a function name, announcing as-is");
                }
                announceTool(tool, state, output);
            }
            String arguments = tool.arguments.isEmpty() ? "{}" : tool.arguments.toString();
            // 截断的参数 JSON 不能作为 completed 项下发：客户端（如 Codex）会把它持久化
            // 进历史并在下一轮回放，导致整个请求被上游拒绝
            if (!isValidJson(arguments)) {
                throw new IOException("Chat Completions tool call '" + tool.callId
                        + "' arguments are invalid JSON (truncated stream?)");
            }
            ObjectNode argsDone = indexedEvent(tool.outputIndex, tool.itemId);
            argsDone.put("arguments", arguments);
            argsDone.put("call_id", tool.callId);
            argsDone.put("name", tool.name);
            writeEvent("response.function_call_arguments.done", argsDone, state, output);

            ObjectNode item = toolItem(tool, "completed", arguments);
            writeOutputItemEvent("response.output_item.done", tool.outputIndex, item, state, output);
            state.completedOutput.put(tool.outputIndex, item);
        }
    }

    private boolean isValidJson(String value) {
        try {
            objectMapper.readTree(value);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private void finalizeStream(State state, OutputStream output) throws IOException {
        ensureCreated(state, output);
        closeReasoning(state, output);
        closeMessage(state, output);
        closeTools(state, output);
        if (state.completedOutput.isEmpty()) {
            ensureMessage(false, state, output);
            closeMessage(state, output);
        }

        ObjectNode response = baseResponse(state);
        String status = "completed";
        String incompleteReason = null;
        if ("length".equals(state.finishReason)) {
            status = "incomplete";
            incompleteReason = "max_output_tokens";
        } else if ("content_filter".equals(state.finishReason)) {
            status = "incomplete";
            incompleteReason = "content_filter";
        }
        response.put("status", status);
        if (incompleteReason != null) {
            response.set("incomplete_details",
                    objectMapper.createObjectNode().put("reason", incompleteReason));
        }
        ArrayNode outputItems = objectMapper.createArrayNode();
        state.completedOutput.values().forEach(outputItems::add);
        response.set("output", outputItems);
        response.put("output_text", collectOutputText(outputItems));
        if (state.usageKnown) {
            response.set("usage", responsesUsage(state));
        }
        writeEvent("response.completed",
                objectMapper.createObjectNode().set("response", response), state, output);
    }

    private void updateUsage(JsonNode usage, State state) {
        if (!usage.isObject()) {
            return;
        }
        state.usageKnown = true;
        JsonNode details = usage.path("prompt_tokens_details");
        state.cacheReadInputTokens = details.path("cached_tokens").asLong(0);
        state.cacheCreationInputTokens = details.path("cache_write_tokens").asLong(0);
        if (state.cacheCreationInputTokens <= 0) {
            state.cacheCreationInputTokens = details.path("cache_creation_tokens").asLong(0);
        }
        state.inputTokens = Math.max(0, usage.path("prompt_tokens").asLong(0)
                - state.cacheReadInputTokens - state.cacheCreationInputTokens);
        state.outputTokens = usage.path("completion_tokens").asLong(0);
    }

    private ObjectNode responsesUsage(State state) {
        return emitter.responsesUsage(
                state.inputTokens, state.outputTokens,
                state.cacheCreationInputTokens, state.cacheReadInputTokens);
    }

    private ObjectNode baseResponse(State state) {
        return emitter.baseResponse(state.responseId, state.createdAt, state.model);
    }

    private ObjectNode indexedEvent(int outputIndex, String itemId) {
        ObjectNode event = objectMapper.createObjectNode();
        event.put("output_index", outputIndex);
        event.put("item_id", itemId);
        return event;
    }

    private void writeOutputItemEvent(
            String type,
            int outputIndex,
            ObjectNode item,
            State state,
            OutputStream output
    ) throws IOException {
        ObjectNode event = indexedEvent(outputIndex, item.path("id").asText(""));
        event.set("item", item);
        writeEvent(type, event, state, output);
    }

    private void writeEvent(String type, ObjectNode event, State state, OutputStream output) throws IOException {
        emitter.writeEvent(type, event, state.sequenceHolder, output);
    }

    private ObjectNode messagePart(boolean refusal, String text) {
        ObjectNode part = objectMapper.createObjectNode();
        if (refusal) {
            part.put("type", "refusal");
            part.put("refusal", text);
        } else {
            part.put("type", "output_text");
            part.put("text", text);
            part.set("annotations", objectMapper.createArrayNode());
        }
        return part;
    }

    private ObjectNode toolItem(ToolState tool, String status, String arguments) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "function_call");
        item.put("id", tool.itemId);
        item.put("call_id", tool.callId);
        item.put("name", tool.name);
        item.put("arguments", arguments);
        item.put("status", status);
        return item;
    }

    private String collectOutputText(ArrayNode output) {
        return emitter.collectOutputText(output);
    }

    private String toResponseId(String chatId) {
        if (chatId.startsWith("resp_")) {
            return chatId;
        }
        String suffix = chatId.startsWith("chatcmpl-")
                ? chatId.substring("chatcmpl-".length()) : chatId;
        return "resp_" + suffix;
    }

    private String itemId(String prefix) {
        return emitter.itemId(prefix);
    }

    private static final class State {
        private String responseId = "resp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        private String model;
        private long createdAt = Instant.now().getEpochSecond();
        private final int[] sequenceHolder = {0};
        private int nextOutputIndex;
        private boolean createdSent;
        private String finishReason = "stop";

        private boolean reasoningOpen;
        private int reasoningIndex;
        private String reasoningId;
        private final StringBuilder reasoning = new StringBuilder();

        private boolean messageOpen;
        private boolean messageRefusal;
        private int messageIndex;
        private String messageId;
        private final StringBuilder messageText = new StringBuilder();

        private final Map<Integer, ToolState> tools = new HashMap<>();
        private final Map<Integer, ObjectNode> completedOutput = new TreeMap<>();
        private boolean usageKnown;
        private long inputTokens;
        private long outputTokens;
        private long cacheCreationInputTokens;
        private long cacheReadInputTokens;

        private State(String model) {
            this.model = model == null ? "" : model;
        }
    }

    private static final class ToolState {
        private int outputIndex;
        private String itemId = "";
        private String callId = "";
        private String name = "";
        private final StringBuilder arguments = new StringBuilder();
        private boolean announced;
    }
}
