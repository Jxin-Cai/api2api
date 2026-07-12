package com.api2api.infr.protocol;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Converts AWS Bedrock ConverseStream event-stream frames into Claude Messages SSE events.
 */
@Component
public class BedrockConverseClaudeStreamingConversionAdapter implements GatewayStreamingConversionPort {

    private static final int EVENT_STREAM_OVERHEAD_BYTES = 16;
    private static final String RESPONSES_OPAQUE_STATE_PLACEHOLDER = "Thinking...";
    private static final String RESPONSES_COMPACTION_PLACEHOLDER = "Context compacted.";

    private final ObjectMapper objectMapper;

    public BedrockConverseClaudeStreamingConversionAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public boolean supports(ProtocolType upstreamProtocol, ProtocolType clientProtocol) {
        return (upstreamProtocol == ProtocolType.AWS_BEDROCK_CONVERSE
                && (clientProtocol == ProtocolType.CLAUDE_MESSAGES || clientProtocol == ProtocolType.OPENAI_RESPONSES))
                || (upstreamProtocol == ProtocolType.OPENAI_RESPONSES
                && clientProtocol == ProtocolType.CLAUDE_MESSAGES);
    }

    @Override
    public UnifiedTokenUsage transform(
            GatewayStreamingConversionContext context,
            InputStream upstreamBody,
            OutputStream clientBody
    ) throws IOException {
        Objects.requireNonNull(context, "Streaming conversion context must not be null");
        ProtocolType upstreamProtocol = context.upstreamProtocol();
        ProtocolType clientProtocol = context.clientProtocol();
        if (!supports(upstreamProtocol, clientProtocol)) {
            return UnifiedTokenUsage.unknown();
        }
        if (upstreamProtocol == ProtocolType.OPENAI_RESPONSES) {
            return transformResponsesToClaude(context.clientModel().value(), upstreamBody, clientBody);
        }
        StreamState state = new StreamState(clientProtocol, context.clientModel().value());
        BedrockEvent event;
        while ((event = readEvent(upstreamBody)) != null) {
            JsonNode payload = objectMapper.readTree(event.payload());
            throwIfModeledException(event, payload);
            handleEvent(event.eventType(), payload, state, clientBody);
        }
        if (!state.upstreamMessageStopped) {
            throw new EOFException("Bedrock Converse stream ended before messageStop");
        }
        writeTerminalEventIfNecessary(state, clientBody);
        clientBody.flush();
        return state.usage == null ? UnifiedTokenUsage.unknown() : state.usage;
    }

    private void handleEvent(String eventType, JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        if (state.clientProtocol == ProtocolType.OPENAI_RESPONSES) {
            handleResponsesEvent(eventType, payload, state, clientBody);
            return;
        }
        switch (eventType) {
            case "messageStart" -> writeMessageStart(state, clientBody);
            case "contentBlockStart" -> writeContentBlockStart(payload, state, clientBody);
            case "contentBlockDelta" -> writeContentBlockDelta(payload, state, clientBody);
            case "contentBlockStop" -> writeContentBlockStop(payload, state, clientBody);
            case "messageStop" -> writeMessageStop(payload, state, clientBody);
            case "metadata" -> writeMetadata(payload, state, clientBody);
            case "internalServerException", "modelStreamErrorException", "validationException",
                 "throttlingException", "serviceUnavailableException" ->
                    throw new IOException("Bedrock Converse stream failed: " + eventType + streamErrorMessage(payload));
            default -> {
            }
        }
    }

    private void handleResponsesEvent(String eventType, JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        switch (eventType) {
            case "messageStart" -> writeResponsesStarted(state, clientBody);
            case "contentBlockDelta" -> writeResponsesDelta(payload, state, clientBody);
            case "messageStop" -> writeResponsesCompleted(payload, state, clientBody);
            case "metadata" -> recordResponsesUsage(payload, state, clientBody);
            default -> {
            }
        }
    }

    private void writeResponsesStarted(StreamState state, OutputStream clientBody) throws IOException {
        if (state.messageStarted) {
            return;
        }
        ObjectNode response = objectNode();
        response.put("id", "resp_api2api_bedrock_stream");
        response.put("object", "response");
        response.put("created_at", 0);
        response.put("model", state.clientModel);
        response.put("status", "in_progress");
        response.set("output", objectMapper.createArrayNode());
        ObjectNode event = objectNode();
        event.put("type", "response.created");
        event.set("response", response);
        writeSse(clientBody, "response.created", event);
        state.messageStarted = true;
    }

    private void writeResponsesDelta(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        JsonNode delta = payload.path("delta");
        if (delta.has("text")) {
            writeResponsesStarted(state, clientBody);
            ObjectNode event = objectNode();
            event.put("type", "response.output_text.delta");
            event.put("item_id", "msg_api2api_bedrock_stream");
            event.put("output_index", 0);
            event.put("content_index", 0);
            event.put("delta", delta.path("text").asText(""));
            writeSse(clientBody, "response.output_text.delta", event);
            return;
        }
        JsonNode reasoningText = reasoningTextNode(delta);
        if (reasoningText != null && !reasoningText.isMissingNode() && !reasoningText.isNull()) {
            writeResponsesStarted(state, clientBody);
            ObjectNode event = objectNode();
            event.put("type", "response.reasoning_summary_text.delta");
            event.put("item_id", "rs_api2api_bedrock_stream");
            event.put("output_index", 0);
            event.put("summary_index", 0);
            event.put("delta", reasoningText.path("text").asText(reasoningText.asText("")));
            writeSse(clientBody, "response.reasoning_summary_text.delta", event);
        }
    }

    private void writeResponsesCompleted(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        state.stopReason = payload.path("stopReason").asText("end_turn");
        state.upstreamMessageStopped = true;
    }

