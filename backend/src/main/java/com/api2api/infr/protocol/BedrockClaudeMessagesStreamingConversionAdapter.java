package com.api2api.infr.protocol;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** Converts Bedrock InvokeModelWithResponseStream frames to native Claude SSE. */
final class BedrockClaudeMessagesStreamingConversionAdapter implements GatewayStreamingConversionPort {

    private final ObjectMapper objectMapper;

    public BedrockClaudeMessagesStreamingConversionAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public boolean supports(ProtocolType upstreamProtocol, ProtocolType clientProtocol) {
        return upstreamProtocol == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES
                && clientProtocol == ProtocolType.CLAUDE_MESSAGES;
    }

    @Override
    public UnifiedTokenUsage transform(
            GatewayStreamingConversionContext context,
            InputStream upstreamBody,
            OutputStream clientBody
    ) throws IOException {
        Objects.requireNonNull(context, "Streaming conversion context must not be null");
        if (!supports(context.upstreamProtocol(), context.clientProtocol())) {
            return UnifiedTokenUsage.unknown();
        }

        UsageAccumulator usage = new UsageAccumulator();
        boolean terminalEventSeen = false;
        UnifiedStreamingConversionAdapter.BedrockEvent event;
        while ((event = UnifiedStreamingConversionAdapter.readEvent(upstreamBody)) != null) {
            JsonNode envelope = objectMapper.readTree(event.payload());
            if ("exception".equals(event.messageType())) {
                String type = event.exceptionType().isBlank() ? "unknownException" : event.exceptionType();
                throw new IOException("Bedrock Invoke stream failed: " + type + errorMessage(envelope));
            }
            if (!"chunk".equals(event.eventType())) {
                continue;
            }
            JsonNode message = decodeChunk(envelope);
            String type = message.path("type").asText("");
            if (type.isBlank()) {
                throw new IOException("Bedrock Invoke stream chunk is missing Claude event type");
            }
            usage.observe(message);
            writeSse(clientBody, type, message);
            if ("message_stop".equals(type) || "error".equals(type)) {
                terminalEventSeen = true;
            }
        }
        if (!terminalEventSeen) {
            throw new EOFException("Bedrock Invoke stream ended before Claude message_stop");
        }
        clientBody.flush();
        return usage.toUnifiedUsage();
    }

    private JsonNode decodeChunk(JsonNode envelope) throws IOException {
        JsonNode bytes = envelope.get("bytes");
        if (bytes == null || !bytes.isTextual() || bytes.asText().isBlank()) {
            throw new IOException("Bedrock Invoke stream chunk is missing bytes");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(bytes.asText());
            JsonNode message = objectMapper.readTree(decoded);
            if (message == null || !message.isObject()) {
                throw new IOException("Bedrock Invoke stream chunk must contain a Claude event object");
            }
            return message;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Bedrock Invoke stream chunk contains invalid base64", exception);
        }
    }

    private void writeSse(OutputStream output, String eventName, JsonNode data) throws IOException {
        output.write(("event: " + eventName + "\n").getBytes(StandardCharsets.UTF_8));
        output.write(("data: " + objectMapper.writeValueAsString(data) + "\n\n")
                .getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private String errorMessage(JsonNode payload) {
        String message = payload == null ? "" : payload.path("message").asText("");
        return message.isBlank() ? "" : " - " + message;
    }

    private static final class UsageAccumulator {
        private long inputTokens;
        private long outputTokens;
        private long cacheCreationInputTokens;
        private long cacheReadInputTokens;
        private boolean known;

        private void observe(JsonNode event) {
            JsonNode usage = "message_start".equals(event.path("type").asText(""))
                    ? event.path("message").path("usage")
                    : event.path("usage");
            if (!usage.isObject()) {
                return;
            }
            UnifiedTokenUsage observed = ClaudeMessagesUsageExtractor.extractUsageNode(usage);
            if (!observed.usageKnown()) {
                return;
            }
            known = true;
            inputTokens = Math.max(inputTokens, observed.inputTokens());
            outputTokens = Math.max(outputTokens, observed.outputTokens());
            cacheCreationInputTokens = Math.max(cacheCreationInputTokens, observed.cacheCreationInputTokens());
            cacheReadInputTokens = Math.max(cacheReadInputTokens, observed.cacheReadInputTokens());
        }

        private UnifiedTokenUsage toUnifiedUsage() {
            return known
                    ? UnifiedTokenUsage.known(
                            inputTokens, outputTokens, cacheCreationInputTokens, cacheReadInputTokens)
                    : UnifiedTokenUsage.unknown();
        }
    }
}
