package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BedrockClaudeMessagesProtocolMessageConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
    private final SseEventTransformer sseEventTransformer = new SseEventTransformer();

    @Test
    void test_preservesClaudeNativeFeatures_when_convertingRequestToBedrockInvoke() throws Exception {
        BedrockClaudeMessagesProtocolMessageConverter converter = converter(
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                ProtocolConversionDirection.REQUEST,
                null
        );
        String body = """
                {
                  "model":"claude-opus-4.8",
                  "stream":true,
                  "max_tokens":4096,
                  "thinking":{"type":"adaptive"},
                  "context_management":{"edits":[{"type":"compact_20260112"}]},
                  "tools":[
                    {"type":"tool_search_tool_regex_20251119","name":"tool_search"},
                    {"name":"Read","defer_loading":true,"input_schema":{"type":"object"}}
                  ],
                  "messages":[
                    {"role":"user","content":[{"type":"mid_conv_system","content":[{"type":"text","text":"new rule"}]}]},
                    {"role":"user","content":"inspect"}
                  ]
                }
                """;

        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, true),
                ProtocolConversionRequest.of(true, true, true)
        ).body());

        assertThat(mapped.has("model")).isFalse();
        assertThat(mapped.has("stream")).isFalse();
        assertThat(mapped.path("anthropic_version").asText()).isEqualTo("bedrock-2023-05-31");
        assertThat(mapped.at("/thinking/type").asText()).isEqualTo("adaptive");
        assertThat(mapped.at("/context_management/edits/0/type").asText()).isEqualTo("compact_20260112");
        assertThat(mapped.at("/tools/0/type").asText()).isEqualTo("tool_search_tool_regex_20251119");
        assertThat(mapped.at("/tools/1/defer_loading").asBoolean()).isTrue();
        assertThat(mapped.at("/messages/0/content/0/type").asText()).isEqualTo("mid_conv_system");
    }

    @Test
    void test_preservesClaudeResponseAndUsage_when_bedrockInvokeReturnsNativeMessage() throws Exception {
        BedrockClaudeMessagesProtocolMessageConverter converter = converter(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                ProtocolType.CLAUDE_MESSAGES,
                ProtocolConversionDirection.RESPONSE,
                new ClaudeMessagesUsageExtractor()
        );
        String body = """
                {
                  "id":"msg_1","type":"message","role":"assistant","model":"claude-opus-4.8",
                  "content":[{"type":"tool_use","id":"toolu_1","name":"Read","input":{}}],
                  "stop_reason":"tool_use","stop_sequence":null,
                  "usage":{"input_tokens":10,"output_tokens":4,"cache_read_input_tokens":3}
                }
                """;

        var result = converter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );
        JsonNode mapped = objectMapper.readTree(result.body());

        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("tool_use");
        assertThat(mapped.path("stop_reason").asText()).isEqualTo("tool_use");
        assertThat(result.usage()).isPresent();
        assertThat(result.usage().orElseThrow().totalTokens()).isEqualTo(17);
    }

    private BedrockClaudeMessagesProtocolMessageConverter converter(
            ProtocolType source,
            ProtocolType target,
            ProtocolConversionDirection direction,
            UnifiedUsageExtractor usageExtractor
    ) {
        return new BedrockClaudeMessagesProtocolMessageConverter(
                json, usageExtractor, source, target, direction, sseEventTransformer);
    }
}
