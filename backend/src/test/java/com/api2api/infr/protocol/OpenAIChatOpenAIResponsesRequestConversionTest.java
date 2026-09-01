package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAIChatOpenAIResponsesRequestConversionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
    private final ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration(new ProtocolConversionProperties());

    // ===== Chat → Responses =====

    @Test
    void test_mapsSystemMessageToSystemInputItem_when_convertingChatToResponses() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "messages":[
                    {"role":"system","content":"You are helpful."},
                    {"role":"user","content":"Hello"}
                  ]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.has("instructions")).isFalse();
        assertThat(mapped.at("/input/0/role").asText()).isEqualTo("system");
        assertThat(mapped.at("/input/0/content/0/type").asText()).isEqualTo("input_text");
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("You are helpful.");
    }

    @Test
    void test_prefersMaxCompletionTokens_when_bothTokenLimitsPresent() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "max_tokens":300,
                  "max_completion_tokens":500,
                  "messages":[{"role":"user","content":"hi"}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.path("max_output_tokens").asInt()).isEqualTo(500);
    }

    @Test
    void test_dropsSamplingParameters_when_chatTargetsReasoningModel() throws Exception {
        String body = """
                {
                  "model":"o3-mini",
                  "temperature":0.7,
                  "top_p":0.9,
                  "messages":[{"role":"user","content":"hi"}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.has("temperature")).isFalse();
        assertThat(mapped.has("top_p")).isFalse();
    }

    @Test
    void test_wrapsReasoningContentInThinkingTags_when_assistantHasReasoning() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "messages":[
                    {"role":"user","content":"why?"},
                    {"role":"assistant","content":"Because.","reasoning_content":"thinking about it"}
                  ]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.at("/input/1/content/0/type").asText()).isEqualTo("output_text");
        assertThat(mapped.at("/input/1/content/0/text").asText())
                .isEqualTo("<thinking>\nthinking about it\n</thinking>\n\nBecause.");
    }

    @Test
    void test_mapsToolCallsToFunctionCallItems_when_assistantCallsTool() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "messages":[
                    {"role":"user","content":"weather?"},
                    {"role":"assistant","content":null,"tool_calls":[{
                      "id":"call_1","type":"function",
                      "function":{"name":"get_weather","arguments":""}
                    }]}
                  ]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        JsonNode item = mapped.path("input").get(1);
        assertThat(item.path("type").asText()).isEqualTo("function_call");
        assertThat(item.path("call_id").asText()).isEqualTo("call_1");
        assertThat(item.path("name").asText()).isEqualTo("get_weather");
        assertThat(item.path("arguments").asText()).isEqualTo("{}");
    }

    @Test
    void test_mapsToolReplyToFunctionCallOutput_when_chatHistoryHasEmptyToolResult() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "messages":[
                    {"role":"tool","tool_call_id":"call_1","content":""}
                  ]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        JsonNode item = mapped.path("input").get(0);
        assertThat(item.path("type").asText()).isEqualTo("function_call_output");
        assertThat(item.path("call_id").asText()).isEqualTo("call_1");
        assertThat(item.path("output").asText()).isEqualTo("(empty)");
    }

    @Test
    void test_flattensToolDefinitions_when_convertingChatToResponses() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "tools":[{"type":"function","function":{
                    "name":"search","description":"Find things",
                    "parameters":{"type":"object","properties":{"q":{"type":"string"}}}
                  }}],
                  "messages":[{"role":"user","content":"find"}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        JsonNode tool = mapped.path("tools").get(0);
        assertThat(tool.path("type").asText()).isEqualTo("function");
        assertThat(tool.path("name").asText()).isEqualTo("search");
        assertThat(tool.path("description").asText()).isEqualTo("Find things");
        assertThat(tool.at("/parameters/type").asText()).isEqualTo("object");
        assertThat(tool.path("strict").asBoolean()).isFalse();
    }

    @Test
    void test_convertsLegacyFunctions_when_chatUsesDeprecatedFunctionApi() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "functions":[{"name":"lookup","parameters":{"type":"object"}}],
                  "function_call":{"name":"lookup"},
                  "messages":[
                    {"role":"assistant","content":null,"function_call":{"name":"lookup","arguments":"{\\"k\\":1}"}},
                    {"role":"function","name":"lookup","content":"result"}
                  ]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.at("/tools/0/name").asText()).isEqualTo("lookup");
        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/tool_choice/name").asText()).isEqualTo("lookup");
        assertThat(mapped.at("/input/0/type").asText()).isEqualTo("function_call");
        assertThat(mapped.at("/input/0/call_id").asText()).isEqualTo("lookup");
        assertThat(mapped.at("/input/1/type").asText()).isEqualTo("function_call_output");
        assertThat(mapped.at("/input/1/call_id").asText()).isEqualTo("lookup");
    }

    @Test
    void test_flattensSpecificToolChoice_when_convertingChatToResponses() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "tools":[{"type":"function","function":{"name":"search","parameters":{"type":"object"}}}],
                  "tool_choice":{"type":"function","function":{"name":"search"}},
                  "messages":[{"role":"user","content":"find"}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/tool_choice/name").asText()).isEqualTo("search");
    }

    @Test
    void test_mapsReasoningEffortToReasoningConfig_when_convertingChatToResponses() throws Exception {
        String body = """
                {
                  "model":"o3-mini",
                  "reasoning_effort":"high",
                  "messages":[{"role":"user","content":"hi"}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("high");
        assertThat(mapped.at("/reasoning/summary").asText()).isEqualTo("auto");
    }

    @Test
    void test_flattensJsonSchemaResponseFormat_when_convertingChatToResponses() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "response_format":{"type":"json_schema","json_schema":{
                    "name":"answer","strict":true,"schema":{"type":"object"}
                  }},
                  "messages":[{"role":"user","content":"hi"}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        JsonNode format = mapped.at("/text/format");
        assertThat(format.path("type").asText()).isEqualTo("json_schema");
        assertThat(format.path("name").asText()).isEqualTo("answer");
        assertThat(format.path("strict").asBoolean()).isTrue();
        assertThat(format.at("/schema/type").asText()).isEqualTo("object");
    }

    @Test
    void test_mapsImageContentPart_when_convertingChatToResponses() throws Exception {
        String body = """
                {
                  "model":"gpt-4.1",
                  "messages":[{"role":"user","content":[
                    {"type":"text","text":"Describe"},
                    {"type":"image_url","image_url":{"url":"https://example.com/cat.png","detail":"high"}}
                  ]}]
                }
                """;

        JsonNode mapped = chatToResponses(body);

        JsonNode parts = mapped.at("/input/0/content");
        assertThat(parts.get(0).path("type").asText()).isEqualTo("input_text");
        assertThat(parts.get(1).path("type").asText()).isEqualTo("input_image");
        assertThat(parts.get(1).path("image_url").asText()).isEqualTo("https://example.com/cat.png");
        assertThat(parts.get(1).path("detail").asText()).isEqualTo("high");
    }

    @Test
    void test_rejectsUnsupportedContentPart_when_convertingChatToResponses() {
        String body = """
                {
                  "model":"gpt-4.1",
                  "messages":[{"role":"user","content":[
                    {"type":"audio","audio":{"data":"base64data"}}
                  ]}]
                }
                """;

        assertThatThrownBy(() -> chatToResponses(body))
                .hasMessageContaining("OPENAI_CHAT_RESPONSES_CONTENT_PART_NOT_SUPPORTED: audio");
    }

    @Test
    void test_supportsStreamingToolCalling_when_chatRequestTargetsResponses() {
        ProtocolMessageConverter converter = configuration.openAIChatToOpenAIResponsesRequest(
                json, new SseEventTransformer());

        boolean supported = converter.supports(ProtocolConversionRequest.of(true, true, false));

        assertThat(supported).isTrue();
    }

    // ===== Responses → Chat =====

    @Test
    void test_mapsInstructionsToSystemMessage_when_convertingResponsesToChat() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "instructions":"Be concise.",
                  "input":"Hello"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.at("/messages/0/role").asText()).isEqualTo("system");
        assertThat(mapped.at("/messages/0/content").asText()).isEqualTo("Be concise.");
        assertThat(mapped.at("/messages/1/role").asText()).isEqualTo("user");
        assertThat(mapped.at("/messages/1/content").asText()).isEqualTo("Hello");
    }

    @Test
    void test_appliesMinimumCompletionTokens_when_maxOutputTokensBelowFloor() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "max_output_tokens":10,
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.path("max_completion_tokens").asInt()).isEqualTo(128);
    }

    @Test
    void test_addsStreamOptions_when_responsesRequestIsStreaming() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "stream":true,
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.at("/stream_options/include_usage").asBoolean()).isTrue();
    }

    @Test
    void test_attachesReasoningToToolCallingAssistant_when_reasoningItemPrecedesFunctionCall() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"message","role":"user","content":"weather?"},
                    {"type":"reasoning","summary":[{"type":"summary_text","text":"need the weather tool"}]},
                    {"type":"function_call","call_id":"call_1","name":"get_weather","arguments":"{\\"city\\":\\"Tokyo\\"}"},
                    {"type":"function_call_output","call_id":"call_1","output":"Sunny"}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        JsonNode assistant = mapped.path("messages").get(1);
        assertThat(assistant.path("role").asText()).isEqualTo("assistant");
        assertThat(assistant.path("reasoning_content").asText()).isEqualTo("need the weather tool");
        assertThat(assistant.at("/tool_calls/0/id").asText()).isEqualTo("call_1");
        assertThat(assistant.at("/tool_calls/0/function/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.path("messages").get(2).path("role").asText()).isEqualTo("tool");
    }

    @Test
    void test_mergesConsecutiveFunctionCalls_when_convertingResponsesToChat() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"function_call","call_id":"call_1","name":"a","arguments":"{}"},
                    {"type":"function_call","call_id":"call_2","name":"b","arguments":"{}"},
                    {"type":"function_call_output","call_id":"call_1","output":"ra"},
                    {"type":"function_call_output","call_id":"call_2","output":"rb"}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        JsonNode assistant = mapped.path("messages").get(0);
        assertThat(assistant.path("tool_calls").size()).isEqualTo(2);
        assertThat(mapped.path("messages").get(1).path("tool_call_id").asText()).isEqualTo("call_1");
        assertThat(mapped.path("messages").get(2).path("tool_call_id").asText()).isEqualTo("call_2");
    }

    @Test
    void test_dropsPoisonedToolCall_when_functionCallArgumentsAreTruncated() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"function_call","call_id":"call_bad","name":"a","arguments":"{\\"k\\":"},
                    {"type":"function_call_output","call_id":"call_bad","output":"ignored"},
                    {"type":"function_call","call_id":"call_ok","name":"b","arguments":"{}"},
                    {"type":"function_call_output","call_id":"call_ok","output":"rb"}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.toString()).doesNotContain("call_bad");
        assertThat(mapped.path("messages").get(0).at("/tool_calls/0/id").asText()).isEqualTo("call_ok");
    }

    @Test
    void test_dropsUnansweredToolCall_when_normalizingConvertedHistory() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"message","role":"user","content":"go"},
                    {"type":"function_call","call_id":"call_1","name":"a","arguments":"{}"}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.toString()).doesNotContain("tool_calls");
    }

    @Test
    void test_renestsFunctionToolAndDropsServerTool_when_convertingResponsesToChat() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "tools":[
                    {"type":"web_search"},
                    {"type":"function","name":"search","description":"Find","strict":true,
                     "parameters":{"type":"object"}}
                  ],
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.path("tools").size()).isEqualTo(1);
        JsonNode tool = mapped.path("tools").get(0);
        assertThat(tool.path("type").asText()).isEqualTo("function");
        assertThat(tool.at("/function/name").asText()).isEqualTo("search");
        assertThat(tool.at("/function/strict").asBoolean()).isTrue();
    }

    @Test
    void test_dropsToolChoice_when_targetFunctionToolWasDropped() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "tools":[{"type":"web_search"}],
                  "tool_choice":{"type":"function","name":"missing"},
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.has("tool_choice")).isFalse();
    }

    @Test
    void test_renestsSpecificToolChoice_when_targetFunctionToolSurvives() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "tools":[{"type":"function","name":"search","parameters":{"type":"object"}}],
                  "tool_choice":{"type":"function","name":"search"},
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.at("/tool_choice/type").asText()).isEqualTo("function");
        assertThat(mapped.at("/tool_choice/function/name").asText()).isEqualTo("search");
    }

    @Test
    void test_renestsJsonSchemaTextFormat_when_convertingResponsesToChat() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "text":{"format":{"type":"json_schema","name":"answer","strict":true,
                    "schema":{"type":"object"}}},
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        JsonNode responseFormat = mapped.path("response_format");
        assertThat(responseFormat.path("type").asText()).isEqualTo("json_schema");
        assertThat(responseFormat.at("/json_schema/name").asText()).isEqualTo("answer");
        assertThat(responseFormat.at("/json_schema/schema/type").asText()).isEqualTo("object");
        assertThat(responseFormat.at("/json_schema").has("type")).isFalse();
    }

    @Test
    void test_mapsReasoningEffortField_when_convertingResponsesToChat() throws Exception {
        String body = """
                {
                  "model":"o3-mini",
                  "reasoning":{"effort":"low"},
                  "input":"hi"
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.path("reasoning_effort").asText()).isEqualTo("low");
    }

    @Test
    void test_mapsDeveloperRoleToSystem_when_convertingResponsesToChat() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"message","role":"developer","content":[{"type":"input_text","text":"rule"}]},
                    {"type":"message","role":"user","content":"hi"}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.at("/messages/0/role").asText()).isEqualTo("system");
        assertThat(mapped.at("/messages/0/content").asText()).isEqualTo("rule");
    }

    @Test
    void test_keepsImagePart_when_responsesUserMessageContainsImage() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"message","role":"user","content":[
                      {"type":"input_text","text":"Describe"},
                      {"type":"input_image","image_url":"https://example.com/cat.png","detail":"low"}
                    ]}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        JsonNode parts = mapped.at("/messages/0/content");
        assertThat(parts.get(0).path("type").asText()).isEqualTo("text");
        assertThat(parts.get(1).path("type").asText()).isEqualTo("image_url");
        assertThat(parts.get(1).at("/image_url/url").asText()).isEqualTo("https://example.com/cat.png");
        assertThat(parts.get(1).at("/image_url/detail").asText()).isEqualTo("low");
    }

    @Test
    void test_skipsServerToolTrace_when_responsesHistoryContainsWebSearchCall() throws Exception {
        String body = """
                {
                  "model":"deepseek-v4",
                  "input":[
                    {"type":"message","role":"user","content":"search it"},
                    {"type":"web_search_call","id":"ws_1","status":"completed"},
                    {"type":"message","role":"assistant","content":[{"type":"output_text","text":"done"}]}
                  ]
                }
                """;

        JsonNode mapped = responsesToChat(body);

        assertThat(mapped.path("messages").size()).isEqualTo(2);
        assertThat(mapped.at("/messages/1/content").asText()).isEqualTo("done");
    }

    private JsonNode chatToResponses(String body) throws Exception {
        ProtocolMessageConverter converter = configuration.openAIChatToOpenAIResponsesRequest(
                json, new SseEventTransformer());
        return objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_CHAT_COMPLETIONS, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());
    }

    private JsonNode responsesToChat(String body) throws Exception {
        ProtocolMessageConverter converter = configuration.openAIResponsesToOpenAIChatRequest(
                json, new SseEventTransformer());
        return objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());
    }
}
