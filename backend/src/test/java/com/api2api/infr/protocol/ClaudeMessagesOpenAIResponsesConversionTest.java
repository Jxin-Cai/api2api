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

    @Test
    void shouldNotForceReasoningOrVerbosityWhenClaudeDidNotRequestThem() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration()
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[{"role":"user","content":"hello"}]}
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("reasoning").isMissingNode()).isTrue();
        assertThat(mapped.path("text").isMissingNode()).isTrue();
    }

    @Test
    void shouldMapLatestClaudeOutputToolsFilesAndMcpSchemasToResponses() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration()
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {
                  "model":"gpt-5.5",
                  "max_tokens":4096,
                  "thinking":{"type":"adaptive"},
                  "output_config":{"effort":"xhigh","format":{"type":"json_schema","schema":{"type":"object"}}},
                  "service_tier":"standard_only",
                  "container":"cntr_123",
                  "tools":[
                    {"name":"local_tool","input_schema":{"type":"object"},"strict":true},
                    {"type":"code_execution_20260120","name":"code_execution"},
                    {"type":"mcp_toolset","mcp_server_name":"docs","default_config":{"enabled":false},"configs":{"search":{"enabled":true}}}
                  ],
                  "mcp_servers":[{"type":"url","name":"docs","url":"https://mcp.example.com","authorization_token":"secret"}],
                  "messages":[{"role":"user","content":[
                    {"type":"image","source":{"type":"url","url":"https://example.com/a.png"}},
                    {"type":"document","title":"doc.pdf","source":{"type":"url","url":"https://example.com/a.pdf"}}
                  ]}]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/reasoning/effort").asText()).isEqualTo("xhigh");
        assertThat(mapped.at("/service_tier").asText()).isEqualTo("default");
        assertThat(mapped.at("/text/format/name").asText()).isEqualTo("json_response");
        assertThat(mapped.at("/tools/0/strict").asBoolean()).isTrue();
        assertThat(mapped.at("/tools/1/type").asText()).isEqualTo("code_interpreter");
        assertThat(mapped.at("/tools/1/container").asText()).isEqualTo("cntr_123");
        assertThat(mapped.at("/tools/2/type").asText()).isEqualTo("mcp");
        assertThat(mapped.at("/tools/2/server_url").asText()).isEqualTo("https://mcp.example.com");
        assertThat(mapped.at("/tools/2/authorization").asText()).isEqualTo("secret");
        assertThat(mapped.at("/tools/2/allowed_tools/0").asText()).isEqualTo("search");
        assertThat(mapped.at("/input/0/content/0/image_url").asText()).isEqualTo("https://example.com/a.png");
        assertThat(mapped.at("/input/0/content/1/file_url").asText()).isEqualTo("https://example.com/a.pdf");
    }

    @Test
    void shouldMapFastModeAndRejectUnknownClaudeFieldsInsteadOfSilentlyDroppingThem() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration()
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String fastBody = """
                {"model":"gpt-5.5","max_tokens":256,"speed":"fast","messages":[{"role":"user","content":"hi"}]}
                """;
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, fastBody, false),
                ProtocolConversionRequest.of(false, false, false)).body());
        assertThat(mapped.path("service_tier").asText()).isEqualTo("priority");

        String unknownBody = """
                {"model":"gpt-5.5","max_tokens":256,"future_feature":true,"messages":[]}
                """;
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> converter.convert(
                        ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, unknownBody, false),
                        ProtocolConversionRequest.of(false, false, false)))
                .hasMessageContaining("CLAUDE_RESPONSES_UNSUPPORTED_REQUEST_FIELD: future_feature");
    }

    @Test
    void shouldPreserveTextAndToolOrderingWithinAssistantContent() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration()
                .claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer());
        String body = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[{"role":"assistant","content":[
                  {"type":"text","text":"before"},
                  {"type":"tool_use","id":"call_1","name":"run","input":{}},
                  {"type":"text","text":"after"}
                ]}]}
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/input/0/content/0/text").asText()).isEqualTo("before");
        assertThat(mapped.at("/input/1/type").asText()).isEqualTo("function_call");
        assertThat(mapped.at("/input/2/content/0/text").asText()).isEqualTo("after");
    }

    @Test
    void shouldRoundTripEncryptedResponsesReasoningThroughClaudeThinkingSignature() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration();
        ProtocolMessageConverter responseConverter = configuration.openAIResponsesToClaudeMessagesResponse(
                json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.5","status":"completed","output":[
                  {"type":"reasoning","id":"rs_1","summary":[{"type":"summary_text","text":"summary"}],"encrypted_content":"encrypted"},
                  {"type":"message","role":"assistant","content":[{"type":"output_text","text":"done"}]}
                ],"usage":{"input_tokens":1,"output_tokens":2}}
                """;
        JsonNode claude = objectMapper.readTree(responseConverter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, responseBody, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        String signature = claude.at("/content/0/signature").asText();
        assertThat(signature).startsWith(ResponsesReasoningBridge.SIGNATURE_PREFIX);

        ProtocolMessageConverter requestConverter = configuration.claudeMessagesToOpenAIResponsesRequest(
                json, new SseEventTransformer());
        String requestBody = """
                {"model":"gpt-5.5","max_tokens":256,"thinking":{"type":"adaptive"},"messages":[
                  {"role":"assistant","content":%s}
                ]}
                """.formatted(objectMapper.writeValueAsString(claude.path("content")));
        JsonNode roundTripped = objectMapper.readTree(requestConverter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, requestBody, false),
                ProtocolConversionRequest.of(false, false, true)).body());

        assertThat(roundTripped.at("/input/0/type").asText()).isEqualTo("reasoning");
        assertThat(roundTripped.at("/input/0/id").asText()).isEqualTo("rs_1");
        assertThat(roundTripped.at("/input/0/encrypted_content").asText()).isEqualTo("encrypted");
        assertThat(roundTripped.at("/input/0/summary/0/text").asText()).isEqualTo("summary");
    }

    @Test
    void shouldRoundTripProviderHostedToolStateThroughOpaqueClaudeSignature() throws Exception {
        ProtocolJsonSupport json = new ProtocolJsonSupport(objectMapper);
        ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration();
        ProtocolMessageConverter responseConverter = configuration.openAIResponsesToClaudeMessagesResponse(
                json, new OpenAIResponsesUsageExtractor(), new SseEventTransformer());
        String responseBody = """
                {"id":"resp_1","model":"gpt-5.5","status":"completed","output":[
                  {"type":"web_search_call","id":"ws_1","status":"completed","action":{"type":"search","query":"docs"}},
                  {"type":"message","role":"assistant","content":[{"type":"output_text","text":"done"}]}
                ],"usage":{"input_tokens":1,"output_tokens":2}}
                """;
        JsonNode claude = objectMapper.readTree(responseConverter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, responseBody, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        String signature = claude.at("/content/0/signature").asText();
        assertThat(signature).startsWith(ResponsesReasoningBridge.ITEM_SIGNATURE_PREFIX);
        assertThat(claude.at("/content/1/text").asText()).isEqualTo("done");

        ProtocolMessageConverter requestConverter = configuration.claudeMessagesToOpenAIResponsesRequest(
                json, new SseEventTransformer());
        String requestBody = """
                {"model":"gpt-5.5","max_tokens":256,"messages":[
                  {"role":"assistant","content":%s}
                ]}
                """.formatted(objectMapper.writeValueAsString(claude.path("content")));
        JsonNode roundTripped = objectMapper.readTree(requestConverter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, requestBody, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        assertThat(roundTripped.at("/input/0/type").asText()).isEqualTo("web_search_call");
        assertThat(roundTripped.at("/input/0/id").asText()).isEqualTo("ws_1");
        assertThat(roundTripped.at("/input/0/action/query").asText()).isEqualTo("docs");
    }
}