    private void recordResponsesUsage(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        state.usage = extractUsage(payload.path("usage"));
        writeTerminalEventIfNecessary(state, clientBody);
    }

    private void writeTerminalEventIfNecessary(StreamState state, OutputStream clientBody) throws IOException {
        if (state.messageStopped) {
            return;
        }
        if (state.clientProtocol == ProtocolType.OPENAI_RESPONSES) {
            writeResponsesStarted(state, clientBody);
            ObjectNode response = objectNode();
            response.put("id", "resp_api2api_bedrock_stream");
            response.put("object", "response");
            response.put("created_at", 0);
            response.put("model", state.clientModel);
            response.put("status", "completed");
            response.set("output", objectMapper.createArrayNode());
            if (state.usage != null && state.usage.usageKnown()) {
                ObjectNode usageNode = objectNode();
                usageNode.put("input_tokens", state.usage.inputTokens());
                usageNode.put("output_tokens", state.usage.outputTokens());
                usageNode.put("total_tokens", state.usage.totalTokens());
                response.set("usage", usageNode);
            }
            ObjectNode event = objectNode();
            event.put("type", "response.completed");
            event.set("response", response);
            writeSse(clientBody, "response.completed", event);
            clientBody.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
        } else {
            ObjectNode deltaEvent = objectNode();
            deltaEvent.put("type", "message_delta");
            ObjectNode delta = objectNode();
            delta.put("stop_reason", state.stopReason);
            if (state.stopSequence == null) {
                delta.putNull("stop_sequence");
            } else {
                delta.put("stop_sequence", state.stopSequence);
            }
            deltaEvent.set("delta", delta);
            if (state.usage != null && state.usage.usageKnown()) {
                deltaEvent.set("usage", claudeUsage(state.usage));
            }
            writeSse(clientBody, "message_delta", deltaEvent);
            writeSse(clientBody, "message_stop", objectNode().put("type", "message_stop"));
        }
        state.messageStopped = true;
    }

    private void writeMessageStart(StreamState state, OutputStream clientBody) throws IOException {
        if (state.messageStarted) {
            return;
        }
        ObjectNode message = objectNode();
        message.put("id", "msg_api2api_bedrock_stream");
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("model", state.clientModel);
        message.set("content", objectMapper.createArrayNode());
        message.putNull("stop_reason");
        message.putNull("stop_sequence");
        ObjectNode usage = objectNode();
        usage.put("input_tokens", 0);
        usage.put("output_tokens", 0);
        message.set("usage", usage);

        ObjectNode event = objectNode();
        event.put("type", "message_start");
        event.set("message", message);
        writeSse(clientBody, "message_start", event);
        state.messageStarted = true;
    }

    private void writeContentBlockStart(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        int index = payload.path("contentBlockIndex").asInt(0);
        JsonNode start = payload.path("start");
        if (start.has("toolUse")) {
            JsonNode toolUse = start.path("toolUse");
            ObjectNode contentBlock = objectNode();
            contentBlock.put("type", "tool_use");
            contentBlock.put("id", toolUse.path("toolUseId").asText(""));
            contentBlock.put("name", toolUse.path("name").asText(""));
            contentBlock.set("input", objectNode());
            writeContentBlockStart(index, contentBlock, "tool_use", state, clientBody);
            return;
        }
        JsonNode reasoningContent = start.path("reasoningContent");
        if (reasoningContent.hasNonNull("redactedContent")) {
            ObjectNode contentBlock = objectNode();
            contentBlock.put("type", "redacted_thinking");
            contentBlock.put("data", reasoningContent.path("redactedContent").asText());
            writeContentBlockStart(index, contentBlock, "redacted_thinking", state, clientBody);
        }
    }

    private void writeContentBlockDelta(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        int index = payload.path("contentBlockIndex").asInt(0);
        JsonNode delta = payload.path("delta");
        if (delta.has("text")) {
            ensureContentBlockStarted(index, "text", state, clientBody);
            ObjectNode event = objectNode();
            event.put("type", "content_block_delta");
            event.put("index", index);
            ObjectNode textDelta = objectNode();
            textDelta.put("type", "text_delta");
            textDelta.put("text", delta.path("text").asText(""));
            event.set("delta", textDelta);
            writeSse(clientBody, "content_block_delta", event);
            return;
        }
        JsonNode reasoningText = reasoningTextNode(delta);
        if (reasoningText != null && !reasoningText.isMissingNode() && !reasoningText.isNull()) {
            ensureContentBlockStarted(index, "thinking", state, clientBody);
            ObjectNode event = objectNode();
            event.put("type", "content_block_delta");
            event.put("index", index);
            ObjectNode thinkingDelta = objectNode();
            thinkingDelta.put("type", "thinking_delta");
            thinkingDelta.put("thinking", reasoningText.path("text").asText(reasoningText.asText("")));
            event.set("delta", thinkingDelta);
            writeSse(clientBody, "content_block_delta", event);
            JsonNode signature = reasoningText.path("signature");
            if (signature.isTextual() && !signature.asText().isBlank()) {
                ObjectNode signatureEvent = objectNode();
                signatureEvent.put("type", "content_block_delta");
                signatureEvent.put("index", index);
                ObjectNode signatureDelta = objectNode();
                signatureDelta.put("type", "signature_delta");
                signatureDelta.put("signature", signature.asText());
                signatureEvent.set("delta", signatureDelta);
                writeSse(clientBody, "content_block_delta", signatureEvent);
            }
            return;
        }
        JsonNode toolUse = delta.path("toolUse");
        if (!toolUse.isMissingNode()) {
            ensureContentBlockStarted(index, "tool_use", state, clientBody);
            String partialJson = toolUse.path("input").isTextual()
                    ? toolUse.path("input").asText("")
                    : toolUse.path("input").toString();
            ObjectNode event = objectNode();
            event.put("type", "content_block_delta");
            event.put("index", index);
            ObjectNode inputDelta = objectNode();
            inputDelta.put("type", "input_json_delta");
            inputDelta.put("partial_json", partialJson);
            event.set("delta", inputDelta);
            writeSse(clientBody, "content_block_delta", event);
        }
    }

