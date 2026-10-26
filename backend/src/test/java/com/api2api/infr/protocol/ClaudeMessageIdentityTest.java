package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaudeMessageIdentityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UnifiedStreamingConversionAdapter streamingAdapter =
            new UnifiedStreamingConversionAdapter(objectMapper);
    private final ProtocolConverterConfiguration configuration =
            new ProtocolConverterConfiguration(new ProtocolConversionProperties());

    @ParameterizedTest
    @ValueSource(strings = {"{\"id\":\"resp_%d\"}", "{}", "{\"id\":null}", "{\"id\":\"\"}", "{\"id\":\"  \"}"})
    void test_generatesDistinctMessageIds_when_convertingSeparateResponsesStreams(String metadata) throws Exception {
        // Arrange
        List<String> messageIds = new ArrayList<>();

        // Act
        for (int invocation = 0; invocation < 16; invocation++) {
            String upstream = responsesStream(metadata.formatted(invocation));
            messageIds.addAll(streamMessageIds(ProtocolType.OPENAI_RESPONSES, upstream));
        }

        // Assert
        assertThat(messageIds).hasSize(16).doesNotHaveDuplicates()
                .allSatisfy(id -> assertThat(id).startsWith("msg_"));
    }

    @Test
    void test_emitsOneMessageIdentity_when_responsesStreamContainsMultipleOutputItems() throws Exception {
        // Arrange
        String upstream = """
                data: {"type":"response.created","response":{"id":"resp_1"}}

                data: {"type":"response.output_text.delta","output_index":0,"delta":"Let me check."}

                data: {"type":"response.output_item.added","output_index":1,"item":{"type":"function_call","id":"fc_1","call_id":"call_1","name":"Bash","arguments":""}}

                data: {"type":"response.output_item.done","output_index":1,"item":{"type":"function_call","id":"fc_1","call_id":"call_1","name":"Bash","arguments":"{}"}}

                data: {"type":"response.completed","response":{"id":"resp_1","status":"completed"}}

                """;

        // Act
        List<String> messageIds = streamMessageIds(ProtocolType.OPENAI_RESPONSES, upstream);

        // Assert
        assertThat(messageIds).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"id\":null}", "{\"id\":\"\"}", "{\"id\":\"  \"}"})
    void test_generatesDistinctMessageIds_when_chatStreamsHaveNoUsableId(String metadata) throws Exception {
        // Arrange
        ObjectNode chunk = (ObjectNode) objectMapper.readTree("""
                {"choices":[{"delta":{"content":"answer"},"finish_reason":"stop"}]}
                """);
        chunk.setAll((ObjectNode) objectMapper.readTree(metadata));
        String upstream = "data: " + chunk + "\n\ndata: [DONE]\n\n";

        // Act
        List<String> messageIds = new ArrayList<>(streamMessageIds(ProtocolType.OPENAI_CHAT_COMPLETIONS, upstream));
        messageIds.addAll(streamMessageIds(ProtocolType.OPENAI_CHAT_COMPLETIONS, upstream));

        // Assert
        assertThat(messageIds).hasSize(2).doesNotHaveDuplicates()
                .allSatisfy(id -> assertThat(id).startsWith("msg_"));
    }

    @Test
    void test_preservesOneUpstreamMessageId_when_chatStreamContainsMultipleChunks() throws Exception {
        // Arrange
        String upstream = """
                data: {"id":"chatcmpl_1","choices":[{"delta":{"role":"assistant"},"finish_reason":null}]}

                data: {"id":"chatcmpl_1","choices":[{"delta":{"content":"answer"},"finish_reason":"stop"}]}

                data: [DONE]

                """;

        // Act
        List<String> messageIds = streamMessageIds(ProtocolType.OPENAI_CHAT_COMPLETIONS, upstream);

        // Assert
        assertThat(messageIds).containsExactly("chatcmpl_1");
    }

    @ParameterizedTest
    @MethodSource("responsesWithoutIds")
    void test_generatesDistinctMessageIds_when_nonStreamingResponsesHaveNoUsableId(
            ProtocolType upstreamProtocol, String metadata
    ) throws Exception {
        // Arrange
        ProtocolMessageConverter converter = responseConverter(upstreamProtocol);
        String body = responseBody(upstreamProtocol, metadata);

        // Act
        List<String> messageIds = List.of(
                convertedMessageId(converter, upstreamProtocol, body),
                convertedMessageId(converter, upstreamProtocol, body));

        // Assert
        assertThat(messageIds).doesNotHaveDuplicates()
                .allSatisfy(id -> assertThat(id).startsWith("msg_"));
    }

    @ParameterizedTest
    @EnumSource(value = ProtocolType.class, names = {"OPENAI_RESPONSES", "OPENAI_CHAT_COMPLETIONS"})
    void test_preservesUpstreamMessageId_when_nonStreamingResponseHasUsableId(ProtocolType upstreamProtocol)
            throws Exception {
        // Arrange
        ProtocolMessageConverter converter = responseConverter(upstreamProtocol);
        String upstreamId = upstreamProtocol == ProtocolType.OPENAI_RESPONSES ? "resp_1" : "chatcmpl_1";
        String body = responseBody(upstreamProtocol, objectMapper.createObjectNode().put("id", upstreamId).toString());

        // Act
        String messageId = convertedMessageId(converter, upstreamProtocol, body);

        // Assert
        assertThat(messageId).isEqualTo(upstreamId);
    }

    private static Stream<Arguments> responsesWithoutIds() {
        return Stream.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_CHAT_COMPLETIONS)
                .flatMap(protocol -> Stream.of("{}", "{\"id\":null}", "{\"id\":\"\"}", "{\"id\":\"  \"}")
                        .map(metadata -> Arguments.of(protocol, metadata)));
    }

    private String responsesStream(String metadata) throws Exception {
        ObjectNode response = (ObjectNode) objectMapper.readTree(metadata);
        response.put("status", "completed");
        ObjectNode created = objectMapper.createObjectNode().put("type", "response.created");
        created.set("response", response);
        ObjectNode completed = objectMapper.createObjectNode().put("type", "response.completed");
        completed.set("response", response);
        return "data: " + created + "\n\n"
                + "data: {\"type\":\"response.output_text.delta\",\"output_index\":0,\"delta\":\"answer\"}\n\n"
                + "data: " + completed + "\n\n";
    }

    private List<String> streamMessageIds(ProtocolType upstreamProtocol, String upstream) throws Exception {
        GatewayStreamingConversionContext context = GatewayStreamingConversionContext.of(
                upstreamProtocol, ProtocolType.CLAUDE_MESSAGES, ModelName.of("claude-test"),
                ProviderChannelId.of(1L), ModelName.of("upstream-test"));
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();
        streamingAdapter.transform(context,
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)), downstream);
        List<String> messageIds = new ArrayList<>();
        for (String line : downstream.toString(StandardCharsets.UTF_8).lines().toList()) {
            if (line.startsWith("data: ")) {
                JsonNode event = objectMapper.readTree(line.substring("data: ".length()));
                if ("message_start".equals(event.path("type").asText())) {
                    messageIds.add(event.at("/message/id").asText());
                }
            }
        }
        return messageIds;
    }

    private ProtocolMessageConverter responseConverter(ProtocolType upstreamProtocol) {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        return switch (upstreamProtocol) {
            case OPENAI_RESPONSES -> configuration.openAIResponsesToClaudeMessagesResponse(
                    json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
            case OPENAI_CHAT_COMPLETIONS -> configuration.openAIChatToClaudeMessagesResponse(
                    json, new OpenAIChatCompletionsUsageExtractor(), new SseEventTransformer());
            default -> throw new IllegalArgumentException("Unsupported test protocol: " + upstreamProtocol);
        };
    }

    private String responseBody(ProtocolType upstreamProtocol, String metadata) throws Exception {
        String payload = upstreamProtocol == ProtocolType.OPENAI_RESPONSES
                ? "{\"model\":\"upstream-test\",\"status\":\"completed\",\"output_text\":\"answer\"}"
                : "{\"model\":\"upstream-test\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"answer\"},\"finish_reason\":\"stop\"}]}";
        ObjectNode response = (ObjectNode) objectMapper.readTree(payload);
        response.setAll((ObjectNode) objectMapper.readTree(metadata));
        return response.toString();
    }

    private String convertedMessageId(ProtocolMessageConverter converter, ProtocolType upstreamProtocol, String body)
            throws Exception {
        String converted = converter.convert(ProtocolPayload.of(upstreamProtocol, body, false),
                ProtocolConversionRequest.of(false, false, false)).body();
        return objectMapper.readTree(converted).path("id").asText();
    }
}
