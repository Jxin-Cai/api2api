package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeMessagesOpenAIResponsesConversionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapStreamingClaudeToolsAndToolResultsToResponsesRequest() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration()
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "stream":true,
                  "max_tokens":256,
                  "system":"Be concise",
                  "thinking":{"type":"enabled","budget_tokens":4096},
                  "tools":[{"name":"get_weather","description":"weather","input_schema":{"type":"object"}}],
                  "tool_choice":{"type":"tool","name":"get_weather"},
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"call_1","name":"get_weather","input":{"city":"BJ"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call_1","content":"sunny"}]}
                  ]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, true),
                ProtocolConversionRequest.of(true, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("stream").asBoolean()).isTrue();
        assertThat(mapped.at("/tools/0/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/tools/0/parameters/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/input/0/role").asText()).isEqualTo("developer");
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("Be concise");
        assertThat(mapped.at("/input/1/type").asText()).isEqualTo("function_call");
        assertThat(mapped.at("/input/1/call_id").asText()).isEqualTo("call_1");
        assertThat(mapped.at("/input/2/type").asText()).isEqualTo("function_call_output");
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("high");
    }

    @Test
    void shouldMapNonStreamingResponsesFunctionCallToClaudeToolUse() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration()
                .openAIResponsesToClaudeMessagesResponse(
                        json,
                        new OpenAIResponsesUsageExtractor(),
                        new SseEventTransformer()
                );
        String body = """
                {
                  "id":"resp_1",
                  "model":"gpt-5.5",
                  "status":"completed",
                  "output":[{
                    "type":"function_call",
                    "call_id":"call_1",
                    "name":"get_weather",
                    "arguments":"{\\\"city\\\":\\\"Beijing\\\"}"
                  }],
                  "usage":{"input_tokens":20,"output_tokens":5,"input_tokens_details":{"cached_tokens":3}}
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/content/0/id").asText()).isEqualTo("call_1");
        assertThat(mapped.at("/content/0/input/city").asText()).isEqualTo("Beijing");
        assertThat(mapped.path("stop_reason").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/usage/cache_read_input_tokens").asLong()).isEqualTo(3);
    }
}