    private void ensureContentBlockStarted(int index, String type, StreamState state, OutputStream clientBody) throws IOException {
        if (state.blockTypes.containsKey(index)) {
            return;
        }
        ObjectNode contentBlock = objectNode();
        contentBlock.put("type", type);
        if ("text".equals(type)) {
            contentBlock.put("text", "");
        } else if ("thinking".equals(type)) {
            contentBlock.put("thinking", "");
        } else if ("tool_use".equals(type)) {
            contentBlock.put("id", "");
            contentBlock.put("name", "");
            contentBlock.set("input", objectNode());
        }
        writeContentBlockStart(index, contentBlock, type, state, clientBody);
    }

    private void writeContentBlockStart(
            int index,
            ObjectNode contentBlock,
            String type,
            StreamState state,
            OutputStream clientBody
    ) throws IOException {
        writeMessageStart(state, clientBody);
        state.blockTypes.put(index, type);
        ObjectNode event = objectNode();
        event.put("type", "content_block_start");
        event.put("index", index);
        event.set("content_block", contentBlock);
        writeSse(clientBody, "content_block_start", event);
    }

    private void writeContentBlockStop(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        int index = payload.path("contentBlockIndex").asInt(0);
        if (!state.stoppedBlocks.containsKey(index)) {
            ObjectNode event = objectNode();
            event.put("type", "content_block_stop");
            event.put("index", index);
            writeSse(clientBody, "content_block_stop", event);
            state.stoppedBlocks.put(index, true);
        }
    }

    private void writeMessageStop(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        state.stopReason = mapBedrockStopToClaudeStop(payload.path("stopReason").asText("end_turn"));
        state.upstreamMessageStopped = true;
        JsonNode stopSequence = payload.path("additionalModelResponseFields").get("stop_sequence");
        if (stopSequence != null && stopSequence.isTextual()) {
            state.stopSequence = stopSequence.asText();
        }
    }

    private void writeMetadata(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        UnifiedTokenUsage usage = extractUsage(payload.path("usage"));
        state.usage = usage;
        if (state.upstreamMessageStopped) {
            writeTerminalEventIfNecessary(state, clientBody);
        }
    }

    private ObjectNode claudeUsage(UnifiedTokenUsage usage) {
        ObjectNode usageNode = objectNode();
        usageNode.put("input_tokens", usage.inputTokens());
        usageNode.put("output_tokens", usage.outputTokens());
        if (usage.cacheReadInputTokens() > 0) {
            usageNode.put("cache_read_input_tokens", usage.cacheReadInputTokens());
        }
        if (usage.cacheCreationInputTokens() > 0) {
            usageNode.put("cache_creation_input_tokens", usage.cacheCreationInputTokens());
        }
        return usageNode;
    }

