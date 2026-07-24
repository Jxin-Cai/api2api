package com.api2api.infr.protocol;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRouteContext;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Unified streaming conversion adapter handling all gateway streaming protocol pairs:
 * <ul>
 *   <li>AWS_BEDROCK_CONVERSE -> CLAUDE_MESSAGES / OPENAI_RESPONSES</li>
 *   <li>OPENAI_RESPONSES -> CLAUDE_MESSAGES</li>
 *   <li>OPENAI_CHAT_COMPLETIONS -> CLAUDE_MESSAGES / OPENAI_RESPONSES</li>
 *   <li>CLAUDE_MESSAGES -> OPENAI_CHAT_COMPLETIONS</li>
 * </ul>
 */
@Component
public class UnifiedStreamingConversionAdapter implements GatewayStreamingConversionPort {

    private static final Logger log = LoggerFactory.getLogger(UnifiedStreamingConversionAdapter.class);
    private static final int EVENT_STREAM_OVERHEAD_BYTES = 16;
    private static final String RESPONSES_OPAQUE_STATE_PLACEHOLDER = ResponsesProtocolConstants.OPAQUE_STATE_PLACEHOLDER;
    private static final String RESPONSES_COMPACTION_PLACEHOLDER = ResponsesProtocolConstants.COMPACTION_PLACEHOLDER;
    private static final String RESPONSES_COMPACTION_VISIBLE_TEXT = ResponsesProtocolConstants.COMPACTION_VISIBLE_TEXT;

