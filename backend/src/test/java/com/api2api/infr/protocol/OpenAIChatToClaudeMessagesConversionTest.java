package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAIChatToClaudeMessagesConversionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
    private final ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration();

    @Test
    void test_mapsBasicTextMessage_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "max_completion_tokens":1024,
                  "temperature":0.7,
                  "messages":[
                    {"role":"system","content":"You are helpful."},
                    {"role":"user","content":"Hello"}
                  ]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.path("model").asText()).isEqualTo("claude-sonnet-4-20250514");
        assertThat(mapped.path("max_tokens").asInt()).isEqualTo(1024);
        assertThat(mapped.path("temperature").asDouble()).isEqualTo(0.7);
        assertThat(mapped.path("system").asText()).isEqualTo("You are helpful.");
        assertThat(mapped.at("/messages/0/role").asText()).isEqualTo("user");
        assertThat(mapped.at("/messages/0/content/0/type").asText()).isEqualTo("text");
        assertThat(mapped.at("/messages/0/content/0/text").asText()).isEqualTo("Hello");
    }

    @Test
    void test_mapsDeveloperRole_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "messages":[
                    {"role":"developer","content":"System instruction via developer role"},
                    {"role":"user","content":"Hi"}
                  ]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.path("system").asText()).isEqualTo("System instruction via developer role");
        assertThat(mapped.path("messages").size()).isEqualTo(1);
    }

    @Test
    void test_mapsToolDefinitions_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "tools":[{
                    "type":"function",
                    "function":{
                      "name":"get_weather",
                      "description":"Get weather for a location",
                      "parameters":{"type":"object","properties":{"city":{"type":"string"}},"required":["city"]},
                      "strict":true
                    }
                  }],
                  "tool_choice":"required",
                  "parallel_tool_calls":false,
                  "messages":[{"role":"user","content":"Weather in Tokyo?"}]
                }
                """;

        JsonNode mapped = convertRequest(body, true);

        assertThat(mapped.at("/tools/0/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/tools/0/description").asText()).isEqualTo("Get weather for a location");
        assertThat(mapped.at("/tools/0/input_schema/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/tools/0/input_schema/required/0").asText()).isEqualTo("city");
        assertThat(mapped.at("/tools/0/strict").asBoolean()).isTrue();
        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("any");
        assertThat(mapped.at("/tool_choice/disable_parallel_tool_use").asBoolean()).isTrue();
    }

    @Test
    void test_mapsToolCallsAndResults_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "messages":[
                    {"role":"user","content":"Weather?"},
                    {"role":"assistant","content":null,"tool_calls":[{
                      "id":"call_abc123",
                      "type":"function",
                      "function":{"name":"get_weather","arguments":"{\\"city\\":\\"Tokyo\\"}"}
                    }]},
                    {"role":"tool","tool_call_id":"call_abc123","content":"Sunny, 25°C"}
                  ]
                }
                """;

        JsonNode mapped = convertRequest(body, true);

        // assistant message with tool_use block (content=null → no empty text block, tool_use at index 0)
        JsonNode assistantMsg = mapped.path("messages").get(1);
        assertThat(assistantMsg.path("role").asText()).isEqualTo("assistant");
        assertThat(assistantMsg.at("/content/0/type").asText()).isEqualTo("tool_use");
        assertThat(assistantMsg.at("/content/0/id").asText()).isEqualTo("call_abc123");
        assertThat(assistantMsg.at("/content/0/name").asText()).isEqualTo("get_weather");
        assertThat(assistantMsg.at("/content/0/input/city").asText()).isEqualTo("Tokyo");

        // tool result message
        JsonNode toolResultMsg = mapped.path("messages").get(2);
        assertThat(toolResultMsg.path("role").asText()).isEqualTo("user");
        assertThat(toolResultMsg.at("/content/0/type").asText()).isEqualTo("tool_result");
        assertThat(toolResultMsg.at("/content/0/tool_use_id").asText()).isEqualTo("call_abc123");
        assertThat(toolResultMsg.at("/content/0/content/0/text").asText()).isEqualTo("Sunny, 25°C");
    }

    @Test
    void test_mapsImageUrl_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "messages":[{"role":"user","content":[
                    {"type":"text","text":"Describe this image"},
                    {"type":"image_url","image_url":{"url":"data:image/png;base64,iVBORw0KGgo="}}
                  ]}]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.at("/messages/0/content/0/type").asText()).isEqualTo("text");
        assertThat(mapped.at("/messages/0/content/1/type").asText()).isEqualTo("image");
        assertThat(mapped.at("/messages/0/content/1/source/type").asText()).isEqualTo("base64");
        assertThat(mapped.at("/messages/0/content/1/source/media_type").asText()).isEqualTo("image/png");
        assertThat(mapped.at("/messages/0/content/1/source/data").asText()).isEqualTo("iVBORw0KGgo=");
    }

    @Test
    void test_mapsImageUrlExternal_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "messages":[{"role":"user","content":[
                    {"type":"image_url","image_url":{"url":"https://example.com/cat.png"}}
                  ]}]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.at("/messages/0/content/0/type").asText()).isEqualTo("image");
        assertThat(mapped.at("/messages/0/content/0/source/type").asText()).isEqualTo("url");
        assertThat(mapped.at("/messages/0/content/0/source/url").asText()).isEqualTo("https://example.com/cat.png");
    }

    @Test
    void test_mapsStopSequences_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "stop":["END","STOP"],
                  "messages":[{"role":"user","content":"go"}]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.path("stop_sequences").isArray()).isTrue();
        assertThat(mapped.path("stop_sequences").get(0).asText()).isEqualTo("END");
        assertThat(mapped.path("stop_sequences").get(1).asText()).isEqualTo("STOP");
    }

    @Test
    void test_mapsSpecificToolChoice_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "tools":[{"type":"function","function":{"name":"search","parameters":{"type":"object"}}}],
                  "tool_choice":{"type":"function","function":{"name":"search"}},
                  "messages":[{"role":"user","content":"find"}]
                }
                """;

        JsonNode mapped = convertRequest(body, true);

        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("tool");
        assertThat(mapped.at("/tool_choice/name").asText()).isEqualTo("search");
    }

    @Test
    void test_rejectsUnsupportedContentPart_when_convertingChatToClaude() {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "messages":[{"role":"user","content":[
                    {"type":"audio","audio":{"data":"base64data"}}
                  ]}]
                }
                """;

        assertThatThrownBy(() -> convertRequest(body, false))
                .hasMessageContaining("OPENAI_CHAT_CLAUDE_CONTENT_PART_NOT_SUPPORTED: audio");
    }

    @Test
    void test_mapsMultipleSystemMessages_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "messages":[
                    {"role":"system","content":"First system"},
                    {"role":"system","content":"Second system"},
                    {"role":"user","content":"go"}
                  ]
                }
                """;

        JsonNode mapped = convertRequest(body, false);

        assertThat(mapped.path("system").asText()).isEqualTo("First system\nSecond system");
    }

    @Test
    void test_supportsToolCallingRequirement_when_convertingChatToClaude() throws Exception {
        String body = """
                {
                  "model":"claude-sonnet-4-20250514",
                  "tools":[{"type":"function","function":{"name":"test","parameters":{"type":"object"}}}],
                  "messages":[{"role":"user","content":"hi"}]
                }
                """;

        // Should not throw even with toolCallingRequired=true
        JsonNode mapped = convertRequest(body, true);

        assertThat(mapped.at("/tools/0/name").asText()).isEqualTo("test");
    }

    private JsonNode convertRequest(String body, boolean toolCallingRequired) throws Exception {
        ProtocolMessageConverter converter = configuration.openAIChatToClaudeMessagesRequest(
                json, new SseEventTransformer());
        return objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, body, false),
                ProtocolConversionRequest.of(false, toolCallingRequired, false)
        ).body());
    }
}
