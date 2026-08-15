package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAIChatResponsesResponseConversionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test_emitsCompleteResponsesItems_when_chatResponseContainsReasoningTextAndToolCall() throws Exception {
        // Arrange
        ProtocolMessageConverter converter = converter();
        String body = """
                {
                  "id":"chatcmpl-1","created":123,"model":"gpt-test",
                  "choices":[{"index":0,"finish_reason":"tool_calls","message":{
                    "role":"assistant","reasoning_content":"inspect",
                    "content":"answer",
                    "tool_calls":[{"id":"call_1","type":"function","function":{"name":"Read","arguments":""}}]
                  }}],
                  "usage":{"prompt_tokens":10,"completion_tokens":3,"total_tokens":13}
                }
                """;

        // Act
        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, body, false),
                ProtocolConversionRequest.of(false, true, true));

        // Assert
        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("status").asText()).isEqualTo("completed");
        assertThat(mapped.path("output_text").asText()).isEqualTo("answer");
        assertThat(mapped.at("/output/0/type").asText()).isEqualTo("reasoning");
        assertThat(mapped.at("/output/1/status").asText()).isEqualTo("completed");
        assertThat(mapped.at("/output/2/id").asText()).startsWith("fc_");
        assertThat(mapped.at("/output/2/arguments").asText()).isEqualTo("{}");
        assertThat(mapped.at("/output/2/status").asText()).isEqualTo("completed");
    }

    @Test
    void test_marksResponsesIncomplete_when_chatCompletionStopsAtLength() throws Exception {
        // Arrange
        ProtocolMessageConverter converter = converter();
        String body = """
                {"id":"chatcmpl-1","model":"gpt-test","choices":[{
                  "finish_reason":"length","message":{"role":"assistant","content":"partial"}
                }],"usage":{"prompt_tokens":1,"completion_tokens":2}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.path("status").asText()).isEqualTo("incomplete");
        assertThat(mapped.at("/incomplete_details/reason").asText()).isEqualTo("max_output_tokens");
    }

    @Test
    void test_preservesRefusalMetadata_when_responsesResponseTargetsChat() throws Exception {
        // Arrange
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesToOpenAIChatResponse(
                        new ProtocolJsonSupport(objectMapper),
                        new OpenAIResponsesUsageExtractor(),
                        new SseEventTransformer());
        String body = """
                {"id":"resp_1","model":"gpt-test","status":"incomplete",
                 "incomplete_details":{"reason":"content_filter"},"output":[{
                   "type":"message","content":[{"type":"refusal","refusal":"blocked"}]
                 }],"usage":{"input_tokens":1,"output_tokens":1}}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, false, false)).body());

        // Assert
        assertThat(mapped.at("/choices/0/message/refusal").asText()).isEqualTo("blocked");
        assertThat(mapped.at("/choices/0/message/content").asText()).isEqualTo("blocked");
        assertThat(mapped.at("/choices/0/finish_reason").asText()).isEqualTo("content_filter");
    }

    private ProtocolMessageConverter converter() {
        return new ProtocolConverterConfiguration(new ProtocolConversionProperties()).openAIChatToOpenAIResponsesResponse(
                new ProtocolJsonSupport(objectMapper),
                new OpenAIChatCompletionsUsageExtractor(),
                new SseEventTransformer());
    }
}