    private final ObjectMapper objectMapper;
    public UnifiedStreamingConversionAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public boolean supports(ProtocolType upstreamProtocol, ProtocolType clientProtocol) {
        return (upstreamProtocol == ProtocolType.AWS_BEDROCK_CONVERSE
                && (clientProtocol == ProtocolType.CLAUDE_MESSAGES || clientProtocol == ProtocolType.OPENAI_RESPONSES))
                || (upstreamProtocol == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
                && clientProtocol == ProtocolType.CLAUDE_MESSAGES)
                || (upstreamProtocol == ProtocolType.OPENAI_RESPONSES
                && clientProtocol == ProtocolType.CLAUDE_MESSAGES)
                || (upstreamProtocol == ProtocolType.OPENAI_CHAT_COMPLETIONS
                && (clientProtocol == ProtocolType.CLAUDE_MESSAGES || clientProtocol == ProtocolType.OPENAI_RESPONSES))
                || (upstreamProtocol == ProtocolType.CLAUDE_MESSAGES
                && clientProtocol == ProtocolType.OPENAI_CHAT_COMPLETIONS);
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
        if (upstreamProtocol == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            return transformBedrockInvokeModelToClaude(upstreamBody, clientBody);
        }
        if (upstreamProtocol == ProtocolType.OPENAI_RESPONSES) {
            return transformResponsesToClaude(context.clientModel().value(), upstreamBody, clientBody);
        }
        if (upstreamProtocol == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return transformChatToClaude(context.clientModel().value(), upstreamBody, clientBody, clientProtocol);
        }
        if (upstreamProtocol == ProtocolType.CLAUDE_MESSAGES && clientProtocol == ProtocolType.OPENAI_CHAT_COMPLETIONS) {
            return transformClaudeToChat(context.clientModel().value(), upstreamBody, clientBody);
        }
        ProtocolConversionRouteContext routeContext = new ProtocolConversionRouteContext(
                context.providerChannelId().value(), context.upstreamModel().value());
        StreamState state = new StreamState(clientProtocol, context.clientModel().value(), routeContext);
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
        state.stopReason = requiredBedrockStopReason(payload);
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
            response.put("status", mapBedrockStopToResponsesStatus(state.stopReason));
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
            String bedrockToolName = toolUse.path("name").asText("");
            ObjectNode contentBlock = objectNode();
            contentBlock.put("type", "tool_use");
            contentBlock.put("id", toolUse.path("toolUseId").asText(""));
            contentBlock.put("name", BedrockToolSchemaAdapter.toClaudeToolName(bedrockToolName));
            contentBlock.set("input", objectNode());
            if (BedrockToolSchemaAdapter.isAdaptedToolName(bedrockToolName)) {
                state.wrappedToolInputBuffers.put(index, new StringBuilder());
            }
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
                state.reasoningSignatureBuffers
                        .computeIfAbsent(index, ignored -> new StringBuilder())
                        .append(signature.asText());
            }
            return;
        }
        JsonNode toolUse = delta.path("toolUse");
        if (!toolUse.isMissingNode()) {
            ensureContentBlockStarted(index, "tool_use", state, clientBody);
            String partialJson = toolUse.path("input").isTextual()
                    ? toolUse.path("input").asText("")
                    : toolUse.path("input").toString();
            StringBuilder wrappedInput = state.wrappedToolInputBuffers.get(index);
            if (wrappedInput != null) {
                wrappedInput.append(partialJson);
                return;
            }
            writeClaudeToolInputDelta(index, partialJson, clientBody);
        }
    }

    private void writeClaudeToolInputDelta(int index, String partialJson, OutputStream clientBody) throws IOException {
        ObjectNode event = objectNode();
        event.put("type", "content_block_delta");
        event.put("index", index);
        ObjectNode inputDelta = objectNode();
        inputDelta.put("type", "input_json_delta");
        inputDelta.put("partial_json", partialJson);
        event.set("delta", inputDelta);
        writeSse(clientBody, "content_block_delta", event);
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
            StringBuilder wrappedToolInput = state.wrappedToolInputBuffers.remove(index);
            if (wrappedToolInput != null) {
                JsonNode wrappedInput;
                try {
                    wrappedInput = objectMapper.readTree(wrappedToolInput.toString());
                    JsonNode unwrappedInput = BedrockToolSchemaAdapter.unwrapInput(wrappedInput);
                    writeClaudeToolInputDelta(index, objectMapper.writeValueAsString(unwrappedInput), clientBody);
                } catch (JsonProcessingException | ProtocolConversionException exception) {
                    throw new IOException("Malformed wrapped Bedrock tool input", exception);
                }
            }
            StringBuilder reasoningSignature = state.reasoningSignatureBuffers.remove(index);
            if (reasoningSignature != null && !reasoningSignature.isEmpty()) {
                ObjectNode signatureEvent = objectNode();
                signatureEvent.put("type", "content_block_delta");
                signatureEvent.put("index", index);
                ObjectNode signatureDelta = objectNode();
                signatureDelta.put("type", "signature_delta");
                signatureDelta.put("signature", BedrockReasoningBridge.encode(
                        reasoningSignature.toString(), state.routeContext));
                signatureEvent.set("delta", signatureDelta);
                writeSse(clientBody, "content_block_delta", signatureEvent);
            }
            ObjectNode event = objectNode();
            event.put("type", "content_block_stop");
            event.put("index", index);
            writeSse(clientBody, "content_block_stop", event);
            state.stoppedBlocks.put(index, true);
        }
    }

    private void writeMessageStop(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        state.stopReason = mapBedrockStopToClaudeStop(requiredBedrockStopReason(payload));
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
                if (state.terminalEventSeen) {
                    break;
                }
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
                } else if (isResponsesCompactionType(itemType)) {
                    state.addedCompactionItems.put(outputIndex, item.deepCopy());
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
        if (isResponsesCompactionType(itemType) && state.completedOutputIndexes.contains(outputIndex)) {
            return;
        }
        if (isResponsesCompactionType(itemType)) {
            state.addedCompactionItems.remove(outputIndex);
        }
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
            } else if (isResponsesCompactionType(itemType)) {
                ObjectNode normalizedItem = (ObjectNode) item.deepCopy();
                normalizedItem.put("type", "compaction");
                ensureClaudeBlockStarted(outputIndex, "thinking", null, null, state, clientBody);
                writeResponsesThinkingFallback(
                        outputIndex, RESPONSES_COMPACTION_PLACEHOLDER, state, clientBody);
                String signature = ResponsesReasoningBridge.encodeItem(objectMapper, normalizedItem)
                        .orElseThrow(() -> new IOException(
                                "OpenAI Responses compaction item is missing state"));
                writeClaudeContentDelta(
                        outputIndex, "signature_delta", "signature", signature, state, clientBody);
                stopClaudeBlock(outputIndex, state, clientBody);
                writeClaudeAuxiliaryText(compactionVisibleText(item), state, clientBody);
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

    private boolean isResponsesCompactionType(String type) {
        return ResponsesProtocolConstants.isCompactionType(type);
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

    private void writeClaudeAuxiliaryText(
            String text,
            ResponsesStreamState state,
            OutputStream clientBody
    ) throws IOException {
        int index = state.nextAuxiliaryClaudeIndex();
        ObjectNode contentBlock = objectNode();
        contentBlock.put("type", "text");
        contentBlock.put("text", "");
        writeClaudeAuxiliaryBlockStart(index, contentBlock, clientBody);
        ObjectNode delta = objectNode();
        delta.put("type", "text_delta");
        delta.put("text", text);
        ObjectNode event = objectNode();
        event.put("type", "content_block_delta");
        event.put("index", index);
        event.set("delta", delta);
        writeSse(clientBody, "content_block_delta", event);
        writeClaudeAuxiliaryBlockStop(index, clientBody);
    }

    private String compactionVisibleText(JsonNode item) {
        JsonNode summary = item.path("summary");
        if (summary.isArray()) {
            for (JsonNode part : summary) {
                String text = part.path("text").asText("");
                if ("summary_text".equals(part.path("type").asText("")) && !text.isBlank()) {
                    return text;
                }
            }
        }
        return RESPONSES_COMPACTION_VISIBLE_TEXT;
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
            state.usage = OpenAIResponsesUsageExtractor.extractUsage(usage);
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
        for (Map.Entry<Integer, JsonNode> entry : new HashMap<>(state.addedCompactionItems).entrySet()) {
            if (!state.completedOutputIndexes.contains(entry.getKey())) {
                handleResponsesOutputItemDone(entry.getValue(), entry.getKey(), state, clientBody);
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

    static void throwIfModeledException(BedrockEvent event, JsonNode payload) throws IOException {
        if (!"exception".equals(event.messageType())) {
            return;
        }
        String exceptionType = event.exceptionType().isBlank() ? "unknownException" : event.exceptionType();
        throw new IOException("Bedrock Converse stream failed: " + exceptionType + streamErrorMessage(payload));
    }

    private static String streamErrorMessage(JsonNode payload) {
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
        return BedrockConverseContentSupport.reasoningTextNode(node);
    }

    private String requiredBedrockStopReason(JsonNode payload) throws IOException {
        try {
            return BedrockConverseContentSupport.requiredStopReason(payload);
        } catch (ProtocolConversionException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private String mapBedrockStopToClaudeStop(String stopReason) throws IOException {
        try {
            return BedrockConverseContentSupport.toClaudeStop(stopReason);
        } catch (ProtocolConversionException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private String mapBedrockStopToResponsesStatus(String stopReason) throws IOException {
        try {
            return BedrockConverseContentSupport.toResponsesStatus(stopReason);
        } catch (ProtocolConversionException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void writeSse(OutputStream outputStream, String eventName, JsonNode data) throws IOException {
        outputStream.write(("event: " + eventName + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("data: " + objectMapper.writeValueAsString(data) + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private ObjectNode objectNode() {
        return objectMapper.createObjectNode();
    }

    static BedrockEvent readEvent(InputStream inputStream) throws IOException {
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

    private static BedrockEventHeaders parseHeaders(byte[] headers) throws IOException {
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

    private static String readHeaderValue(DataInputStream input, int type) throws IOException {
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

    static record BedrockEvent(String eventType, String messageType, String exceptionType, byte[] payload) {
    }

    private static final class StreamState {
        private final ProtocolType clientProtocol;
        private final String clientModel;
        private final ProtocolConversionRouteContext routeContext;
        private boolean messageStarted;
        private boolean messageStopped;
        private boolean upstreamMessageStopped;
        private String stopReason = "end_turn";
        private String stopSequence;
        private UnifiedTokenUsage usage;
        private final Map<Integer, String> blockTypes = new HashMap<>();
        private final Map<Integer, Boolean> stoppedBlocks = new HashMap<>();
        private final Map<Integer, StringBuilder> reasoningSignatureBuffers = new HashMap<>();
        private final Map<Integer, StringBuilder> wrappedToolInputBuffers = new HashMap<>();

        private StreamState(
                ProtocolType clientProtocol,
                String clientModel,
                ProtocolConversionRouteContext routeContext
        ) {
            this.clientProtocol = clientProtocol;
            this.clientModel = clientModel;
            this.routeContext = routeContext;
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
        private final Map<Integer, JsonNode> addedCompactionItems = new HashMap<>();
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

    private static final class ChatToClaudeStreamState {
        private final String model;
        private boolean messageStartSent;
        private int blockIndex;
        private boolean textBlockOpen;
        private boolean reasoningBlockOpen;
        private final Map<Integer, String> toolCallIds = new HashMap<>();
        private final Map<Integer, String> toolCallNames = new HashMap<>();
        private final Map<Integer, Integer> blockToToolIndex = new HashMap<>();
        private final Map<Integer, StringBuilder> pendingToolArguments = new HashMap<>();
        private final Set<Integer> announcedToolCalls = new HashSet<>();
        private final Set<Integer> openToolBlocks = new HashSet<>();
        private long inputTokens;
        private long outputTokens;
        private long cacheCreationInputTokens;
        private long cacheReadInputTokens;
        private String stopReason = "end_turn";

        private ChatToClaudeStreamState(String model) {
            this.model = model;
        }
    }

    private static final class ClaudeToChatStreamState {
        private final String model;
        private final String chatId;
        private final long created;
        private boolean roleSent;
        private int toolCallIndex;
        private final Map<Integer, Integer> blockToToolIndex = new HashMap<>();
        private long inputTokens;
        private long outputTokens;

        private ClaudeToChatStreamState(String model) {
            this.model = model;
            this.chatId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            this.created = Instant.now().getEpochSecond();
        }
    }

    // ==================== Chat ↔ Claude streaming ====================

    private UnifiedTokenUsage transformChatToClaude(String model, InputStream upstreamBody,
                                                     OutputStream clientBody, ProtocolType clientProtocol) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(upstreamBody, StandardCharsets.UTF_8));
        ChatToClaudeStreamState state = new ChatToClaudeStreamState(model);

        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data: ")) continue;
            String data = line.substring(6).trim();
            if ("[DONE]".equals(data)) break;

            JsonNode chunk;
            try {
                chunk = objectMapper.readTree(data);
            } catch (JsonProcessingException e) {
                log.warn("Skipping malformed Chat SSE chunk: {}", data.length() > 200 ? data.substring(0, 200) : data, e);
                continue;
            }

            handleChatChunkForClaude(chunk, state, clientBody);
        }

        // Close any open blocks
        if (state.textBlockOpen || state.reasoningBlockOpen) {
            writeSse(clientBody, "content_block_stop", objectNode().put("type", "content_block_stop").put("index", state.blockIndex));
            state.blockIndex++;
        }
        closeOpenClaudeToolBlocks(state.openToolBlocks, clientBody);

        // Emit message_delta with stop_reason and usage
        ObjectNode msgDelta = objectNode();
        msgDelta.put("type", "message_delta");
        ObjectNode deltaNode = objectNode();
        deltaNode.put("stop_reason", state.stopReason);
        msgDelta.set("delta", deltaNode);
        ObjectNode usageNode = objectNode();
        usageNode.put("input_tokens", state.inputTokens);
        usageNode.put("output_tokens", state.outputTokens);
        if (state.cacheCreationInputTokens > 0) {
            usageNode.put("cache_creation_input_tokens", state.cacheCreationInputTokens);
        }
        if (state.cacheReadInputTokens > 0) {
            usageNode.put("cache_read_input_tokens", state.cacheReadInputTokens);
        }
        msgDelta.set("usage", usageNode);
        writeSse(clientBody, "message_delta", msgDelta);

        // Emit message_stop
        writeSse(clientBody, "message_stop", objectNode().put("type", "message_stop"));

        return UnifiedTokenUsage.known(
                state.inputTokens,
                state.outputTokens,
                state.cacheCreationInputTokens,
                state.cacheReadInputTokens);
    }

    private void handleChatChunkForClaude(JsonNode chunk, ChatToClaudeStreamState state,
                                          OutputStream clientBody) throws IOException {
        JsonNode choice = chunk.path("choices").path(0);
        JsonNode delta = choice.path("delta");
        String finishReason = choice.has("finish_reason") && !choice.get("finish_reason").isNull()
                ? choice.get("finish_reason").asText() : null;

        if (!state.messageStartSent) {
            state.messageStartSent = true;
            String msgId = chunk.path("id").asText("msg_api2api");
            ObjectNode msgStart = objectNode();
            msgStart.put("type", "message_start");
            ObjectNode message = objectNode();
            message.put("id", msgId);
            message.put("type", "message");
            message.put("role", "assistant");
            message.set("content", objectMapper.createArrayNode());
            message.put("model", state.model);
            message.put("stop_reason", (String) null);
            message.set("usage", objectNode().put("input_tokens", 0).put("output_tokens", 0));
            msgStart.set("message", message);
            writeSse(clientBody, "message_start", msgStart);
        }

        // Handle reasoning_content (thinking)
        String reasoningContent = delta.path("reasoning_content").asText(null);
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            closeOpenClaudeToolBlocks(state.openToolBlocks, clientBody);
            if (!state.reasoningBlockOpen) {
                if (state.textBlockOpen) {
                    writeSse(clientBody, "content_block_stop", objectNode()
                            .put("type", "content_block_stop").put("index", state.blockIndex));
                    state.textBlockOpen = false;
                    state.blockIndex++;
                }
                ObjectNode blockStart = objectNode();
                blockStart.put("type", "content_block_start");
                blockStart.put("index", state.blockIndex);
                ObjectNode contentBlock = objectNode();
                contentBlock.put("type", "thinking");
                contentBlock.put("thinking", "");
                blockStart.set("content_block", contentBlock);
                writeSse(clientBody, "content_block_start", blockStart);
                state.reasoningBlockOpen = true;
            }
            ObjectNode blockDelta = objectNode();
            blockDelta.put("type", "content_block_delta");
            blockDelta.put("index", state.blockIndex);
            ObjectNode thinkingDelta = objectNode();
            thinkingDelta.put("type", "thinking_delta");
            thinkingDelta.put("thinking", reasoningContent);
            blockDelta.set("delta", thinkingDelta);
            writeSse(clientBody, "content_block_delta", blockDelta);
        }

        // Handle text content
        String content = delta.path("content").asText(null);
        if ((content == null || content.isEmpty()) && delta.path("refusal").isTextual()) {
            content = delta.path("refusal").asText("");
        }
        if (content != null && !content.isEmpty()) {
            closeOpenClaudeToolBlocks(state.openToolBlocks, clientBody);
            if (state.reasoningBlockOpen) {
                writeSse(clientBody, "content_block_stop", objectNode().put("type", "content_block_stop").put("index", state.blockIndex));
                state.reasoningBlockOpen = false;
                state.blockIndex++;
            }
            if (!state.textBlockOpen) {
                ObjectNode blockStart = objectNode();
                blockStart.put("type", "content_block_start");
                blockStart.put("index", state.blockIndex);
                ObjectNode contentBlock = objectNode();
                contentBlock.put("type", "text");
                contentBlock.put("text", "");
                blockStart.set("content_block", contentBlock);
                writeSse(clientBody, "content_block_start", blockStart);
                state.textBlockOpen = true;
            }
            ObjectNode blockDelta = objectNode();
            blockDelta.put("type", "content_block_delta");
            blockDelta.put("index", state.blockIndex);
            ObjectNode textDelta = objectNode();
            textDelta.put("type", "text_delta");
            textDelta.put("text", content);
            blockDelta.set("delta", textDelta);
            writeSse(clientBody, "content_block_delta", blockDelta);
        }

        // Handle tool_calls
        JsonNode toolCalls = delta.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray()) {
            for (JsonNode tc : toolCalls) {
                int tcIndex = tc.path("index").asInt(0);
                String tcId = tc.path("id").asText(null);
                String tcName = tc.path("function").path("name").asText(null);
                String tcArgs = tc.path("function").path("arguments").asText(null);

                if (!state.blockToToolIndex.containsKey(tcIndex)) {
                    if (state.textBlockOpen) {
                        writeSse(clientBody, "content_block_stop", objectNode()
                                .put("type", "content_block_stop").put("index", state.blockIndex));
                        state.textBlockOpen = false;
                        state.blockIndex++;
                    }
                    if (state.reasoningBlockOpen) {
                        writeSse(clientBody, "content_block_stop", objectNode()
                                .put("type", "content_block_stop").put("index", state.blockIndex));
                        state.reasoningBlockOpen = false;
                        state.blockIndex++;
                    }
                    state.blockToToolIndex.put(tcIndex, state.blockIndex++);
                }

                if (tcId != null && !tcId.isBlank()) {
                    state.toolCallIds.put(tcIndex, tcId);
                }
                if (tcName != null && !tcName.isBlank()) {
                    state.toolCallNames.put(tcIndex, tcName);
                }

                boolean announced = state.announcedToolCalls.contains(tcIndex);
                if (!announced && state.toolCallNames.containsKey(tcIndex)) {
                    announceClaudeToolCall(state, tcIndex, clientBody);
                    announced = true;
                }

                if (tcArgs != null && !tcArgs.isEmpty()) {
                    if (announced) {
                        writeClaudeToolArgumentsDelta(
                                state.blockToToolIndex.get(tcIndex), tcArgs, clientBody);
                    } else {
                        state.pendingToolArguments
                                .computeIfAbsent(tcIndex, ignored -> new StringBuilder())
                                .append(tcArgs);
                    }
                }
            }
        }

        // Handle usage
        JsonNode usage = chunk.get("usage");
        if (usage != null && !usage.isNull()) {
            JsonNode details = usage.path("prompt_tokens_details");
            state.cacheReadInputTokens = details.path("cached_tokens").asLong(0);
            state.cacheCreationInputTokens = details.path("cache_creation_tokens").asLong(0)
                    + details.path("cache_write_tokens").asLong(0);
            state.inputTokens = Math.max(0, usage.path("prompt_tokens").asLong(0)
                    - state.cacheReadInputTokens - state.cacheCreationInputTokens);
            state.outputTokens = usage.path("completion_tokens").asLong(0);
        }

        // Handle finish
        if (finishReason != null) {
            boolean hasToolCalls = !state.announcedToolCalls.isEmpty();
            state.stopReason = switch (finishReason) {
                case "length" -> "max_tokens";
                case "tool_calls", "function_call" -> "tool_use";
                case "content_filter" -> "refusal";
                case "stop" -> hasToolCalls ? "tool_use" : "end_turn";
                default -> hasToolCalls ? "tool_use" : "end_turn";
            };
        }
    }

    private void announceClaudeToolCall(
            ChatToClaudeStreamState state,
            int toolCallIndex,
            OutputStream clientBody
    ) throws IOException {
        int blockIndex = state.blockToToolIndex.get(toolCallIndex);
        String toolCallId = state.toolCallIds.computeIfAbsent(
                toolCallIndex,
                ignored -> "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        ObjectNode blockStart = objectNode();
        blockStart.put("type", "content_block_start");
        blockStart.put("index", blockIndex);
        ObjectNode contentBlock = objectNode();
        contentBlock.put("type", "tool_use");
        contentBlock.put("id", toolCallId);
        contentBlock.put("name", state.toolCallNames.get(toolCallIndex));
        blockStart.set("content_block", contentBlock);
        writeSse(clientBody, "content_block_start", blockStart);
        state.announcedToolCalls.add(toolCallIndex);
        state.openToolBlocks.add(blockIndex);

        StringBuilder pendingArguments = state.pendingToolArguments.remove(toolCallIndex);
        if (pendingArguments != null && !pendingArguments.isEmpty()) {
            writeClaudeToolArgumentsDelta(blockIndex, pendingArguments.toString(), clientBody);
        }
    }

    private void writeClaudeToolArgumentsDelta(
            int blockIndex,
            String arguments,
            OutputStream clientBody
    ) throws IOException {
        ObjectNode blockDelta = objectNode();
        blockDelta.put("type", "content_block_delta");
        blockDelta.put("index", blockIndex);
        ObjectNode inputDelta = objectNode();
        inputDelta.put("type", "input_json_delta");
        inputDelta.put("partial_json", arguments);
        blockDelta.set("delta", inputDelta);
        writeSse(clientBody, "content_block_delta", blockDelta);
    }

    private void closeOpenClaudeToolBlocks(Set<Integer> openToolBlocks, OutputStream clientBody) throws IOException {
        for (Integer blockIndex : openToolBlocks.stream().sorted().toList()) {
            writeSse(clientBody, "content_block_stop",
                    objectNode().put("type", "content_block_stop").put("index", blockIndex));
        }
        openToolBlocks.clear();
    }

    private UnifiedTokenUsage transformBedrockInvokeModelToClaude(
            InputStream upstreamBody, OutputStream clientBody) throws IOException {
        UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
        BedrockEvent event;
        while ((event = readEvent(upstreamBody)) != null) {
            if ("exception".equals(event.messageType())) {
                String errorPayload = new String(event.payload(), StandardCharsets.UTF_8);
                throw new IOException("Bedrock InvokeModel stream exception ["
                        + event.exceptionType() + "]: " + errorPayload);
            }
            JsonNode chunk = objectMapper.readTree(event.payload());
            String base64Bytes = chunk.path("bytes").asText("");
            if (base64Bytes.isEmpty()) {
                continue;
            }
            byte[] sseChunk = java.util.Base64.getDecoder().decode(base64Bytes);
            clientBody.write(sseChunk);
            clientBody.flush();
            String sseText = new String(sseChunk, StandardCharsets.UTF_8);
            if (sseText.contains("\"type\":\"message_delta\"")) {
                usage = tryExtractClaudeUsageFromSseChunk(sseText, usage);
            }
        }
        return usage;
    }

    private UnifiedTokenUsage tryExtractClaudeUsageFromSseChunk(String sseText, UnifiedTokenUsage current) {
        for (String line : sseText.split("\n")) {
            if (!line.startsWith("data: ")) {
                continue;
            }
            try {
                JsonNode data = objectMapper.readTree(line.substring(6));
                JsonNode usageNode = data.path("usage");
                if (usageNode.isMissingNode() || !usageNode.isObject()) {
                    continue;
                }
                return ClaudeMessagesUsageExtractor.extractUsageNode(usageNode);
            } catch (Exception ignored) {
            }
        }
        return current;
    }

    private UnifiedTokenUsage transformClaudeToChat(String model, InputStream upstreamBody,
                                                     OutputStream clientBody) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(upstreamBody, StandardCharsets.UTF_8));
        ClaudeToChatStreamState state = new ClaudeToChatStreamState(model);

        String currentEvent = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event: ")) {
                currentEvent = line.substring(7).trim();
                continue;
            }
            if (!line.startsWith("data: ")) continue;
            String data = line.substring(6).trim();
            if (data.isEmpty()) continue;

            JsonNode event;
            try {
                event = objectMapper.readTree(data);
            } catch (JsonProcessingException e) {
                log.warn("Skipping malformed Claude SSE event: {}", data.length() > 200 ? data.substring(0, 200) : data, e);
                continue;
            }

            String eventType = currentEvent != null ? currentEvent : event.path("type").asText("");
            currentEvent = null;

            handleClaudeEventForChat(eventType, event, state, clientBody);
        }

        return UnifiedTokenUsage.known(state.inputTokens, state.outputTokens, 0, 0);
    }

    private void handleClaudeEventForChat(String eventType, JsonNode event,
                                          ClaudeToChatStreamState state, OutputStream clientBody) throws IOException {
        switch (eventType) {
            case "message_start" -> {
                if (!state.roleSent) {
                    state.roleSent = true;
                    writeChatChunk(clientBody, state.chatId, state.model, state.created,
                            objectNode().put("role", "assistant"), null);
                }
                JsonNode msgUsage = event.path("message").path("usage");
                state.inputTokens = msgUsage.path("input_tokens").asLong(0);
            }
            case "content_block_delta" -> {
                JsonNode blockDelta = event.path("delta");
                String deltaType = blockDelta.path("type").asText("");
                switch (deltaType) {
                    case "text_delta" -> {
                        ObjectNode d = objectNode();
                        d.put("content", blockDelta.path("text").asText(""));
                        writeChatChunk(clientBody, state.chatId, state.model, state.created, d, null);
                    }
                    case "thinking_delta" -> {
                        ObjectNode d = objectNode();
                        d.put("reasoning_content", blockDelta.path("thinking").asText(""));
                        writeChatChunk(clientBody, state.chatId, state.model, state.created, d, null);
                    }
                    case "input_json_delta" -> {
                        int blockIdx = event.path("index").asInt(0);
                        Integer tcIdx = state.blockToToolIndex.get(blockIdx);
                        if (tcIdx != null) {
                            ObjectNode d = objectNode();
                            ArrayNode tcs = objectMapper.createArrayNode();
                            ObjectNode tc = objectNode();
                            tc.put("index", tcIdx);
                            ObjectNode fn = objectNode();
                            fn.put("arguments", blockDelta.path("partial_json").asText(""));
                            tc.set("function", fn);
                            tcs.add(tc);
                            d.set("tool_calls", tcs);
                            writeChatChunk(clientBody, state.chatId, state.model, state.created, d, null);
                        }
                    }
                }
            }
            case "content_block_start" -> {
                JsonNode contentBlock = event.path("content_block");
                String blockType = contentBlock.path("type").asText("");
                int blockIdx = event.path("index").asInt(0);
                if ("tool_use".equals(blockType)) {
                    state.blockToToolIndex.put(blockIdx, state.toolCallIndex);
                    ObjectNode d = objectNode();
                    ArrayNode tcs = objectMapper.createArrayNode();
                    ObjectNode tc = objectNode();
                    tc.put("index", state.toolCallIndex);
                    tc.put("id", contentBlock.path("id").asText(""));
                    tc.put("type", "function");
                    ObjectNode fn = objectNode();
                    fn.put("name", contentBlock.path("name").asText(""));
                    fn.put("arguments", "");
                    tc.set("function", fn);
                    tcs.add(tc);
                    d.set("tool_calls", tcs);
                    writeChatChunk(clientBody, state.chatId, state.model, state.created, d, null);
                    state.toolCallIndex++;
                }
            }
            case "message_delta" -> {
                JsonNode msgDelta = event.path("delta");
                JsonNode srNode = msgDelta.get("stop_reason");
                if (srNode == null || !srNode.isTextual() || srNode.asText().isBlank()) {
                    throw new IOException("Claude stream missing stop_reason in message_delta");
                }
                String sr = srNode.asText();
                String finishReason = switch (sr) {
                    case "end_turn", "stop_sequence" -> "stop";
                    case "max_tokens" -> "length";
                    case "tool_use" -> "tool_calls";
                    case "refusal" -> "content_filter";
                    default -> throw new IOException("Unsupported Claude stop_reason in stream: " + sr);
                };
                state.outputTokens = event.path("usage").path("output_tokens").asLong(0);
                writeChatChunk(clientBody, state.chatId, state.model, state.created, objectNode(), finishReason);

                // Write usage chunk
                ObjectNode usageChunk = objectNode();
                usageChunk.put("id", state.chatId);
                usageChunk.put("object", "chat.completion.chunk");
                usageChunk.put("created", state.created);
                usageChunk.put("model", state.model);
                ArrayNode choices = objectMapper.createArrayNode();
                usageChunk.set("choices", choices);
                ObjectNode usageNode = objectNode();
                usageNode.put("prompt_tokens", state.inputTokens);
                usageNode.put("completion_tokens", state.outputTokens);
                usageNode.put("total_tokens", state.inputTokens + state.outputTokens);
                usageChunk.set("usage", usageNode);
                clientBody.write(("data: " + objectMapper.writeValueAsString(usageChunk) + "\n\n").getBytes(StandardCharsets.UTF_8));
                clientBody.flush();
            }
            case "message_stop" -> {
                clientBody.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                clientBody.flush();
            }
        }
    }

    private void writeChatChunk(OutputStream out, String id, String model, long created,
                                ObjectNode delta, String finishReason) throws IOException {
        ObjectNode chunk = objectNode();
        chunk.put("id", id);
        chunk.put("object", "chat.completion.chunk");
        chunk.put("created", created);
        chunk.put("model", model);
        ArrayNode choices = objectMapper.createArrayNode();
        ObjectNode choice = objectNode();
        choice.put("index", 0);
        choice.set("delta", delta);
        if (finishReason != null) {
            choice.put("finish_reason", finishReason);
        } else {
            choice.putNull("finish_reason");
        }
        choices.add(choice);
        chunk.set("choices", choices);
        out.write(("data: " + objectMapper.writeValueAsString(chunk) + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
