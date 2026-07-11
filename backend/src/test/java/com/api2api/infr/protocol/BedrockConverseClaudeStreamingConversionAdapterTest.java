package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(dataEvents.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .allMatch(node -> node.has("delta"))).isTrue();
        assertThat(usage.usageKnown()).isTrue();
        assertThat(usage.totalTokens()).isEqualTo(5);
    }

    @Test
    void shouldPreserveWriteToolUseAfterPlanIntroText() throws Exception {
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "contentBlockStart", "{\"contentBlockIndex\":0,\"start\":{}}");
        writeEvent(upstream, "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"现在让我写计划文件：\"}}");
        writeEvent(upstream, "contentBlockStop", "{\"contentBlockIndex\":0}");
        writeEvent(upstream, "contentBlockStart", "{\"contentBlockIndex\":1,\"start\":{\"toolUse\":{\"toolUseId\":\"tooluse_plan_1\",\"name\":\"Write\"}}}");
        writeEvent(upstream, "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"toolUse\":{\"input\":\"{\\\"file_path\\\":\\\"/tmp/plan.md\\\",\"}}}");
        writeEvent(upstream, "contentBlockDelta", "{\"contentBlockIndex\":1,\"delta\":{\"toolUse\":{\"input\":\"\\\"content\\\":\\\"# Plan\\\"}\"}}}");
        writeEvent(upstream, "contentBlockStop", "{\"contentBlockIndex\":1}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"tool_use\"}");
        writeEvent(upstream, "metadata", "{\"usage\":{\"inputTokens\":100,\"outputTokens\":20}}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        adapter.transform(
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.CLAUDE_MESSAGES,
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        JsonNode toolStart = events.stream()
                .filter(node -> "tool_use".equals(node.at("/content_block/type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(toolStart.path("index").asInt()).isEqualTo(1);
        assertThat(toolStart.at("/content_block/id").asText()).isEqualTo("tooluse_plan_1");
        assertThat(toolStart.at("/content_block/name").asText()).isEqualTo("Write");
        assertThat(events.stream()
                .filter(node -> "input_json_delta".equals(node.at("/delta/type").asText()))
                .map(node -> node.at("/delta/partial_json").asText())
                .toList()).containsExactly(
                        "{\"file_path\":\"/tmp/plan.md\",",
                        "\"content\":\"# Plan\"}"
                );
        JsonNode messageDelta = events.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(messageDelta.at("/delta/stop_reason").asText()).isEqualTo("tool_use");
    }

    @Test
    void shouldConvertOpenAIResponsesSseToClaudeSseWithToolUseAndUsage() throws Exception {
        String upstream = """
                event: response.created
                data: {"type":"response.created","response":{"id":"resp_1"}}

                event: response.output_item.done
                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":"encrypted"}}

                event: response.output_item.added
                data: {"type":"response.output_item.added","output_index":1,"item":{"type":"function_call","call_id":"call_1","name":"get_weather"}}

                event: response.function_call_arguments.delta
                data: {"type":"response.function_call_arguments.delta","output_index":1,"delta":"{\\\"city\\\":\\\"BJ\\\"}"}

                event: response.output_item.done
                data: {"type":"response.output_item.done","output_index":1}

                event: response.completed
                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":7,"output_tokens":3,"input_tokens_details":{"cached_tokens":2}}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        UnifiedTokenUsage usage = adapter.transform(
                ProtocolType.OPENAI_RESPONSES,
                ProtocolType.CLAUDE_MESSAGES,
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        assertThat(events.stream().anyMatch(node -> "tool_use".equals(node.at("/content_block/type").asText()))).isTrue();
        assertThat(events.stream()
                .filter(node -> "content_block_start".equals(node.path("type").asText()))
                .findFirst().orElseThrow().path("index").asInt()).isZero();
        assertThat(events.stream().anyMatch(node -> "input_json_delta".equals(node.at("/delta/type").asText()))).isTrue();
        assertThat(events.stream().anyMatch(node -> "signature_delta".equals(node.at("/delta/type").asText())
                && node.at("/delta/signature").asText().startsWith(ResponsesReasoningBridge.SIGNATURE_PREFIX))).isTrue();
        JsonNode messageDelta = events.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(messageDelta.at("/delta/stop_reason").asText()).isEqualTo("tool_use");
        assertThat(messageDelta.at("/usage/output_tokens").asLong()).isEqualTo(3);
        assertThat(usage.totalTokens()).isEqualTo(10);
    }

    @Test
    void shouldConvertBedrockEventStreamToOpenAIResponsesSseAndExtractUsage() throws Exception {
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"OK\"}}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"end_turn\"}");
        writeEvent(upstream, "metadata", "{\"usage\":{\"inputTokens\":3,\"outputTokens\":2}}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        UnifiedTokenUsage usage = adapter.transform(
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.OPENAI_RESPONSES,
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        String sse = downstream.toString(StandardCharsets.UTF_8);
        assertThat(sse).contains("event: response.created");
        assertThat(sse).contains("event: response.output_text.delta");
        assertThat(sse).contains("event: response.completed");
        assertThat(sse).contains("data: [DONE]");
        assertThat(dataEvents(sse).stream().anyMatch(node -> "OK".equals(node.path("delta").asText()))).isTrue();
        assertThat(usage.usageKnown()).isTrue();
        assertThat(usage.totalTokens()).isEqualTo(5);
    }

    @Test
    void shouldSurfaceBedrockStreamExceptionInsteadOfCompletingSuccessfully() throws Exception {
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "validationException", "{\"message\":\"invalid thinking field\"}");

        assertThatThrownBy(() -> adapter.transform(
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.CLAUDE_MESSAGES,
                new ByteArrayInputStream(upstream.toByteArray()),
                new ByteArrayOutputStream()
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("validationException")
                .hasMessageContaining("invalid thinking field");
    }

    private List<JsonNode> dataEvents(String sse) throws Exception {
        List<JsonNode> events = new ArrayList<>();
        for (String line : sse.split("\\R")) {
            if (line.startsWith("data: ")) {
                String data = line.substring("data: ".length());
                if (!"[DONE]".equals(data)) {
                    events.add(objectMapper.readTree(data));
                }
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
