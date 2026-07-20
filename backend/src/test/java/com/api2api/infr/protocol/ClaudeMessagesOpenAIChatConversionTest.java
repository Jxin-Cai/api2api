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

    @Test
    void test_placesToolRepliesImmediatelyAfterAssistantCalls_when_userContentIntervenes() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"deepseek-v4-pro",
                  "messages":[
                    {"role":"assistant","content":[
                      {"type":"tool_use","id":"call_a","name":"Read","input":{"path":"a"}},
                      {"type":"tool_use","id":"call_b","name":"Read","input":{"path":"b"}},
                      {"type":"tool_use","id":"call_unanswered","name":"Read","input":{"path":"c"}}
                    ]},
                    {"role":"user","content":[
                      {"type":"text","text":"approval recorded"},
                      {"type":"tool_result","tool_use_id":"call_b","content":"B"},
                      {"type":"tool_result","tool_use_id":"call_a","content":"A"}
                    ]}
                  ]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, true);

        // Assert
        assertThat(mapped.at("/messages/0/tool_calls").size()).isEqualTo(2);
        assertThat(mapped.at("/messages/0/tool_calls/0/id").asText()).isEqualTo("call_a");
        assertThat(mapped.at("/messages/1/tool_call_id").asText()).isEqualTo("call_a");
        assertThat(mapped.at("/messages/2/tool_call_id").asText()).isEqualTo("call_b");
        assertThat(mapped.at("/messages/3/content/0/text").asText()).isEqualTo("approval recorded");
    }

    @Test
    void test_dropsOrphanToolReply_when_assistantCallIsMissing() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"deepseek-v4-pro",
                  "messages":[{"role":"user","content":[
                    {"type":"tool_result","tool_use_id":"orphan","content":"stale"},
                    {"type":"text","text":"continue"}
                  ]}]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, true);

        // Assert
        assertThat(mapped.path("messages")).hasSize(1);
        assertThat(mapped.at("/messages/0/role").asText()).isEqualTo("user");
        assertThat(mapped.at("/messages/0/content").asText()).isEqualTo("continue");
    }

    @Test
    void test_filtersBillingHeaderAndSeparatesTextBlocks_when_systemIsStructured() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"deepseek-v4-pro",
                  "system":[
                    {"type":"text","text":"first"},
                    {"type":"text","text":"x-anthropic-billing-header: internal"},
                    {"type":"text","text":"second"}
                  ],
                  "messages":[{"role":"user","content":"hello"}]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.at("/messages/0/content").asText()).isEqualTo("first\n\nsecond");
    }

    @Test
    void test_separatesAssistantTextBlocks_when_multipleBlocksArePresent() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"deepseek-v4-pro",
                  "messages":[{"role":"assistant","content":[
                    {"type":"text","text":"first"},
                    {"type":"text","text":"second"}
                  ]}]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.at("/messages/0/content").asText()).isEqualTo("first\n\nsecond");
    }

    @Test
    void test_usesEmptyPlaceholder_when_toolResultHasNoText() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"deepseek-v4-pro",
                  "messages":[
                    {"role":"assistant","content":[
                      {"type":"tool_use","id":"call_1","name":"Read","input":{}}
                    ]},
                    {"role":"user","content":[
                      {"type":"tool_result","tool_use_id":"call_1","content":[]}
                    ]}
                  ]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, true);

        // Assert
        assertThat(mapped.at("/messages/1/content").asText()).isEqualTo("(empty)");
    }

    @Test
    void test_skipsEmptyImages_when_sourceHasNoData() throws Exception {
        // Arrange
        String body = """
                {
                  "model":"deepseek-v4-pro",
                  "messages":[{"role":"user","content":[
                    {"type":"image","source":{"type":"base64","media_type":"image/png","data":""}},
                    {"type":"text","text":"describe"}
                  ]}]
                }
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.at("/messages/0/content").asText()).isEqualTo("describe");
    }

    @Test
    void test_appliesChatTokenFloor_when_claudeLimitIsTooSmall() throws Exception {
        // Arrange
        String body = """
                {"model":"gpt-5.6","max_tokens":10,"messages":[{"role":"user","content":"hello"}]}
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.path("max_completion_tokens").asInt()).isEqualTo(128);
    }

    @Test
    void test_defaultsReasoningEffortToMedium_when_targetIsReasoningModel() throws Exception {
        // Arrange
        String body = """
                {"model":"gpt-5.6","messages":[{"role":"user","content":"hello"}]}
                """;

        // Act
        JsonNode mapped = convertRequest(body, false);

        // Assert
        assertThat(mapped.path("reasoning_effort").asText()).isEqualTo("medium");
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
