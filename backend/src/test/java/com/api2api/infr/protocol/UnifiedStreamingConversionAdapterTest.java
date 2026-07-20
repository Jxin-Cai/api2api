package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.protocol.model.ProtocolConversionRouteContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class UnifiedStreamingConversionAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UnifiedStreamingConversionAdapter adapter =
            new UnifiedStreamingConversionAdapter(objectMapper);

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
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
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
    void test_closesEachChatToolBlock_when_multipleToolCallsAreStreamedToClaude() throws Exception {
        String upstream = """
                data: {"id":"chatcmpl_1","choices":[{"delta":{"role":"assistant"},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_0","type":"function","function":{"name":"Read","arguments":"{\\\"path\\\":\\\"a\\\"}"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{"tool_calls":[{"index":1,"id":"call_1","type":"function","function":{"name":"Bash","arguments":"{\\\"command\\\":\\\"pwd\\\"}"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{},"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":10,"completion_tokens":4}}

                data: [DONE]

                """;

        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        String sse = downstream.toString(StandardCharsets.UTF_8);
        List<JsonNode> events = dataEvents(sse);
        assertThat(events.stream()
                .filter(node -> "content_block_start".equals(node.path("type").asText()))
                .map(node -> node.path("index").asInt())
                .toList()).containsExactly(0, 1);
        assertThat(events.stream()
                .filter(node -> "content_block_stop".equals(node.path("type").asText()))
                .map(node -> node.path("index").asInt())
                .toList()).containsExactly(0, 1);
        assertThat(events.stream()
                .filter(node -> "input_json_delta".equals(node.at("/delta/type").asText()))
                .map(node -> node.path("index").asInt())
                .toList()).containsExactly(0, 1);
        assertThat(events.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst().orElseThrow().at("/delta/stop_reason").asText()).isEqualTo("tool_use");
        assertThat(usage.inputTokens()).isEqualTo(10);
        assertThat(usage.outputTokens()).isEqualTo(4);
    }

    @Test
    void test_buffersChatToolArguments_when_toolNameArrivesLate() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl_1","choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"arguments":"{\\\"path\\\":"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{"tool_calls":[{"index":0,"function":{"name":"Read","arguments":"\\\"a\\\"}"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{},"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":3,"completion_tokens":2}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        JsonNode toolStart = events.stream()
                .filter(node -> "tool_use".equals(node.at("/content_block/type").asText()))
                .findFirst().orElseThrow();
        assertThat(toolStart.at("/content_block/name").asText()).isEqualTo("Read");
        assertThat(events.stream()
                .filter(node -> "input_json_delta".equals(node.at("/delta/type").asText()))
                .map(node -> node.at("/delta/partial_json").asText())
                .reduce("", String::concat)).isEqualTo("{\"path\":\"a\"}");
    }

    @Test
    void test_reportsToolUseStopReason_when_streamedToolCallFinishesWithStop() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl_1","choices":[{"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"name":"Read","arguments":"{}"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        assertThat(events.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst().orElseThrow().at("/delta/stop_reason").asText()).isEqualTo("tool_use");
    }

    @Test
    void test_preservesChatCacheUsage_when_streamContainsUsageDetails() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl_1","choices":[{"delta":{"content":"done"},"finish_reason":"stop"}]}

                data: {"id":"chatcmpl_1","choices":[],"usage":{"prompt_tokens":100,"completion_tokens":5,"prompt_tokens_details":{"cached_tokens":30,"cache_creation_tokens":10,"cache_write_tokens":5}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(55);
        assertThat(usage.cacheCreationInputTokens()).isEqualTo(15);
        assertThat(usage.cacheReadInputTokens()).isEqualTo(30);
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        JsonNode messageDelta = events.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst().orElseThrow();
        assertThat(messageDelta.at("/usage/cache_creation_input_tokens").asLong()).isEqualTo(15);
        assertThat(messageDelta.at("/usage/cache_read_input_tokens").asLong()).isEqualTo(30);
    }

    @Test
    void test_usesValidBlockIndexes_when_reasoningArrivesAfterText() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl_1","choices":[{"delta":{"content":"answer"},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{"reasoning_content":"late reasoning"},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        assertThat(dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> node.has("index"))
                .map(node -> node.path("index").asInt())
                .toList()).allMatch(index -> index >= 0);
    }

    @Test
    void test_flushesClaudeEvent_before_bedrockStreamCompletes() throws Exception {
        // Arrange
        try (PipedInputStream upstreamInput = new PipedInputStream();
             PipedOutputStream upstreamOutput = new PipedOutputStream(upstreamInput)) {
            FlushAwareOutputStream downstream = new FlushAwareOutputStream();
            CompletableFuture<UnifiedTokenUsage> conversion = CompletableFuture.supplyAsync(() -> {
                try {
                    return adapter.transform(
                            context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                            upstreamInput,
                            downstream
                    );
                } catch (Exception exception) {
                    throw new java.util.concurrent.CompletionException(exception);
                }
            });
            ByteArrayOutputStream firstFrame = new ByteArrayOutputStream();
            writeEvent(firstFrame, "messageStart", "{\"role\":\"assistant\"}");

            // Act
            upstreamOutput.write(firstFrame.toByteArray());
            upstreamOutput.flush();

            // Assert
            assertThat(downstream.awaitFlush()).isTrue();
            assertThat(downstream.toString(StandardCharsets.UTF_8)).contains("event: message_start");
            assertThat(conversion).isNotDone();

            ByteArrayOutputStream terminalFrames = new ByteArrayOutputStream();
            writeEvent(terminalFrames, "messageStop", "{\"stopReason\":\"end_turn\"}");
            upstreamOutput.write(terminalFrames.toByteArray());
            upstreamOutput.close();
            conversion.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void test_wrapsBedrockThinkingSignature_when_streamTargetsClaude() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "contentBlockDelta", """
                {"contentBlockIndex":0,"delta":{"reasoningContent":{"reasoningText":{
                  "text":"think","signature":"bedrock-"
                }}}}
                """);
        writeEvent(upstream, "contentBlockDelta", """
                {"contentBlockIndex":0,"delta":{"reasoningContent":{"reasoningText":{
                  "text":"","signature":"signature"
                }}}}
                """);
        writeEvent(upstream, "contentBlockStop", "{\"contentBlockIndex\":0}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"end_turn\"}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        String signature = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "signature_delta".equals(node.at("/delta/type").asText()))
                .map(node -> node.at("/delta/signature").asText())
                .findFirst()
                .orElseThrow();
        assertThat(BedrockReasoningBridge.decode(
                signature,
                new ProtocolConversionRouteContext(1L, "anthropic.claude-opus-4-6-v1:0")))
                .contains("bedrock-signature");
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
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
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
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
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
    void test_stopsReadingUpstream_when_responsesTerminalEventIsReceived() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.output_text.delta","output_index":0,"delta":"done"}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":7,"output_tokens":1}}}

                data: {malformed-event-after-terminal}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8)).contains("event: message_stop");
        assertThat(usage.totalTokens()).isEqualTo(8);
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
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_RESPONSES),
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
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                new ByteArrayOutputStream()
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("validationException")
                .hasMessageContaining("invalid thinking field");
    }

    @Test
    void shouldSurfaceModeledBedrockExceptionFrame_when_eventTypeIsAbsent() throws Exception {
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();
        writeModeledException(upstream, "validationException", "{\"message\":\"invalid thinking field\"}");

        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("validationException")
                .hasMessageContaining("invalid thinking field");
        assertThat(downstream.toString(StandardCharsets.UTF_8)).doesNotContain("event: message_stop");
    }

    @Test
    void shouldSurfaceModeledBedrockExceptionFrameForResponses_when_eventTypeIsAbsent() throws Exception {
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();
        writeModeledException(upstream, "throttlingException", "{\"message\":\"rate limited\"}");

        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("throttlingException")
                .hasMessageContaining("rate limited");
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .doesNotContain("event: response.completed")
                .doesNotContain("data: [DONE]");
    }

    @Test
    void test_preservesRequestedModel_when_streamingBedrockResponse() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"end_turn\"}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        JsonNode messageStart = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "message_start".equals(node.path("type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(messageStart.at("/message/model").asText()).isEqualTo("claude-opus-4.6");
    }

    @Test
    void test_throwsIOException_when_streamEndsBeforeMessageStop() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "contentBlockDelta", "{\"contentBlockIndex\":0,\"delta\":{\"text\":\"partial plan\"}}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act / Assert
        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        )).isInstanceOf(java.io.EOFException.class)
                .hasMessageContaining("before messageStop");
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .doesNotContain("event: message_delta")
                .doesNotContain("event: message_stop");
    }

    @Test
    void test_mapsRefusal_when_bedrockContentIsFiltered() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"content_filtered\"}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        JsonNode messageDelta = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(messageDelta.at("/delta/stop_reason").asText()).isEqualTo("refusal");
    }

    @Test
    void test_mapsContextWindowStop_when_bedrockContextWindowIsExceeded() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"model_context_window_exceeded\"}");
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        JsonNode messageDelta = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(messageDelta.at("/delta/stop_reason").asText())
                .isEqualTo("model_context_window_exceeded");
    }

    @Test
    void test_throwsIOException_when_bedrockToolUseIsMalformed() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeEvent(upstream, "messageStart", "{\"role\":\"assistant\"}");
        writeEvent(upstream, "messageStop", "{\"stopReason\":\"malformed_tool_use\"}");

        // Act / Assert
        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CONVERSE, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                new ByteArrayOutputStream()
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("malformed_tool_use");
    }

    @Test
    void test_emitsClaudeToolUse_when_responsesStreamsCustomToolCall() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_item.added","output_index":0,"item":{"type":"custom_tool_call","call_id":"custom_1","name":"apply_patch"}}

                data: {"type":"response.custom_tool_call_input.delta","output_index":0,"delta":"*** Begin Patch"}

                data: {"type":"response.custom_tool_call_input.done","output_index":0,"input":"*** Begin Patch"}

                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"custom_tool_call","call_id":"custom_1","name":"apply_patch","input":"*** Begin Patch"}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        JsonNode toolStart = events.stream()
                .filter(node -> "tool_use".equals(node.at("/content_block/type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(toolStart.at("/content_block/id").asText())
                .startsWith("toolu_api2api_custom_");
        JsonNode inputDelta = events.stream()
                .filter(node -> "input_json_delta".equals(node.at("/delta/type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(objectMapper.readTree(inputDelta.at("/delta/partial_json").asText()).path("input").asText())
                .isEqualTo("*** Begin Patch");
    }

    @Test
    void test_removesEmptyPages_when_responsesStreamsReadArguments() throws Exception {
        // Arrange
        String arguments = "{\"file_path\":\"README.md\",\"pages\":\"\"}";
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_item.added","output_index":0,"item":{"type":"function_call","call_id":"call_1","name":"Read"}}

                data: {"type":"response.function_call_arguments.delta","output_index":0,"delta":"{\\\"file_path\\\":\\\"README.md\\\","}

                data: {"type":"response.function_call_arguments.delta","output_index":0,"delta":"\\\"pages\\\":\\\"\\\"}"}

                data: {"type":"response.function_call_arguments.done","output_index":0,"arguments":%s}

                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"function_call","call_id":"call_1","name":"Read","arguments":%s}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """.formatted(
                        objectMapper.writeValueAsString(arguments),
                        objectMapper.writeValueAsString(arguments)
                );
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        List<String> partialJson = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "input_json_delta".equals(node.at("/delta/type").asText()))
                .map(node -> node.at("/delta/partial_json").asText())
                .toList();
        assertThat(partialJson).containsExactly("{\"file_path\":\"README.md\"}");
    }

    @Test
    void test_emitsOpaqueThinkingState_when_responsesStreamsCompaction() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"compaction","id":"cmp_1","encrypted_content":"encrypted"}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        assertThat(events.stream().anyMatch(node -> "Context compacted."
                .equals(node.at("/delta/thinking").asText()))).isTrue();
        assertThat(events.stream().anyMatch(node -> "Conversation compacted."
                .equals(node.at("/delta/text").asText()))).isTrue();
        assertThat(events.stream().anyMatch(node -> node.at("/delta/signature").asText()
                .startsWith(ResponsesReasoningBridge.ITEM_SIGNATURE_PREFIX))).isTrue();
        JsonNode messageDelta = events.stream()
                .filter(node -> "message_delta".equals(node.path("type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(messageDelta.at("/delta/stop_reason").asText()).isEqualTo("pause_turn");
    }

    @Test
    void test_emitsVisibleCompactText_when_compactionAliasOnlyAppearsInAddedEvent() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_item.added","output_index":0,"item":{"type":"compaction_summary","id":"cmp_1","encrypted_content":"encrypted"}}

                data: {"type":"response.completed","response":{"status":"completed","output":[],"usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        assertThat(dataEvents(downstream.toString(StandardCharsets.UTF_8))).anySatisfy(event ->
                assertThat(event.at("/delta/text").asText()).isEqualTo("Conversation compacted."));
    }

    @Test
    void test_linksProgramCallerToServerTool_when_responsesStreamsProgrammaticCall() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"program","id":"prog_1","call_id":"call_prog_1","code":"await tools.Read({});","fingerprint":"opaque"}}

                data: {"type":"response.output_item.added","output_index":1,"item":{"type":"function_call","call_id":"call_1","name":"Read","caller":{"type":"program","caller_id":"call_prog_1"}}}

                data: {"type":"response.function_call_arguments.done","output_index":1,"arguments":"{}"}

                data: {"type":"response.output_item.done","output_index":1,"item":{"type":"function_call","call_id":"call_1","name":"Read","arguments":"{}","caller":{"type":"program","caller_id":"call_prog_1"}}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        JsonNode serverTool = events.stream()
                .filter(node -> "server_tool_use".equals(node.at("/content_block/type").asText()))
                .findFirst()
                .orElseThrow();
        JsonNode clientTool = events.stream()
                .filter(node -> "tool_use".equals(node.at("/content_block/type").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(clientTool.at("/content_block/caller/type").asText())
                .isEqualTo("code_execution_20260521");
        assertThat(clientTool.at("/content_block/caller/tool_id").asText())
                .isEqualTo(serverTool.at("/content_block/id").asText());
    }

    @Test
    void test_mapsCacheWriteUsage_when_responsesStreamCompletes() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":20,"output_tokens":2,"input_tokens_details":{"cached_tokens":3,"cache_write_tokens":4}}}}

                data: [DONE]

                """;

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream()
        );

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(13);
        assertThat(usage.cacheCreationInputTokens()).isEqualTo(4);
        assertThat(usage.cacheReadInputTokens()).isEqualTo(3);
        assertThat(usage.totalTokens()).isEqualTo(22);
    }

    @Test
    void test_emitsCompletedText_when_responsesOnlySendsDoneEvent() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_text.done","output_index":0,"text":"fallback text"}

                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"message"}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        assertThat(dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .anyMatch(node -> "fallback text".equals(node.at("/delta/text").asText())))
                .isTrue();
    }

    @Test
    void test_recoversOutputItems_when_responsesOnlySendsCompletedEnvelope() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.completed","response":{"id":"resp_1","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"completed envelope text"}]}],"usage":{"input_tokens":1,"output_tokens":1}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        );

        // Assert
        assertThat(dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .anyMatch(node -> "completed envelope text".equals(node.at("/delta/text").asText())))
                .isTrue();
    }

    @Test
    void test_throwsEofException_when_responsesStreamEndsBeforeTerminalEvent() {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_text.delta","output_index":0,"delta":"partial"}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act / Assert
        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        )).isInstanceOf(java.io.EOFException.class)
                .hasMessageContaining("before a terminal response event");
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .doesNotContain("event: message_delta")
                .doesNotContain("event: message_stop");
    }

    @Test
    void test_throwsIOException_when_responsesStreamReportsFailure() {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.failed","response":{"status":"failed","error":{"message":"sandbox unavailable"}}}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act / Assert
        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("sandbox unavailable");
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .doesNotContain("event: message_delta")
                .doesNotContain("event: message_stop");
    }

    private GatewayStreamingConversionContext context(
            ProtocolType upstreamProtocol,
            ProtocolType clientProtocol
    ) {
        String requestedModel = clientProtocol == ProtocolType.CLAUDE_MESSAGES
                ? "claude-opus-4.6"
                : "gpt-5.5";
        return GatewayStreamingConversionContext.of(
                upstreamProtocol,
                clientProtocol,
                ModelName.of(requestedModel),
                ProviderChannelId.of(1L),
                ModelName.of("anthropic.claude-opus-4-6-v1:0")
        );
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

    private void writeEvent(OutputStream outputStream, String eventType, String payload) throws Exception {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(":event-type", eventType);
        headers.put(":content-type", "application/json");
        headers.put(":message-type", "event");
        writeFrame(outputStream, headers, payload);
    }

    private void writeModeledException(OutputStream outputStream, String exceptionType, String payload) throws Exception {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(":content-type", "application/json");
        headers.put(":message-type", "exception");
        headers.put(":exception-type", exceptionType);
        writeFrame(outputStream, headers, payload);
    }

    private void writeFrame(OutputStream outputStream, Map<String, String> headerValues, String payload) throws Exception {
        byte[] headers = headers(headerValues);
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

    private byte[] headers(Map<String, String> headerValues) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Map.Entry<String, String> entry : headerValues.entrySet()) {
            writeStringHeader(outputStream, entry.getKey(), entry.getValue());
        }
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

    private static final class FlushAwareOutputStream extends ByteArrayOutputStream {
        private final CountDownLatch flushed = new CountDownLatch(1);

        @Override
        public void flush() throws java.io.IOException {
            super.flush();
            flushed.countDown();
        }

        private boolean awaitFlush() throws InterruptedException {
            return flushed.await(2, TimeUnit.SECONDS);
        }
    }
}
