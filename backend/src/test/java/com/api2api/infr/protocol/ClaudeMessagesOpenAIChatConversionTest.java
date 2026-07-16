package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeMessagesOpenAIChatConversionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
    private final ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration();

    @Test
    void test_preservesClaudeToolCalling_when_convertingToChatCompletions() throws Exception {
        String body = """
                {
                  "model":"gpt-5.6",
                  "max_tokens":512,
                  "service_tier":"standard_only",
                  "tools":[{"name":"Read","description":"read a file",
                    "input_schema":{"type":"object","properties":{"path":{"type":"string"}}},"strict":true}],
                  "tool_choice":{"type":"tool","name":"Read"},
                  "messages":[{"role":"user","content":"inspect"}]
                }
                """;

        JsonNode mapped = convertRequest(body, true);

        assertThat(mapped.path("max_completion_tokens").asInt()).isEqualTo(512);
        assertThat(mapped.path("service_tier").asText()).isEqualTo("default");
        assertThat(mapped.at("/tools/0/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/tools/0/function/name").asText()).isEqualTo("Read");
        assertThat(mapped.at("/tools/0/function/strict").asBoolean()).isTrue();
        assertThat(mapped.at("/tool_choice/function/name").asText()).isEqualTo("Read");
    }

    @Test
    void test_mapsClaudeBase64Document_when_chatSupportsFileContentParts() throws Exception {
        String body = """
                {
                  "model":"gpt-5.5",
                  "messages":[{"role":"user","content":[
                    {"type":"document","title":"notes.txt",
                     "source":{"type":"base64","media_type":"text/plain","data":"bm90ZXM="}}
                  ]}]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.at("/messages/0/content/0/type").asText()).isEqualTo("file");
        assertThat(mapped.at("/messages/0/content/0/file/file_data").asText()).isEqualTo("bm90ZXM=");
        assertThat(mapped.at("/messages/0/content/0/file/filename").asText()).isEqualTo("notes.txt");
    }

    @Test
    void test_rejectsClaudeMcpToolset_when_chatHasNoNativeMcpToolType() {
        String body = """
                {
                  "model":"gpt-5.5",
                  "tools":[{"type":"mcp_toolset","mcp_server_name":"docs"}],
                  "messages":[{"role":"user","content":"search"}]
                }
                """;

        assertThatThrownBy(() -> convertRequest(body, true))
                .hasMessageContaining("CLAUDE_CHAT_TOOL_NOT_SUPPORTED: mcp_toolset");
    }

    private JsonNode convertRequest(String body, boolean toolCallingRequired) throws Exception {
        ProtocolMessageConverter converter = configuration.claudeMessagesToOpenAIChatRequest(
                json, new SseEventTransformer());
        return objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, toolCallingRequired, false)
        ).body());
    }
}