    private UnifiedTokenUsage transformResponsesToClaude(
            String clientModel,
            InputStream upstreamBody,
            OutputStream clientBody
    ) throws IOException {
        ResponsesStreamState state = new ResponsesStreamState(clientModel);
        writeClaudeMessageStart(clientBody, state);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(upstreamBody, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                handleResponsesSseEvent(objectMapper.readTree(data), state, clientBody);
            }
        }
        if (!state.terminalEventSeen) {
            throw new EOFException("OpenAI Responses stream ended before a terminal response event");
        }
        finishResponsesToClaude(state, clientBody);
        clientBody.flush();
        return state.usage == null ? UnifiedTokenUsage.unknown() : state.usage;
    }

    private void handleResponsesSseEvent(JsonNode event, ResponsesStreamState state, OutputStream clientBody) throws IOException {
        String type = event.path("type").asText("");
        int outputIndex = event.path("output_index").asInt(0);
        switch (type) {
            case "response.output_text.delta" -> {
                state.responseOutputSeen = true;
                state.finalMessageSeen = true;
                ensureClaudeBlockStarted(outputIndex, "text", null, null, state, clientBody);
                writeClaudeContentDelta(outputIndex, "text_delta", "text", event.path("delta").asText(""), state, clientBody);
                state.textDeltaIndexes.add(outputIndex);
            }
            case "response.reasoning_summary_text.delta", "response.reasoning_text.delta" -> {
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeClaudeContentDelta(outputIndex, "thinking_delta", "thinking", event.path("delta").asText(""), state, clientBody);
                state.thinkingDeltaIndexes.add(outputIndex);
            }
            case "response.refusal.delta" -> {
                state.responseOutputSeen = true;
                state.finalMessageSeen = true;
                ensureClaudeBlockStarted(outputIndex, "text", null, null, state, clientBody);
                writeClaudeContentDelta(outputIndex, "text_delta", "text", event.path("delta").asText(""), state, clientBody);
                state.textDeltaIndexes.add(outputIndex);
                state.stopReason = "refusal";
            }
            case "response.output_item.added" -> {
                JsonNode item = event.path("item");
                String itemType = item.path("type").asText("");
                markResponsesOutputItem(itemType, state);
                if (ResponsesToolCallBridge.isToolCall(itemType)) {
                    rememberResponsesToolCall(outputIndex, item, state);
                    ensureClaudeBlockStarted(
                            outputIndex,
                            "tool_use",
                            ResponsesToolCallBridge.toClaudeToolUseId(item),
                            item.path("name").asText(""),
                            item.get("caller"),
                            state,
                            clientBody
                    );
                    state.toolCallSeen = true;
                } else if ("reasoning".equals(itemType)) {
                    ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                }
            }
            case "response.function_call_arguments.delta", "response.custom_tool_call_input.delta" ->
                    handleResponsesToolInputDelta(event, outputIndex, state, clientBody);
            case "response.function_call_arguments.done", "response.custom_tool_call_input.done" ->
                    handleResponsesToolInputDone(event, outputIndex, state, clientBody);
            case "response.output_text.done" -> writeResponsesTextFallback(
                    outputIndex, event.path("text").asText(""), false, state, clientBody);
            case "response.refusal.done" -> writeResponsesTextFallback(
                    outputIndex,
                    event.path("refusal").asText(event.path("text").asText("")),
                    true,
                    state,
                    clientBody
            );
            case "response.content_part.done" -> handleResponsesContentPartDone(
                    event.path("part"), outputIndex, state, clientBody);
            case "response.reasoning_summary_text.done", "response.reasoning_text.done" ->
                    writeResponsesThinkingFallback(
                            outputIndex, event.path("text").asText(""), state, clientBody);
            case "response.reasoning_summary_part.added", "response.reasoning_summary_part.done" -> {
                JsonNode part = event.path("part");
                if ("summary_text".equals(part.path("type").asText(""))) {
                    writeResponsesThinkingFallback(
                            outputIndex, part.path("text").asText(""), state, clientBody);
                }
            }
            case "response.output_item.done" -> handleResponsesOutputItemDone(
                    event.path("item"), outputIndex, state, clientBody);
            case "response.completed", "response.done", "response.incomplete" ->
                    recordResponsesCompletion(event.path("response"), state, clientBody);
            case "response.failed", "response.cancelled", "response.canceled" ->
                    throw responsesStreamFailure(event);
            case "error", "response.error" -> throw new IOException("OpenAI Responses stream failed: "
                    + event.path("error").path("message").asText(event.path("message").asText("unknown error")));
            default -> {
            }
        }
    }

    private void rememberResponsesToolCall(int outputIndex, JsonNode item, ResponsesStreamState state) {
        state.toolNames.put(outputIndex, item.path("name").asText(""));
    }

    private void markResponsesOutputItem(String itemType, ResponsesStreamState state) {
        if (itemType == null || itemType.isBlank()) {
            return;
        }
        state.responseOutputSeen = true;
        if ("message".equals(itemType)) {
            state.finalMessageSeen = true;
        }
    }

    private void handleResponsesToolInputDelta(
            JsonNode event,
            int outputIndex,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        boolean custom = "response.custom_tool_call_input.delta".equals(event.path("type").asText(""));
        String name = event.path("name").asText(state.toolNames.getOrDefault(outputIndex, ""));
        String callId = event.path("call_id").asText(event.path("item_id").asText(""));
        ObjectNode item = responsesToolItem(custom, callId, name);
        rememberResponsesToolCall(outputIndex, item, state);
        String delta = event.path("delta").asText("");
        state.toolInputBuffers.computeIfAbsent(outputIndex, ignored -> new StringBuilder()).append(delta);
        state.toolCallSeen = true;
        if (name.isBlank()) {
            return;
        }
        ensureClaudeBlockStarted(
                outputIndex,
                "tool_use",
                ResponsesToolCallBridge.toClaudeToolUseId(item),
                name,
                event.get("caller"),
                state,
                clientBody
        );
        if (!custom && !name.isBlank() && !"Read".equals(name) && !delta.isEmpty()) {
            writeClaudeContentDelta(
                    outputIndex, "input_json_delta", "partial_json", delta, state, clientBody);
            state.toolInputDeltaIndexes.add(outputIndex);
        }
    }

    private void handleResponsesToolInputDone(
            JsonNode event,
            int outputIndex,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        boolean custom = event.path("type").asText("").contains("custom_tool_call");
        String name = event.path("name").asText(state.toolNames.getOrDefault(outputIndex, ""));
        String callId = event.path("call_id").asText(event.path("item_id").asText(""));
        ObjectNode item = responsesToolItem(custom, callId, name);
        rememberResponsesToolCall(outputIndex, item, state);
        ensureClaudeBlockStarted(
                outputIndex,
                "tool_use",
                ResponsesToolCallBridge.toClaudeToolUseId(item),
                name,
                event.get("caller"),
                state,
                clientBody
        );
        String rawInput = custom
                ? event.path("input").asText("")
                : event.path("arguments").asText("");
        emitCompletedResponsesToolInput(outputIndex, name, rawInput, custom, state, clientBody);
    }

    private void emitCompletedResponsesToolInput(
            int outputIndex,
            String name,
            String rawInput,
            boolean custom,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        if (state.toolInputCompletedIndexes.contains(outputIndex)) {
            return;
        }
        String buffered = state.toolInputBuffers.getOrDefault(outputIndex, new StringBuilder()).toString();
        String completeInput = rawInput == null || rawInput.isBlank() ? buffered : rawInput;
        try {
            String sanitized = ResponsesToolCallBridge.toClaudeToolInputJson(
                    objectMapper, name, completeInput, custom);
            if (custom || "Read".equals(name) || !state.toolInputDeltaIndexes.contains(outputIndex)) {
                writeClaudeContentDelta(
                        outputIndex, "input_json_delta", "partial_json", sanitized, state, clientBody);
            }
        } catch (ProtocolConversionException exception) {
            throw new IOException("OpenAI Responses tool input cannot be converted to Claude", exception);
        }
        state.toolInputCompletedIndexes.add(outputIndex);
    }

    private ObjectNode responsesToolItem(boolean custom, String callId, String name) {
        ObjectNode item = objectNode();
        item.put("type", custom ? "custom_tool_call" : "function_call");
        item.put("call_id", callId);
        item.put("name", name);
        return item;
    }

    private void handleResponsesContentPartDone(
            JsonNode part,
            int outputIndex,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        String partType = part.path("type").asText("");
        if ("output_text".equals(partType)) {
            writeResponsesTextFallback(outputIndex, part.path("text").asText(""), false, state, clientBody);
        } else if ("refusal".equals(partType)) {
            writeResponsesTextFallback(
                    outputIndex,
                    part.path("refusal").asText(part.path("text").asText("")),
                    true,
                    state,
                    clientBody
            );
        }
    }

    private void writeResponsesTextFallback(
            int outputIndex,
            String text,
            boolean refusal,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        state.responseOutputSeen = true;
        state.finalMessageSeen = true;
        if (text == null || text.isEmpty() || state.textDeltaIndexes.contains(outputIndex)) {
            if (refusal) {
                state.stopReason = "refusal";
            }
            return;
        }
        ensureClaudeBlockStarted(outputIndex, "text", null, null, state, clientBody);
        writeClaudeContentDelta(outputIndex, "text_delta", "text", text, state, clientBody);
        state.textDeltaIndexes.add(outputIndex);
        if (refusal) {
            state.stopReason = "refusal";
        }
    }

    private void writeResponsesThinkingFallback(
            int outputIndex,
            String thinking,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        if (thinking == null || thinking.isEmpty() || state.thinkingDeltaIndexes.contains(outputIndex)) {
            return;
        }
        ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
        writeClaudeContentDelta(
                outputIndex, "thinking_delta", "thinking", thinking, state, clientBody);
        state.thinkingDeltaIndexes.add(outputIndex);
    }

    private void handleResponsesOutputItemDone(
            JsonNode item,
            int outputIndex,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        String itemType = item.path("type").asText("");
        markResponsesOutputItem(itemType, state);
        state.completedOutputIndexes.add(outputIndex);
        try {
            if (ResponsesToolCallBridge.isToolCall(itemType)) {
                rememberResponsesToolCall(outputIndex, item, state);
                ensureClaudeBlockStarted(
                        outputIndex,
                        "tool_use",
                        ResponsesToolCallBridge.toClaudeToolUseId(item),
                        item.path("name").asText(""),
                        item.get("caller"),
                        state,
                        clientBody
                );
                String rawInput = "custom_tool_call".equals(itemType)
                        ? item.path("input").asText("")
                        : item.path("arguments").asText("");
                emitCompletedResponsesToolInput(
                        outputIndex,
                        item.path("name").asText(""),
                        rawInput,
                        "custom_tool_call".equals(itemType),
                        state,
                        clientBody
                );
                state.toolCallSeen = true;
            } else if ("reasoning".equals(itemType)) {
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeResponsesThinkingFallback(
                        outputIndex, RESPONSES_OPAQUE_STATE_PLACEHOLDER, state, clientBody);
                String signature = ResponsesReasoningBridge.encode(objectMapper, item)
                        .orElseThrow(() -> new IOException(
                                "OpenAI Responses reasoning item is missing encrypted state"));
                writeClaudeContentDelta(
                        outputIndex, "signature_delta", "signature", signature, state, clientBody);
            } else if ("compaction".equals(itemType)) {
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeResponsesThinkingFallback(
                        outputIndex, RESPONSES_COMPACTION_PLACEHOLDER, state, clientBody);
                String signature = ResponsesReasoningBridge.encodeItem(objectMapper, item)
                        .orElseThrow(() -> new IOException(
                                "OpenAI Responses compaction item is missing state"));
                writeClaudeContentDelta(
                        outputIndex, "signature_delta", "signature", signature, state, clientBody);
            } else if ("program".equals(itemType)) {
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeResponsesThinkingFallback(
                        outputIndex, RESPONSES_OPAQUE_STATE_PLACEHOLDER, state, clientBody);
                String signature = ResponsesReasoningBridge.encodeItem(objectMapper, item)
                        .orElseThrow(() -> new IOException(
                                "OpenAI Responses program item is missing state"));
                writeClaudeContentDelta(
                        outputIndex, "signature_delta", "signature", signature, state, clientBody);
                stopClaudeBlock(outputIndex, state, clientBody);
                writeClaudeProgramServerTool(item, state, clientBody);
                return;
            } else if ("program_output".equals(itemType)) {
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeResponsesThinkingFallback(
                        outputIndex, RESPONSES_OPAQUE_STATE_PLACEHOLDER, state, clientBody);
                String signature = ResponsesReasoningBridge.encodeItem(objectMapper, item)
                        .orElseThrow(() -> new IOException(
                                "OpenAI Responses program output item is missing state"));
                writeClaudeContentDelta(
                        outputIndex, "signature_delta", "signature", signature, state, clientBody);
                stopClaudeBlock(outputIndex, state, clientBody);
                writeClaudeProgramResult(item, state, clientBody);
                return;
            } else if (!"message".equals(itemType) && !itemType.isBlank()) {
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeResponsesThinkingFallback(
                        outputIndex, RESPONSES_OPAQUE_STATE_PLACEHOLDER, state, clientBody);
                String signature = ResponsesReasoningBridge.encodeItem(objectMapper, item)
                        .orElseThrow(() -> new IOException(
                                "OpenAI Responses output item is missing state"));
                writeClaudeContentDelta(
                        outputIndex, "signature_delta", "signature", signature, state, clientBody);
            }
        } catch (ProtocolConversionException exception) {
            throw new IOException("OpenAI Responses output item cannot be converted to Claude", exception);
        }
        stopClaudeBlock(outputIndex, state, clientBody);
    }

    private void writeClaudeProgramServerTool(
            JsonNode item,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        int index = state.nextAuxiliaryClaudeIndex();
        String toolId = ResponsesProgrammaticToolBridge.toClaudeProgramToolId(
                item.path("call_id").asText(""));
        ObjectNode contentBlock = objectNode();
        contentBlock.put("type", "server_tool_use");
        contentBlock.put("id", toolId);
        contentBlock.put("name", "code_execution");
        contentBlock.set("input", objectNode());
        writeClaudeAuxiliaryBlockStart(index, contentBlock, clientBody);

        ObjectNode input = objectNode();
        input.put("code", item.path("code").asText(""));
        ObjectNode delta = objectNode();
        delta.put("type", "input_json_delta");
        delta.put("partial_json", objectMapper.writeValueAsString(input));
        ObjectNode event = objectNode();
        event.put("type", "content_block_delta");
        event.put("index", index);
        event.set("delta", delta);
        writeSse(clientBody, "content_block_delta", event);
        writeClaudeAuxiliaryBlockStop(index, clientBody);
    }

    private void writeClaudeProgramResult(
            JsonNode item,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        int index = state.nextAuxiliaryClaudeIndex();
        boolean completed = "completed".equals(item.path("status").asText(""));
        ObjectNode contentBlock = objectNode();
        contentBlock.put("type", "code_execution_tool_result");
        contentBlock.put(
                "tool_use_id",
                ResponsesProgrammaticToolBridge.toClaudeProgramToolId(
                        item.path("call_id").asText(""))
        );
        ObjectNode result = objectNode();
        result.put("type", "code_execution_result");
        result.put("stdout", item.path("result").asText(""));
        result.put("stderr", completed ? "" : "Program did not complete.");
        result.put("return_code", completed ? 0 : 1);
        result.set("content", objectMapper.createArrayNode());
        contentBlock.set("content", result);
        writeClaudeAuxiliaryBlockStart(index, contentBlock, clientBody);
        writeClaudeAuxiliaryBlockStop(index, clientBody);
    }

    private void writeClaudeAuxiliaryBlockStart(
            int index,
            ObjectNode contentBlock,
            OutputStream clientBody
    ) throws IOException {
        ObjectNode event = objectNode();
        event.put("type", "content_block_start");
        event.put("index", index);
        event.set("content_block", contentBlock);
        writeSse(clientBody, "content_block_start", event);
    }

    private void writeClaudeAuxiliaryBlockStop(int index, OutputStream clientBody) throws IOException {
        ObjectNode event = objectNode();
        event.put("type", "content_block_stop");
        event.put("index", index);
        writeSse(clientBody, "content_block_stop", event);
    }

    private IOException responsesStreamFailure(JsonNode event) {
        JsonNode response = event.path("response");
        String message = response.path("error").path("message").asText("");
        if (message.isBlank()) {
            message = event.path("error").path("message").asText(event.path("message").asText("unknown error"));
        }
        return new IOException("OpenAI Responses stream failed: " + message);
    }

    private void writeClaudeMessageStart(OutputStream clientBody, ResponsesStreamState state) throws IOException {
        ObjectNode message = objectNode();
        message.put("id", "msg_api2api_responses_stream");
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("model", state.clientModel);
        message.set("content", objectMapper.createArrayNode());
        message.putNull("stop_reason");
        message.putNull("stop_sequence");
        message.set("usage", objectNode().put("input_tokens", 0).put("output_tokens", 0));
        ObjectNode event = objectNode();
        event.put("type", "message_start");
        event.set("message", message);
        writeSse(clientBody, "message_start", event);
        state.messageStarted = true;
    }

    private void ensureClaudeBlockStarted(int index, String type, String id, String name,
                                          ResponsesStreamState state, OutputStream clientBody) throws IOException {
        ensureClaudeBlockStarted(index, type, id, name, null, state, clientBody);
    }

    private void ensureClaudeBlockStarted(
            int index,
            String type,
            String id,
            String name,
            JsonNode caller,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        int claudeIndex = state.claudeIndexFor(index);
        if (state.blockTypes.containsKey(claudeIndex)) {
            return;
        }
        ObjectNode block = objectNode();
        block.put("type", type);
        if ("text".equals(type)) {
            block.put("text", "");
        } else if ("thinking".equals(type)) {
            block.put("thinking", "");
        } else {
            block.put("id", id == null ? "" : id);
            block.put("name", name == null ? "" : name);
            block.set("input", objectNode());
            try {
                ObjectNode claudeCaller = ResponsesProgrammaticToolBridge.toClaudeCaller(
                        objectMapper, caller);
                if (claudeCaller != null) {
                    block.set("caller", claudeCaller);
                }
            } catch (ProtocolConversionException exception) {
                throw new IOException("OpenAI Responses tool caller cannot be converted to Claude", exception);
            }
        }
        ObjectNode event = objectNode();
        event.put("type", "content_block_start");
        event.put("index", claudeIndex);
        event.set("content_block", block);
        writeSse(clientBody, "content_block_start", event);
        state.blockTypes.put(claudeIndex, type);
    }

    private void writeClaudeContentDelta(int index, String deltaType, String valueField, String value,
                                         ResponsesStreamState state, OutputStream clientBody) throws IOException {
        ObjectNode delta = objectNode();
        delta.put("type", deltaType);
        delta.put(valueField, value);
        ObjectNode event = objectNode();
        event.put("type", "content_block_delta");
        event.put("index", state.claudeIndexFor(index));
        event.set("delta", delta);
        writeSse(clientBody, "content_block_delta", event);
    }

    private void stopClaudeBlock(int index, ResponsesStreamState state, OutputStream clientBody) throws IOException {
        Integer claudeIndex = state.claudeIndexes.get(index);
        if (claudeIndex == null) {
            return;
        }
        if (!state.blockTypes.containsKey(claudeIndex) || state.stoppedBlocks.containsKey(claudeIndex)) {
            return;
        }
        ObjectNode event = objectNode();
        event.put("type", "content_block_stop");
        event.put("index", claudeIndex);
        writeSse(clientBody, "content_block_stop", event);
        state.stoppedBlocks.put(claudeIndex, true);
    }

    private void recordResponsesCompletion(
            JsonNode response,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        String status = response.path("status").asText("");
        if ("failed".equals(status) || "cancelled".equals(status) || "canceled".equals(status)) {
            String message = response.path("error").path("message").asText("unknown error");
            throw new IOException("OpenAI Responses stream failed: " + message);
        }
        JsonNode usage = response.path("usage");
        if (!usage.isMissingNode() && !usage.isNull()) {
            long input = usage.path("input_tokens").asLong(0);
            long output = usage.path("output_tokens").asLong(0);
            long cached = usage.path("input_tokens_details").path("cached_tokens").asLong(0);
            long cacheWrite = usage.path("input_tokens_details").path("cache_write_tokens").asLong(0);
            state.usage = UnifiedTokenUsage.known(
                    Math.max(0, input - cached - cacheWrite), output, cacheWrite, cached);
        }
        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (int outputIndex = 0; outputIndex < output.size(); outputIndex++) {
                JsonNode item = output.get(outputIndex);
                markResponsesOutputItem(item.path("type").asText(""), state);
                if (!state.completedOutputIndexes.contains(outputIndex)) {
                    handleResponsesCompletionFallbackItem(item, outputIndex, state, clientBody);
                }
            }
        }
        if ("incomplete".equals(status)
                && "max_output_tokens".equals(response.path("incomplete_details").path("reason").asText())) {
            state.stopReason = "max_tokens";
        } else if ("content_filter".equals(response.path("incomplete_details").path("reason").asText())) {
            state.stopReason = "refusal";
        } else if (state.responseOutputSeen && !state.finalMessageSeen && !state.toolCallSeen) {
            state.stopReason = "pause_turn";
        }
        state.terminalEventSeen = true;
    }

    private void handleResponsesCompletionFallbackItem(
            JsonNode item,
            int outputIndex,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        if (!"message".equals(item.path("type").asText(""))) {
            handleResponsesOutputItemDone(item, outputIndex, state, clientBody);
            return;
        }
        JsonNode content = item.path("content");
        if (content.isArray()) {
            for (JsonNode part : content) {
                handleResponsesContentPartDone(part, outputIndex, state, clientBody);
            }
        }
        state.completedOutputIndexes.add(outputIndex);
        stopClaudeBlock(outputIndex, state, clientBody);
    }

    private void throwIfModeledException(BedrockEvent event, JsonNode payload) throws IOException {
        if (!"exception".equals(event.messageType())) {
            return;
        }
        String exceptionType = event.exceptionType().isBlank() ? "unknownException" : event.exceptionType();
        throw new IOException("Bedrock Converse stream failed: " + exceptionType + streamErrorMessage(payload));
    }

    private String streamErrorMessage(JsonNode payload) {
        String message = payload == null ? "" : payload.path("message").asText("");
        return message.isBlank() ? "" : " - " + message;
    }

    private void finishResponsesToClaude(ResponsesStreamState state, OutputStream clientBody) throws IOException {
        for (Integer index : state.claudeIndexes.keySet()) {
            stopClaudeBlock(index, state, clientBody);
        }
        ObjectNode event = objectNode();
        event.put("type", "message_delta");
        ObjectNode delta = objectNode();
        delta.put("stop_reason", state.toolCallSeen ? "tool_use" : state.stopReason);
        delta.putNull("stop_sequence");
        event.set("delta", delta);
        if (state.usage != null && state.usage.usageKnown()) {
            event.set("usage", claudeUsage(state.usage));
        }
        writeSse(clientBody, "message_delta", event);
        writeSse(clientBody, "message_stop", objectNode().put("type", "message_stop"));
    }

    private UnifiedTokenUsage extractUsage(JsonNode usage) {
        if (usage == null || usage.isMissingNode() || usage.isNull()) {
            return UnifiedTokenUsage.unknown();
        }
        long inputTokens = usage.path("inputTokens").asLong(0);
        long outputTokens = usage.path("outputTokens").asLong(0);
        long cacheWriteInputTokens = usage.path("cacheWriteInputTokens").asLong(0);
        long cacheReadInputTokens = usage.path("cacheReadInputTokens").asLong(0);
        return UnifiedTokenUsage.known(inputTokens, outputTokens, cacheWriteInputTokens, cacheReadInputTokens);
    }

    private JsonNode reasoningTextNode(JsonNode node) {
        JsonNode reasoningContent = node.path("reasoningContent");
        if (reasoningContent.isMissingNode() || reasoningContent.isNull()) {
            return null;
        }
        JsonNode reasoningText = reasoningContent.path("reasoningText");
        if (!reasoningText.isMissingNode()) {
            return reasoningText;
        }
        return reasoningContent.has("text") || reasoningContent.has("signature") ? reasoningContent : null;
    }

    private String mapBedrockStopToClaudeStop(String stopReason) throws IOException {
        return switch (stopReason) {
            case "end_turn" -> "end_turn";
            case "max_tokens" -> "max_tokens";
            case "tool_use" -> "tool_use";
            case "stop_sequence" -> "stop_sequence";
            case "model_context_window_exceeded" -> "model_context_window_exceeded";
            case "guardrail_intervened", "content_filtered" -> "refusal";
            case "malformed_model_output", "malformed_tool_use" ->
                    throw new IOException("Bedrock Converse stopped with invalid model output: " + stopReason);
            default -> throw new IOException("Unsupported Bedrock Converse stop reason: " + stopReason);
        };
    }

    private void writeSse(OutputStream outputStream, String eventName, JsonNode data) throws IOException {
        outputStream.write(("event: " + eventName + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("data: " + objectMapper.writeValueAsString(data) + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private ObjectNode objectNode() {
        return objectMapper.createObjectNode();
    }

    private BedrockEvent readEvent(InputStream inputStream) throws IOException {
        DataInputStream dataInput = new DataInputStream(inputStream);
        int totalLength;
        try {
            totalLength = dataInput.readInt();
        } catch (EOFException exception) {
            return null;
        }
        int headersLength = dataInput.readInt();
        dataInput.readInt(); // prelude CRC
        if (totalLength < EVENT_STREAM_OVERHEAD_BYTES || headersLength < 0) {
            throw new IOException("Invalid Bedrock event-stream frame length");
        }
        byte[] headers = dataInput.readNBytes(headersLength);
        int payloadLength = totalLength - EVENT_STREAM_OVERHEAD_BYTES - headersLength;
        if (payloadLength < 0) {
            throw new IOException("Invalid Bedrock event-stream payload length");
        }
        byte[] payload = dataInput.readNBytes(payloadLength);
        dataInput.readInt(); // message CRC
        BedrockEventHeaders eventHeaders = parseHeaders(headers);
        return new BedrockEvent(
                eventHeaders.eventType(),
                eventHeaders.messageType(),
                eventHeaders.exceptionType(),
                payload
        );
    }

    private BedrockEventHeaders parseHeaders(byte[] headers) throws IOException {
        DataInputStream input = new DataInputStream(new java.io.ByteArrayInputStream(headers));
        String eventType = "";
        String messageType = "";
        String exceptionType = "";
        while (input.available() > 0) {
            int nameLength = input.readUnsignedByte();
            String name = new String(input.readNBytes(nameLength), StandardCharsets.UTF_8);
            int type = input.readUnsignedByte();
            String value = readHeaderValue(input, type);
            switch (name) {
                case ":event-type" -> eventType = value;
                case ":message-type" -> messageType = value;
                case ":exception-type" -> exceptionType = value;
                default -> {
                }
            }
        }
        return new BedrockEventHeaders(eventType, messageType, exceptionType);
    }

    private String readHeaderValue(DataInputStream input, int type) throws IOException {
        return switch (type) {
            case 0, 1 -> "";
            case 2 -> {
                input.readByte();
                yield "";
            }
            case 3 -> {
                input.readShort();
                yield "";
            }
            case 4 -> {
                input.readInt();
                yield "";
            }
            case 5, 8 -> {
                input.readLong();
                yield "";
            }
            case 6 -> {
                int length = input.readUnsignedShort();
                input.readNBytes(length);
                yield "";
            }
            case 7 -> {
                int length = input.readUnsignedShort();
                yield new String(input.readNBytes(length), StandardCharsets.UTF_8);
            }
            case 9 -> {
                input.readNBytes(16);
                yield "";
            }
            default -> throw new IOException("Unsupported Bedrock event-stream header type: " + type);
        };
    }

    private record BedrockEventHeaders(String eventType, String messageType, String exceptionType) {
    }

    private record BedrockEvent(String eventType, String messageType, String exceptionType, byte[] payload) {
    }

    private static final class StreamState {
        private final ProtocolType clientProtocol;
        private final String clientModel;
        private boolean messageStarted;
        private boolean messageStopped;
        private boolean upstreamMessageStopped;
        private String stopReason = "end_turn";
        private String stopSequence;
        private UnifiedTokenUsage usage;
        private final Map<Integer, String> blockTypes = new HashMap<>();
        private final Map<Integer, Boolean> stoppedBlocks = new HashMap<>();

        private StreamState(ProtocolType clientProtocol, String clientModel) {
            this.clientProtocol = clientProtocol;
            this.clientModel = clientModel;
        }
    }

    private static final class ResponsesStreamState {
        private final String clientModel;
        private boolean messageStarted;
        private boolean toolCallSeen;
        private boolean terminalEventSeen;
        private boolean responseOutputSeen;
        private boolean finalMessageSeen;
        private String stopReason = "end_turn";
        private UnifiedTokenUsage usage;
        private int nextClaudeIndex;
        private final Map<Integer, String> blockTypes = new HashMap<>();
        private final Map<Integer, Boolean> stoppedBlocks = new HashMap<>();
        private final Map<Integer, Integer> claudeIndexes = new HashMap<>();
        private final Map<Integer, String> toolNames = new HashMap<>();
        private final Map<Integer, StringBuilder> toolInputBuffers = new HashMap<>();
        private final Set<Integer> toolInputDeltaIndexes = new HashSet<>();
        private final Set<Integer> toolInputCompletedIndexes = new HashSet<>();
        private final Set<Integer> textDeltaIndexes = new HashSet<>();
        private final Set<Integer> thinkingDeltaIndexes = new HashSet<>();
        private final Set<Integer> completedOutputIndexes = new HashSet<>();

        private ResponsesStreamState(String clientModel) {
            this.clientModel = clientModel;
        }

        private int claudeIndexFor(int responseOutputIndex) {
            Integer existing = claudeIndexes.get(responseOutputIndex);
            if (existing != null) {
                return existing;
            }
            int assigned = nextClaudeIndex++;
            claudeIndexes.put(responseOutputIndex, assigned);
            return assigned;
        }

        private int nextAuxiliaryClaudeIndex() {
            return nextClaudeIndex++;
        }
    }
}
