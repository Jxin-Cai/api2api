package com.api2api.infr.protocol;

import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Converts AWS Bedrock ConverseStream event-stream frames into Claude Messages SSE events.
 */
@Component
public class BedrockConverseClaudeStreamingConversionAdapter implements GatewayStreamingConversionPort {

    private static final int EVENT_STREAM_OVERHEAD_BYTES = 16;

    private final ObjectMapper objectMapper;

    public BedrockConverseClaudeStreamingConversionAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public boolean supports(ProtocolType upstreamProtocol, ProtocolType clientProtocol) {
        return upstreamProtocol == ProtocolType.AWS_BEDROCK_CONVERSE
                && (clientProtocol == ProtocolType.CLAUDE_MESSAGES || clientProtocol == ProtocolType.OPENAI_RESPONSES);
    }

    @Override
    public UnifiedTokenUsage transform(
            ProtocolType upstreamProtocol,
            ProtocolType clientProtocol,
            InputStream upstreamBody,
            OutputStream clientBody
    ) throws IOException {
        if (!supports(upstreamProtocol, clientProtocol)) {
            return UnifiedTokenUsage.unknown();
        }
        StreamState state = new StreamState(clientProtocol);
        BedrockEvent event;
        while ((event = readEvent(upstreamBody)) != null) {
            JsonNode payload = objectMapper.readTree(event.payload());
            handleEvent(event.eventType(), payload, state, clientBody);
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
        response.put("model", "bedrock");
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
            response.put("model", "bedrock");
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
        message.put("model", "bedrock");
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
        String stopReason = mapBedrockStopToClaudeStop(payload.path("stopReason").asText("end_turn"));
        ObjectNode event = objectNode();
        event.put("type", "message_delta");
        ObjectNode delta = objectNode();
        delta.put("stop_reason", stopReason);
        delta.putNull("stop_sequence");
        event.set("delta", delta);
        writeSse(clientBody, "message_delta", event);
    }

    private void writeMetadata(JsonNode payload, StreamState state, OutputStream clientBody) throws IOException {
        UnifiedTokenUsage usage = extractUsage(payload.path("usage"));
        state.usage = usage;
        if (usage.usageKnown()) {
            ObjectNode event = objectNode();
            event.put("type", "message_delta");
            ObjectNode usageNode = objectNode();
            usageNode.put("input_tokens", usage.inputTokens());
            usageNode.put("output_tokens", usage.outputTokens());
            if (usage.cacheReadInputTokens() > 0) {
                usageNode.put("cache_read_input_tokens", usage.cacheReadInputTokens());
            }
            if (usage.cacheCreationInputTokens() > 0) {
                usageNode.put("cache_creation_input_tokens", usage.cacheCreationInputTokens());
            }
            event.set("usage", usageNode);
            writeSse(clientBody, "message_delta", event);
        }
        writeTerminalEventIfNecessary(state, clientBody);
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
        return reasoningText.isMissingNode() ? reasoningContent : reasoningText;
    }

    private String mapBedrockStopToClaudeStop(String stopReason) {
        return switch (stopReason) {
            case "max_tokens" -> "max_tokens";
            case "tool_use" -> "tool_use";
            case "stop_sequence" -> "stop_sequence";
            default -> "end_turn";
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
        return new BedrockEvent(parseEventType(headers), payload);
    }

    private String parseEventType(byte[] headers) throws IOException {
        DataInputStream input = new DataInputStream(new java.io.ByteArrayInputStream(headers));
        while (input.available() > 0) {
            int nameLength = input.readUnsignedByte();
            String name = new String(input.readNBytes(nameLength), StandardCharsets.UTF_8);
            int type = input.readUnsignedByte();
            String value = readHeaderValue(input, type);
            if (":event-type".equals(name)) {
                return value;
            }
        }
        return "";
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

    private record BedrockEvent(String eventType, byte[] payload) {
    }

    private static final class StreamState {
        private final ProtocolType clientProtocol;
        private boolean messageStarted;
        private boolean messageStopped;
        private String stopReason = "end_turn";
        private UnifiedTokenUsage usage;
        private final Map<Integer, String> blockTypes = new HashMap<>();
        private final Map<Integer, Boolean> stoppedBlocks = new HashMap<>();

        private StreamState(ProtocolType clientProtocol) {
            this.clientProtocol = clientProtocol;
        }
    }
}
