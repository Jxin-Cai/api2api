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
    void test_emitsClaudeSse_when_invokeModelFrameContainsNativeClaudeEvent() throws Exception {
        // Arrange
        String messageDelta = """
                {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"input_tokens":3,"output_tokens":2}}
                """;
        String messageStop = """
                {"type":"message_stop"}
                """;
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeClaudeInvokeModelEvent(upstream, messageDelta);
        writeClaudeInvokeModelEvent(upstream, messageStop);
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        String sse = downstream.toString(StandardCharsets.UTF_8);
        assertThat(sse).contains("event: message_delta\n"
                + "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"input_tokens\":3,\"output_tokens\":2}}\n\n");
        assertThat(usage.usageKnown()).isTrue();
        assertThat(usage.totalTokens()).isEqualTo(5);
    }

    @Test
    void test_restoresClientModel_when_invokeModelStartsMessage() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_start","message":{"id":"msg_1","model":"claude-opus-4-6","usage":{"input_tokens":3,"output_tokens":0}}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_stop"}
                """);
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        JsonNode messageStart = dataEvents(downstream.toString(StandardCharsets.UTF_8)).get(0);
        assertThat(messageStart.at("/message/model").asText()).isEqualTo("claude-opus-4.6");
    }

    @Test
    void test_combinesUsage_when_invokeModelSplitsInputAndOutputUsage() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_start","message":{"usage":{"input_tokens":10,"cache_read_input_tokens":4,"output_tokens":0}}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":3}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_stop"}
                """);
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(10);
        assertThat(usage.outputTokens()).isEqualTo(3);
        assertThat(usage.cacheReadInputTokens()).isEqualTo(4);
    }

    @Test
    void test_unwrapsStructuredOutputInput_when_invokeModelStreamsWrappedArray() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"toolu_1","name":"StructuredOutput","input":{}}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\\"__api2api_structured_output\\":[\\"design\\","}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"\\"security\\"]}"}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"content_block_stop","index":0}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"input_tokens":3,"output_tokens":2}}
                """);
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_stop"}
                """);
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        assertThat(events.stream()
                .filter(node -> "input_json_delta".equals(node.at("/delta/type").asText()))
                .map(node -> node.at("/delta/partial_json").asText())
                .toList()).containsExactly("[\"design\",\"security\"]");
    }

    @Test
    void test_removesBedrockExtensions_when_invokeModelReturnsProviderMetadata() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_stop","amazon-bedrock-invocationMetrics":{"inputTokenCount":3}}
                """);
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                downstream
        );

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .doesNotContain("amazon-bedrock-invocationMetrics");
    }

    @Test
    void test_failsClosed_when_invokeModelStreamEndsBeforeMessageStop() throws Exception {
        // Arrange
        ByteArrayOutputStream upstream = new ByteArrayOutputStream();
        writeClaudeInvokeModelEvent(upstream, """
                {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":2}}
                """);

        // Act / Assert
        assertThatThrownBy(() -> adapter.transform(
                context(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.toByteArray()),
                new ByteArrayOutputStream()
        )).isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("message_stop");
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
    void test_emitsResponsesLifecycle_when_chatStreamTargetsResponses() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl-1","model":"upstream-model","choices":[{"index":0,"delta":{"reasoning_content":"plan"},"finish_reason":null}]}

                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"content":"answer"},"finish_reason":null}]}

                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":4,"completion_tokens":2}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        assertThat(events).extracting(node -> node.path("type").asText()).containsSequence(
                "response.created",
                "response.output_item.added",
                "response.reasoning_summary_part.added",
                "response.reasoning_summary_text.delta",
                "response.reasoning_summary_text.done",
                "response.reasoning_summary_part.done",
                "response.output_item.done",
                "response.output_item.added",
                "response.content_part.added",
                "response.output_text.delta",
                "response.output_text.done",
                "response.content_part.done",
                "response.output_item.done",
                "response.completed");
        assertThat(downstream.toString(StandardCharsets.UTF_8)).doesNotContain("message_start");
    }

    @Test
    void test_closesResponsesToolCallWithCompleteArguments_when_chatArgumentsArriveInFirstChunk() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_1","type":"function","function":{"name":"Read","arguments":"{\\"path\\":\\"a\\"}"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{},"finish_reason":"tool_calls"}]}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        assertThat(events.stream()
                .filter(node -> "response.function_call_arguments.done".equals(node.path("type").asText()))
                .findFirst().orElseThrow().path("arguments").asText()).isEqualTo("{\"path\":\"a\"}");
        JsonNode completed = events.stream()
                .filter(node -> "response.completed".equals(node.path("type").asText()))
                .findFirst().orElseThrow();
        assertThat(completed.at("/response/output/0/type").asText()).isEqualTo("function_call");
        assertThat(completed.at("/response/output/0/status").asText()).isEqualTo("completed");
        assertThat(completed.at("/response/output/0/arguments").asText()).isEqualTo("{\"path\":\"a\"}");
    }

    @Test
    void test_replaysBufferedResponsesToolArguments_when_chatToolNameArrivesLate() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"call_1","function":{"arguments":"{\\"path\\":"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"name":"Read","arguments":"\\"a\\"}"}}]},"finish_reason":null}]}

                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{},"finish_reason":"tool_calls"}]}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        String arguments = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "response.function_call_arguments.delta".equals(node.path("type").asText()))
                .map(node -> node.path("delta").asText())
                .reduce("", String::concat);
        assertThat(arguments).isEqualTo("{\"path\":\"a\"}");
    }

    @Test
    void test_mapsStreamingUsageAndLengthStatus_when_chatStreamTargetsResponses() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl-1","choices":[{"index":0,"delta":{"content":"partial"},"finish_reason":"length"}],"usage":{"prompt_tokens":20,"completion_tokens":3,"prompt_tokens_details":{"cached_tokens":4,"cache_write_tokens":2}}}

                data: [DONE]

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        JsonNode completed = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "response.completed".equals(node.path("type").asText()))
                .findFirst().orElseThrow();
        assertThat(completed.at("/response/status").asText()).isEqualTo("incomplete");
        assertThat(completed.at("/response/incomplete_details/reason").asText())
                .isEqualTo("max_output_tokens");
        assertThat(completed.at("/response/usage/input_tokens_details/cached_tokens").asLong()).isEqualTo(4);
        assertThat(usage.inputTokens()).isEqualTo(14);
        assertThat(usage.cacheReadInputTokens()).isEqualTo(4);
        assertThat(usage.cacheCreationInputTokens()).isEqualTo(2);
    }

    @Test
    void test_emitsResponsesItems_when_claudeStreamContainsTextAndToolUse() throws Exception {
        // Arrange
        String upstream = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_1","model":"claude-upstream","usage":{"input_tokens":5,"output_tokens":0}}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"answer"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: content_block_start
                data: {"type":"content_block_start","index":1,"content_block":{"type":"tool_use","id":"call_1","name":"Read","input":{}}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\\"path\\":\\"a\\"}"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":1}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"output_tokens":3}}

                event: message_stop
                data: {"type":"message_stop"}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        JsonNode completed = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "response.completed".equals(node.path("type").asText()))
                .findFirst().orElseThrow();
        assertThat(completed.at("/response/id").asText()).isEqualTo("resp_1");
        assertThat(completed.at("/response/model").asText()).isEqualTo("gpt-5.5");
        assertThat(completed.at("/response/output_text").asText()).isEqualTo("answer");
        assertThat(completed.at("/response/output/0/type").asText()).isEqualTo("message");
        assertThat(completed.at("/response/output/1/type").asText()).isEqualTo("function_call");
        assertThat(completed.at("/response/output/1/arguments").asText()).isEqualTo("{\"path\":\"a\"}");
    }

    @Test
    void test_mapsClaudeCacheUsageAndMaxTokens_when_streamTargetsResponses() throws Exception {
        // Arrange
        String upstream = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_1","usage":{"input_tokens":10,"cache_creation_input_tokens":2,"cache_read_input_tokens":4,"output_tokens":0}}}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"max_tokens"},"usage":{"output_tokens":3}}

                event: message_stop
                data: {"type":"message_stop"}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = adapter.transform(
                context(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        JsonNode completed = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "response.incomplete".equals(node.path("type").asText()))
                .findFirst().orElseThrow();
        assertThat(completed.at("/response/status").asText()).isEqualTo("incomplete");
        assertThat(completed.at("/response/incomplete_details/reason").asText())
                .isEqualTo("max_output_tokens");
        assertThat(completed.at("/response/usage/input_tokens").asLong()).isEqualTo(16);
        assertThat(usage.inputTokens()).isEqualTo(10);
        assertThat(usage.outputTokens()).isEqualTo(3);
        assertThat(usage.cacheCreationInputTokens()).isEqualTo(2);
        assertThat(usage.cacheReadInputTokens()).isEqualTo(4);
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
    void test_convertsOpenAIResponsesSse_when_targetIsClaudeSseWithToolUse() throws Exception {
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

    @Test
    void test_tunnelsSignatureDeltaThroughEncryptedContent_when_claudeStreamTargetsResponses() throws Exception {
        // Arrange
        String upstream = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_1","usage":{"input_tokens":5,"output_tokens":0}}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"deep thought"}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"anthropic-signature"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":3}}

                event: message_stop
                data: {"type":"message_stop"}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        JsonNode reasoningDone = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "response.output_item.done".equals(node.path("type").asText())
                        && "reasoning".equals(node.at("/item/type").asText()))
                .findFirst().orElseThrow();
        JsonNode restored = ClaudeThinkingStateBridge.decode(
                objectMapper, reasoningDone.at("/item/encrypted_content").asText()).orElseThrow();
        assertThat(restored.path("signature").asText()).isEqualTo("anthropic-signature");
    }

    @Test
    void test_bridgesRedactedThinkingBlock_when_claudeStreamTargetsResponses() throws Exception {
        // Arrange
        String upstream = """
                event: message_start
                data: {"type":"message_start","message":{"id":"msg_1","usage":{"input_tokens":5,"output_tokens":0}}}

                event: content_block_start
                data: {"type":"content_block_start","index":0,"content_block":{"type":"redacted_thinking","data":"opaque-redacted-data"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":0}

                event: content_block_start
                data: {"type":"content_block_start","index":1,"content_block":{"type":"text","text":""}}

                event: content_block_delta
                data: {"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"visible"}}

                event: content_block_stop
                data: {"type":"content_block_stop","index":1}

                event: message_delta
                data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":3}}

                event: message_stop
                data: {"type":"message_stop"}

                """;
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        JsonNode reasoningDone = dataEvents(downstream.toString(StandardCharsets.UTF_8)).stream()
                .filter(node -> "response.output_item.done".equals(node.path("type").asText())
                        && "reasoning".equals(node.at("/item/type").asText()))
                .findFirst().orElseThrow();
        JsonNode restored = ClaudeThinkingStateBridge.decode(
                objectMapper, reasoningDone.at("/item/encrypted_content").asText()).orElseThrow();
        assertThat(restored.path("type").asText()).isEqualTo("redacted_thinking");
        assertThat(restored.path("data").asText()).isEqualTo("opaque-redacted-data");
    }

    @Test
    void test_restoresNativeSignature_when_responsesStreamCarriesBridgedThinkingState() throws Exception {
        // Arrange
        String bridged = ClaudeThinkingStateBridge.encode(objectMapper, objectMapper.readTree("""
                {"type":"thinking","thinking":"prior reasoning","signature":"anthropic-signature"}
                """)).orElseThrow();
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_item.done","output_index":0,"item":{"type":"reasoning","id":"rs_1","summary":[],"encrypted_content":%s}}

                data: {"type":"response.output_item.done","output_index":1,"item":{"type":"message","role":"assistant","content":[{"type":"output_text","text":"ok"}]}}

                data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":5,"output_tokens":3}}}

                """.formatted(objectMapper.writeValueAsString(bridged));
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        adapter.transform(
                context(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES),
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream);

        // Assert
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        JsonNode thinkingDelta = events.stream()
                .filter(node -> "content_block_delta".equals(node.path("type").asText())
                        && "thinking_delta".equals(node.at("/delta/type").asText()))
                .findFirst().orElseThrow();
        assertThat(thinkingDelta.at("/delta/thinking").asText()).isEqualTo("prior reasoning");
        JsonNode signatureDelta = events.stream()
                .filter(node -> "content_block_delta".equals(node.path("type").asText())
                        && "signature_delta".equals(node.at("/delta/type").asText()))
                .findFirst().orElseThrow();
        assertThat(signatureDelta.at("/delta/signature").asText()).isEqualTo("anthropic-signature");
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

    private void writeClaudeInvokeModelEvent(OutputStream upstream, String event) throws Exception {
        String encodedEvent = java.util.Base64.getEncoder()
                .encodeToString(event.getBytes(StandardCharsets.UTF_8));
        writeEvent(upstream, "chunk", "{\"chunk\":{\"bytes\":\"" + encodedEvent + "\"}}");
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
