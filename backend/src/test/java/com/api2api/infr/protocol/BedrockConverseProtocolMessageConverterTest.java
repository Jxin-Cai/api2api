package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionRouteContext;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
                  "system":[{"type":"text","text":"You are Claude Code.","cache_control":{"type":"ephemeral","ttl":"1h"}}],
                  "thinking":{"type":"enabled","budget_tokens":1024},
                  "tools":[{"name":"get_weather","description":"weather","input_schema":{"type":"object"},"strict":true,"cache_control":{"type":"ephemeral"}}],
                  "tool_choice":{"type":"tool","name":"get_weather"},
                  "messages":[
                    {"role":"user","content":[{"type":"text","text":"hello","cache_control":{"type":"ephemeral"}}]},
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
        assertThat(mapped.at("/system/0/text").asText()).isEqualTo("You are Claude Code.");
        assertThat(mapped.at("/system/1/cachePoint/type").asText()).isEqualTo("default");
        assertThat(mapped.at("/system/1/cachePoint/ttl").asText()).isEqualTo("1h");
        assertThat(mapped.at("/inferenceConfig/maxTokens").asInt()).isEqualTo(64);
        assertThat(mapped.at("/messages/0/content/1/cachePoint/type").asText()).isEqualTo("default");
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/inputSchema/json/type").asText()).isEqualTo("object");
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/strict").asBoolean()).isTrue();
        assertThat(mapped.at("/toolConfig/tools/1/cachePoint/type").asText()).isEqualTo("default");
        assertThat(mapped.at("/toolConfig/toolChoice/tool/name").asText()).isEqualTo("get_weather");
        assertThat(mapped.at("/additionalModelRequestFields/thinking/type").asText()).isEqualTo("enabled");
        assertThat(mapped.at("/messages/1/content/0/toolUse/toolUseId").asText()).isEqualTo("toolu_1");
        assertThat(mapped.at("/messages/2/content/0/toolResult/toolUseId").asText()).isEqualTo("toolu_1");
        assertThat(mapped.at("/messages/2/content/0/toolResult/content/0/text").asText()).isEqualTo("sunny");
    }

    @Test
    void test_marksToolResultSuccessful_when_claudeOmitsIsError() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":64,
                 "messages":[
                   {"role":"assistant","content":[{"type":"tool_use","id":"question_1","name":"AskUserQuestion","input":{}}]},
                   {"role":"user","content":[{"type":"tool_result","tool_use_id":"question_1","content":"仅位置移动"}]}
                 ]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());

        // Assert
        assertThat(mapped.at("/messages/1/content/0/toolResult/status").asText()).isEqualTo("success");
    }

    @Test
    void test_hidesAskUserQuestionForNextTurn_when_answerWasSuccessful() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":64,
                 "tools":[
                   {"name":"AskUserQuestion","input_schema":{"type":"object"}},
                   {"name":"ExitPlanMode","input_schema":{"type":"object"}}
                 ],
                 "messages":[
                   {"role":"assistant","content":[{"type":"tool_use","id":"question_1","name":"AskUserQuestion","input":{}}]},
                   {"role":"user","content":[{"type":"tool_result","tool_use_id":"question_1","content":"仅位置移动"}]}
                 ]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());

        // Assert
        assertThat(mapped.at("/toolConfig/tools").findValuesAsText("name"))
                .containsExactly("ExitPlanMode");
    }

    @Test
    void test_hidesExitPlanMode_when_planWasAlreadyApproved() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":64,
                 "tools":[
                   {"name":"AskUserQuestion","input_schema":{"type":"object"}},
                   {"name":"EnterPlanMode","input_schema":{"type":"object"}},
                   {"name":"ExitPlanMode","input_schema":{"type":"object"}}
                 ],
                 "messages":[
                   {"role":"assistant","content":[{"type":"tool_use","id":"exit_1","name":"ExitPlanMode","input":{}}]},
                   {"role":"user","content":[{"type":"tool_result","tool_use_id":"exit_1","content":"User has approved your plan."}]}
                 ]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());

        // Assert
        assertThat(mapped.at("/toolConfig/tools").findValuesAsText("name"))
                .containsExactlyInAnyOrder("AskUserQuestion", "EnterPlanMode");
    }

    @Test
    void test_exposesExitPlanMode_when_planWasEnteredAgain() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":64,
                 "tools":[
                   {"name":"EnterPlanMode","input_schema":{"type":"object"}},
                   {"name":"ExitPlanMode","input_schema":{"type":"object"}}
                 ],
                 "messages":[
                   {"role":"assistant","content":[{"type":"tool_use","id":"exit_1","name":"ExitPlanMode","input":{}}]},
                   {"role":"user","content":[{"type":"tool_result","tool_use_id":"exit_1","content":"User approved."}]},
                   {"role":"assistant","content":[{"type":"tool_use","id":"enter_2","name":"EnterPlanMode","input":{}}]},
                   {"role":"user","content":[{"type":"tool_result","tool_use_id":"enter_2","content":"Entered plan mode."}]}
                 ]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());

        // Assert
        assertThat(mapped.at("/toolConfig/tools").findValuesAsText("name"))
                .containsExactly("ExitPlanMode");
    }

    @Test
    void shouldNotInventBedrockMaxTokensWhenClaudeThinkingHasNoMaxTokens() throws Exception {
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
                  "thinking":{"type":"enabled","budget_tokens":4096},
                  "messages":[{"role":"user","content":"work on this task"}]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/inferenceConfig/maxTokens").isMissingNode()).isTrue();
        assertThat(mapped.at("/additionalModelRequestFields/thinking/budget_tokens").asInt()).isEqualTo(4096);
    }

    @Test
    void shouldAcceptClaudeCodeContextManagementHintWithoutSendingUnsupportedConverseField() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4-6",
                  "stream":true,
                  "max_tokens":8192,
                  "thinking":{"type":"adaptive"},
                  "context_management":{"edits":[{"type":"clear_thinking_20251015","keep":"all"}]},
                  "tools":[{"name":"Write","description":"Write a file","input_schema":{"type":"object","properties":{"file_path":{"type":"string"},"content":{"type":"string"}},"required":["file_path","content"]}}],
                  "messages":[{"role":"user","content":"Write the implementation plan to the plan file"}]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, true),
                ProtocolConversionRequest.of(true, true, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.has("context_management")).isFalse();
        assertThat(mapped.at("/additionalModelRequestFields/context_management").isMissingNode()).isTrue();
        assertThat(mapped.at("/additionalModelRequestFields/thinking/type").asText()).isEqualTo("adaptive");
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/name").asText()).isEqualTo("Write");
        assertThat(mapped.at("/messages/0/content/0/text").asText())
                .isEqualTo("Write the implementation plan to the plan file");
    }

    @Test
    void test_mapsClearedToolResultPlaceholder_when_bedrockCannotApplyContextEditing() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":1024,
                  "context_management":{"edits":[{
                    "type":"clear_tool_uses_20250919",
                    "trigger":{"type":"tool_uses","value":1},
                    "keep":{"type":"tool_uses","value":1}
                  }]},
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"Read","input":{"path":"old"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":"old result"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"Read","input":{"path":"current"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":"current result"}]}
                  ]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        ).body());

        // Assert
        assertThat(mapped.at("/messages/1/content/0/toolResult/content/0/text").asText())
                .isEqualTo("[Tool result cleared by context management]");
        assertThat(mapped.at("/messages/3/content/0/toolResult/content/0/text").asText())
                .isEqualTo("current result");
    }

    @Test
    void test_rejectsRequestBeforeFourthExecution_when_successfulToolCallRepeatedThreeTimes() {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":1024,
                  "messages":[
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-1","name":"Write","input":{"path":"same"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-1","content":"success"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-2","name":"Write","input":{"path":"same"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-2","content":"success"}]},
                    {"role":"assistant","content":[{"type":"tool_use","id":"call-3","name":"Write","input":{"path":"same"}}]},
                    {"role":"user","content":[{"type":"tool_result","tool_use_id":"call-3","content":"success"}]}
                  ]
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        )).hasMessageContaining("CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL");
    }

    @Test
    void test_placesTopLevelCachePointOnStableSystemPrefix_when_systemExists() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":1024,
                  "cache_control":{"type":"ephemeral"},
                  "system":"stable instructions",
                  "messages":[{"role":"user","content":"dynamic turn"}]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        ).body());

        // Assert
        assertThat(mapped.at("/system/1/cachePoint/type").asText()).isEqualTo("default");
        assertThat(mapped.at("/messages/0/content/1/cachePoint").isMissingNode()).isTrue();
    }

    @Test
    void test_skipsTopLevelCachePoint_when_fourExplicitCheckpointsAlreadyExist() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":1024,
                  "cache_control":{"type":"ephemeral"},
                  "system":[
                    {"type":"text","text":"one","cache_control":{"type":"ephemeral"}},
                    {"type":"text","text":"two","cache_control":{"type":"ephemeral"}},
                    {"type":"text","text":"three","cache_control":{"type":"ephemeral"}},
                    {"type":"text","text":"four","cache_control":{"type":"ephemeral"}}
                  ],
                  "messages":[{"role":"user","content":"dynamic turn"}]
                }
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        ).body());

        // Assert
        assertThat(mapped.at("/system").findValues("cachePoint")).hasSize(4);
    }

    @Test
    void shouldDropClaudeMetadataValuesThatBedrockRequestMetadataRejects() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "max_tokens":64,
                  "metadata":{"user_id":"user-1","session":"session{}"},
                  "messages":[{"role":"user","content":"hello"}]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/requestMetadata/user_id").asText()).isEqualTo("user-1");
        assertThat(mapped.at("/requestMetadata/session").isMissingNode()).isTrue();
    }

    @Test
    void test_acceptsCompactContextManagement_when_clientSideCompactionEnabled() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4-6","max_tokens":8192,
                 "context_management":{"edits":[{"type":"compact_20260112","trigger":{"value":80000}}]},
                 "messages":[{"role":"user","content":"continue"}]}
                """;

        var result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );
        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("messages")).isNotEmpty();
    }

    @Test
    void test_rejectsUnsupportedContextManagement_when_typeIsNotCompactOrClearThinking() {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4-6","max_tokens":8192,
                 "context_management":{"edits":[{"type":"clear_tool_uses"}]},
                 "messages":[{"role":"user","content":"continue"}]}
                """;

        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        )).hasMessageContaining("CLAUDE_BEDROCK_CONTEXT_MANAGEMENT_NOT_SUPPORTED_BY_CONVERSE: clear_tool_uses");
    }

    @Test
    void test_expandsDeferredTools_when_toolSearchIsUnavailableInConverse() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"anthropic.claude-opus-4-8","max_tokens":1024,"tools":[
                  {"type":"tool_search_tool_regex_20251119","name":"tool_search_tool_regex"},
                  {"name":"Read","description":"Read a file","input_schema":{"type":"object"},
                   "input_examples":[{"path":"README.md"}],"defer_loading":true,
                   "eager_input_streaming":true,"allowed_callers":["direct"]}
                ],"messages":[{"role":"user","content":"inspect"}]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)).body());

        // Assert
        assertThat(mapped.path("toolConfig").path("tools")).hasSize(1);
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/name").asText()).isEqualTo("Read");
        assertThat(mapped.at("/toolConfig/tools/0/toolSpec/description").asText()).contains("README.md");
    }

    @Test
    void test_rejectsProgrammaticCaller_when_converseCannotPreserveCallerRestriction() {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"anthropic.claude-opus-4-8","max_tokens":1024,"tools":[
                  {"name":"query_database","input_schema":{"type":"object"},
                   "allowed_callers":["code_execution_20260120"]}
                ],"messages":[{"role":"user","content":"inspect"}]}
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, false)
        )).hasMessageContaining("CLAUDE_BEDROCK_PROGRAMMATIC_TOOL_CALLING_NOT_SUPPORTED_BY_CONVERSE");
    }

    @Test
    void shouldMapAdaptiveEffortStructuredOutputMultimodalContentAndMidConversationSystem() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4-8",
                  "max_tokens":8192,
                  "thinking":{"type":"adaptive"},
                  "output_config":{
                    "effort":"xhigh",
                    "format":{"type":"json_schema","name":"result","schema":{"type":"object","properties":{"ok":{"type":"boolean"}},"required":["ok"],"additionalProperties":false}}
                  },
                  "service_tier":"standard_only",
                  "speed":"fast",
                  "metadata":{"user_id":"user-1"},
                  "messages":[
                    {"role":"user","content":[
                      {"type":"text","text":"inspect"},
                      {"type":"image","source":{"type":"base64","media_type":"image/png","data":"aW1hZ2U="}},
                      {"type":"document","title":"notes.md","source":{"type":"text","media_type":"text/plain","data":"hello"}},
                      {"type":"search_result","source":"https://example.com","title":"Example","content":[{"type":"text","text":"result"}]}
                    ]},
                    {"role":"user","content":[{"type":"mid_conv_system","content":[{"type":"text","text":"Use the new constraint"}]}]},
                    {"role":"assistant","content":"ack"}
                  ]
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/additionalModelRequestFields/thinking/type").asText()).isEqualTo("adaptive");
        assertThat(mapped.at("/additionalModelRequestFields/output_config/effort").asText()).isEqualTo("xhigh");
        assertThat(mapped.at("/outputConfig/textFormat/type").asText()).isEqualTo("json_schema");
        assertThat(mapped.at("/outputConfig/textFormat/structure/jsonSchema/name").asText()).isEqualTo("result");
        assertThat(mapped.at("/messages/0/content/1/image/format").asText()).isEqualTo("png");
        assertThat(mapped.at("/messages/0/content/2/document/format").asText()).isEqualTo("txt");
        assertThat(mapped.at("/messages/0/content/3/searchResult/source").asText()).isEqualTo("https://example.com");
        assertThat(mapped.at("/messages/1/role").asText()).isEqualTo("system");
        assertThat(mapped.at("/messages/1/content/0/text").asText()).isEqualTo("Use the new constraint");
        assertThat(mapped.at("/messages/2/role").asText()).isEqualTo("assistant");
        assertThat(mapped.at("/serviceTier/type").asText()).isEqualTo("default");
        assertThat(mapped.at("/performanceConfig/latency").asText()).isEqualTo("optimized");
        assertThat(mapped.at("/requestMetadata/user_id").asText()).isEqualTo("user-1");
        assertThat(mapped.at("/additionalModelResponseFieldPaths/0").asText()).isEqualTo("/stop_sequence");
    }

    @Test
    void shouldMergeConsecutiveRolesWithoutFabricatingMessages() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4-8","max_tokens":100,
                 "messages":[{"role":"user","content":"one"},{"role":"user","content":"two"}]}
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, false)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.path("messages")).hasSize(1);
        assertThat(mapped.at("/messages/0/content/0/text").asText()).isEqualTo("one");
        assertThat(mapped.at("/messages/0/content/1/text").asText()).isEqualTo("two");
    }

    @Test
    void shouldMapOpenAIResponsesReasoningToBedrockThinking() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json,
                null,
                ProtocolType.OPENAI_RESPONSES,
                ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST,
                sseEventTransformer
        );
        String body = """
                {
                  "model":"claude-opus-4.6",
                  "stream":true,
                  "input":[{"type":"message","role":"user","content":"Hello"}],
                  "reasoning":{"effort":"high","summary":"detailed"},
                  "tools":[],
                  "instructions":null
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, true),
                ProtocolConversionRequest.of(true, false, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/additionalModelRequestFields/thinking/type").asText()).isEqualTo("enabled");
        assertThat(mapped.at("/additionalModelRequestFields/thinking/budget_tokens").asInt()).isEqualTo(4096);
        assertThat(mapped.at("/inferenceConfig/maxTokens").asInt()).isEqualTo(5120);
        assertThat(mapped.at("/messages/0/content/0/text").asText()).isEqualTo("Hello");
    }

    @Test
    void test_omitsResponsesThinkingState_when_replayingHistoryToBedrock() throws Exception {
        // Arrange
        ObjectNode reasoningItem = objectMapper.createObjectNode();
        reasoningItem.put("type", "reasoning");
        reasoningItem.put("id", "rs_1");
        reasoningItem.put("encrypted_content", "encrypted");
        String signature = ResponsesReasoningBridge.encode(objectMapper, reasoningItem).orElseThrow();
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":1024,"messages":[
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"summary","signature":"%s"},
                    {"type":"text","text":"visible answer"}
                  ]},
                  {"role":"user","content":"continue"}
                ]}
                """.formatted(signature);

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)
        ).body());

        // Assert
        assertThat(mapped.at("/messages/0/content")).hasSize(1);
        assertThat(mapped.at("/messages/0/content/0/text").asText()).isEqualTo("visible answer");
    }

    @Test
    void test_omitsForeignThinkingSignature_when_claudeHistoryTargetsBedrock() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":1024,"messages":[
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"summary","signature":"native-claude-signature"},
                    {"type":"text","text":"visible answer"}
                  ]},
                  {"role":"user","content":"continue"}
                ]}
                """;

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)
        ).body());

        // Assert
        assertThat(mapped.at("/messages/0/content/0/text").asText()).isEqualTo("visible answer");
    }

    @Test
    void test_restoresBedrockThinkingSignature_when_wrappedStateReturns() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":1024,"messages":[
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"think","signature":"%s"},
                    {"type":"tool_use","id":"toolu_1","name":"Read","input":{}}
                  ]}
                ]}
                """.formatted(BedrockReasoningBridge.encode(
                        "bedrock-signature", new ProtocolConversionRouteContext(1L, "bedrock-model")));

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, true, true).forRoute(1L, "bedrock-model")
        ).body());

        // Assert
        assertThat(mapped.at("/messages/0/content/0/reasoningContent/reasoningText/signature").asText())
                .isEqualTo("bedrock-signature");
    }

    @Test
    void test_omitsBedrockThinkingSignature_when_routeModelChanges() throws Exception {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String previousSignature = BedrockReasoningBridge.encode(
                "bedrock-signature", new ProtocolConversionRouteContext(1L, "old-bedrock-model"));
        String body = """
                {"model":"claude-opus-4.6","max_tokens":1024,"messages":[
                  {"role":"assistant","content":[
                    {"type":"thinking","thinking":"think","signature":"%s"},
                    {"type":"text","text":"visible answer"}
                  ]}
                ]}
                """.formatted(previousSignature);

        // Act
        JsonNode mapped = objectMapper.readTree(converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true).forRoute(2L, "new-bedrock-model")
        ).body());

        // Assert
        assertThat(mapped.at("/messages/0/content/0/text").asText()).isEqualTo("visible answer");
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
                ProtocolConversionRequest.of(false, true, true).forRoute(1L, "bedrock-model")
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("thinking");
        assertThat(mapped.at("/content/0/thinking").asText()).isEqualTo("think");
        assertThat(BedrockReasoningBridge.decode(
                mapped.at("/content/0/signature").asText(),
                new ProtocolConversionRouteContext(1L, "bedrock-model")))
                .contains("sig");
        assertThat(mapped.at("/content/1/type").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/content/1/id").asText()).isEqualTo("toolu_1");
        assertThat(mapped.at("/stop_reason").asText()).isEqualTo("tool_use");
        assertThat(mapped.at("/usage/input_tokens").asLong()).isEqualTo(10);
        assertThat(result.usage()).isPresent();
        assertThat(result.usage().orElseThrow().totalTokens()).isEqualTo(18);
    }

    @Test
    void shouldMapBedrockRedactedReasoningAndExactStopSequenceToClaude() throws Exception {
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, new BedrockConverseUsageExtractor(), ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE, sseEventTransformer
        );
        String body = """
                {
                  "output":{"message":{"content":[
                    {"reasoningContent":{"redactedContent":"cmVkYWN0ZWQ="}},
                    {"text":"done"}
                  ]}},
                  "stopReason":"stop_sequence",
                  "additionalModelResponseFields":{"stop_sequence":"<END>"},
                  "usage":{"inputTokens":1,"outputTokens":2}
                }
                """;

        ProtocolConversionResult result = converter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CONVERSE, body, false),
                ProtocolConversionRequest.of(false, false, true)
        );

        JsonNode mapped = objectMapper.readTree(result.body());
        assertThat(mapped.at("/content/0/type").asText()).isEqualTo("redacted_thinking");
        assertThat(mapped.at("/content/0/data").asText()).isEqualTo("cmVkYWN0ZWQ=");
        assertThat(mapped.path("stop_reason").asText()).isEqualTo("stop_sequence");
        assertThat(mapped.path("stop_sequence").asText()).isEqualTo("<END>");
    }

    @Test
    void test_rejectsCacheOnlyRequest_when_converseCannotWarmClaudePromptCache() {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, null, ProtocolType.CLAUDE_MESSAGES, ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolConversionDirection.REQUEST, sseEventTransformer
        );
        String body = """
                {"model":"claude-opus-4.6","max_tokens":0,
                 "messages":[{"role":"user","content":"warm cache"}]}
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, body, false),
                ProtocolConversionRequest.of(false, false, true)))
                .hasMessageContaining("CLAUDE_BEDROCK_CACHE_ONLY_REQUEST_NOT_SUPPORTED_BY_CONVERSE");
    }

    @Test
    void test_throwsConversionException_when_bedrockToolUseIsMalformed() {
        // Arrange
        BedrockConverseProtocolMessageConverter converter = new BedrockConverseProtocolMessageConverter(
                json, new BedrockConverseUsageExtractor(), ProtocolType.AWS_BEDROCK_CONVERSE,
                ProtocolType.CLAUDE_MESSAGES, ProtocolConversionDirection.RESPONSE, sseEventTransformer
        );
        String body = """
                {
                  "output":{"message":{"content":[]}},
                  "stopReason":"malformed_tool_use",
                  "usage":{"inputTokens":1,"outputTokens":1}
                }
                """;

        // Act / Assert
        assertThatThrownBy(() -> converter.convert(
                ProtocolPayload.of(ProtocolType.AWS_BEDROCK_CONVERSE, body, false),
                ProtocolConversionRequest.of(false, true, false)
        )).hasMessageContaining("BEDROCK_CONVERSE_INVALID_MODEL_OUTPUT: malformed_tool_use");
    }
}
