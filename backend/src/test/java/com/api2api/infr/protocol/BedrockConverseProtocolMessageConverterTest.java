package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BedrockConverseProtocolMessageConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
    private final SseEventTransformer sseEventTransformer = new SseEventTransformer();

    @Test
    void shouldMapClaudeToolsThinkingAndToolResultToBedrockRequest() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json,
                null,
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST,
                sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":64,
                  "thinking":{"type":"enabled","budget_tokens":1024},
                  "tools":[{"name":"get_weather","description":"weather","input_schema":{"type":"object"}}],
                  "tool_choice":{"type":"tool","name":"get_weather"},
                  "messages":[
                    {"role":"user","content":"hello"},
                    {"role":"assistant","content":[{"type":"tool_use","id":"toolu_1","name":"get_weather","input":{"city":"BJ"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"toolu_1","content":"sunny"}]}
                  ]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/inputSchema/json/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/toolConfig/toolChoice/tool/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/additionalModelRequestFields/thinking/type").asText()).isEqualTo("enabled");
        assertThat(mapped.at("/messages/1/content/0/toolUse/toolUseId").asText()).isEqualTo("toolu_1");
        assertThat(mapped.at("/messages/2/content/0/toolResult/toolUseId").asText()).isEqualTo("toolu_1");
        assertThat(mapped.at("/messages/2/content/0/toolResult/content/0/text").asText()).isEqualTo("sunny");
    }

    @Test
    void shouldMapBedrockToolUseAndReasoningToClaudeResponse() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json,
                new BedrockConverseUsageExtractor(),
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolConversionDirection.RESPONSE,
                sseEventTransformer
        );
        String body = """
                {
                  "output":{"message":{"content":[
                    {"reasoningContent":{"reasoningText":{"text":"think","signature":"sig"}}},
                    {"toolUse":{"toolUseId":"toolu_1","name":"get_weather","input":{"city":"BJ"}}}
                  ]}},
                  "stopReason":"tool_use",
                  "usage":{"inputTokens":10,"outputTokens":5,"cacheReadInputTokens":1,"cacheWriteInputTokens":2}
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CONVERSE, body, false),
                ProtocolConversionRequest.of(false, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("thinking");
        assertThat(mapped.at("/content/0/thinking").asText()).isEqualTo("think");
        assertThat(mapped.at("/content/0/signature").asText()).isEqualTo("sig");
        assertThat(mapped.at("/content/1/type").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/content/1/id").asText()).isEqualTo("toolu_1");
        assertThat(mapped.at("/stop_reason").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/usage/input_tokens").asLong()).isEqualTo(10);
        assertThat(result.usage()).isPresent();
        assertThat(result.usage().orElseThrow().totalTokens()).isEqualTo(18);
    }
}
