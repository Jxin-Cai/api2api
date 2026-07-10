package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class BedrockConverseClaudeStreamingConversionAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BedrockConverseClaudeStreamingConversionAdapter adapter =
            new BedrockConverseClaudeStreamingConversionAdapter(objectMapper);

    @Test
    void shouldConvertBedrockEventStreamToClaudeSseAndExtractUsage() throws Exception {
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"OK\"}}");
        writeEvent(upstream, "contentBlockStop", "{\"contentBlockIndex\":0}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"end_turn\"}");
        writeEvent(upstream, "metadata", "{\"usage\":{\"inputTokens\":3,\"outputTokens\":2}}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        UnifiedTokenUsage usage = adapter.transform(
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.CLAUDE_MESSAGES,
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        String sse = downstream.toString(StandardCharsets.UTF_8);
        assertThat(sse).contains("event: message_start");
        assertThat(sse).contains("event: content_block_delta");
        assertThat(sse).contains("event: message_stop");
        List<JsonNode> dataEvents = dataEvents(sse);
        assertThat(dataEvents.stream().anyMatch(node -> "text_delta".equals(node.at("/delta/type").asText()))).isTrue();
        assertThat(dataEvents.stream().anyMatch(node -> "OK".equals(node.at("/delta/text").asText()))).isTrue();
        assertThat(usage.usageKnown()).isTrue();
        assertThat(usage.totalTokens()).isEqualTo(5);
    }

    private List<JsonNode> dataEvents(String sse) throws Exception {
        List<JsonNode> events = new ArrayList<>();
        for (String line : sse.split("\\R")) {
            if (line.startsWith("data: ")) {
                events.add(objectMapper.readTree(line.substring("data: ".length())));
            }
        }
        return events;
    }

    private void writeEvent(ByteArrayOutputStream outputStream, String eventType, String payload) throws Exception {
        byte[] headers = headers(eventType);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int totalLength = 16 + headers.length + payloadBytes.length;
        ByteArrayOutputStream messageWithoutCrc = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(messageWithoutCrc);
        dataOutput.writeInt(totalLength);
        dataOutput.writeInt(headers.length);
        CRC32 preludeCrc = new CRC32();
        preludeCrc.update(messageWithoutCrc.toByteArray());
        dataOutput.writeInt((int) preludeCrc.getValue());
        dataOutput.write(headers);
        dataOutput.write(payloadBytes);
        byte[] withoutMessageCrc = messageWithoutCrc.toByteArray();
        CRC32 messageCrc = new CRC32();
        messageCrc.update(withoutMessageCrc);
        dataOutput.writeInt((int) messageCrc.getValue());
        outputStream.write(messageWithoutCrc.toByteArray());
    }

    private byte[] headers(String eventType) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeStringHeader(outputStream, ":event-type", eventType);
        writeStringHeader(outputStream, ":content-type", "application/json");
        writeStringHeader(outputStream, ":message-type", "event");
        return outputStream.toByteArray();
    }

    private void writeStringHeader(ByteArrayOutputStream outputStream, String name, String value) throws Exception {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        outputStream.write(nameBytes.length);
        outputStream.write(nameBytes);
        outputStream.write(7);
        outputStream.write((valueBytes.length >>> 8) & 0xFF);
        outputStream.write(valueBytes.length & 0xFF);
        outputStream.write(valueBytes);
    }
}
