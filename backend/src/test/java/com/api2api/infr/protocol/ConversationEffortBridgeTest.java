package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConversationEffortBridgeTest {

    private static final String USER = "{\"role\":\"user\",\"content\":\"hello\"}";
    private static final String ASSISTANT = "{\"role\":\"assistant\",\"content\":\"done\"}";
    private static final String UPDATE = "{\"role\":\"system\",\"content\":[],\"output_config\":{\"effort\":\"low\"}}";
    private static final String TOOL_CALL = """
            {"role":"assistant","content":[{"type":"tool_use","id":"call_1","name":"read","input":{}}]}
            """;
    private static final String TOOL_RESULT = """
            {"role":"user","content":[{"type":"tool_result","tool_use_id":"call_1","content":"ok"}]}
            """;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void test_usesMessageEffort_when_topLevelReasoningIsAbsent() throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE, false);
        request.remove("output_config");

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.at("/reasoning/effort").asText()).isEqualTo("low");
    }

    @Test
    void test_preservesNativeConfigurationHistory_when_protocolDoesNotChange() throws Exception {
        // Arrange
        String body = responsesRequest(true).put("model", "gpt-6.1-sol").toString();
        ProtocolMessageConverter converter = new ProtocolConverterConfiguration(new ProtocolConversionProperties())
                .openAIResponsesRequest(new ProtocolJsonSupport(mapper));

        // Act
        String converted = converter.convert(ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, body, true),
                ProtocolConversionRequest.of(true, false, true)).body();

        // Assert
        assertThat(converted).isEqualTo(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "top_p"})
    void test_preservesSampling_when_legacyAdaptiveModelHasNoThinkingOverride(String parameter) throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(false).put("model", "claude-sonnet-4-6").put(parameter, 0.98);
        request.remove("reasoning");
        request.put("input", "hello");

        // Act
        JsonNode result = convert(request, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(result.path(parameter).asDouble()).isEqualTo(0.98);
    }

    @ParameterizedTest
    @ValueSource(strings = {"claude-opus-4-7", "claude-opus-4-8", "claude-opus-5-5", "claude-sonnet-5-5",
            "claude-haiku-5-5", "claude-fable-5-1", "claude-mythos-5-1"})
    void test_usesAdaptiveThinking_when_responsesTargetsModernClaude(String model) throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(false).put("model", model);

        // Act
        JsonNode result = convert(request, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(result.path("thinking")).isEqualTo(mapper.readTree("{\"type\":\"adaptive\"}"));
    }

    @Test
    void test_preservesXhigh_when_targetSupportsModernEffortLevels() throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(false).put("model", "claude-opus-5-5");
        ((ObjectNode) request.at("/input/0/reasoning")).put("effort", "xhigh");

        // Act
        JsonNode result = convert(request, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(result.at("/output_config/effort").asText()).isEqualTo("xhigh");
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "top_p"})
    void test_omitsSamplingOverride_when_targetUsesAdaptiveThinking(String parameter) throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(false).put("model", "claude-opus-5-5").put(parameter, 0.5);

        // Act
        JsonNode result = convert(request, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(result.has(parameter)).isFalse();
    }

    static Stream<Arguments> activationCases() {
        return Stream.of(
                Arguments.of(USER + "," + UPDATE, "low"),
                Arguments.of(UPDATE + "," + USER, "low"),
                Arguments.of(USER + "," + ASSISTANT + "," + UPDATE, "high"),
                Arguments.of(USER + "," + ASSISTANT + "," + UPDATE + "," + USER, "low"),
                Arguments.of(USER + "," + TOOL_CALL + "," + TOOL_RESULT + "," + UPDATE, "high"),
                Arguments.of(USER + "," + TOOL_CALL + "," + UPDATE + "," + TOOL_RESULT, "high"),
                Arguments.of(USER + "," + TOOL_CALL + "," + TOOL_RESULT + "," + UPDATE + "," + USER, "low"),
                Arguments.of(USER + "," + UPDATE + "," + TOOL_CALL + "," + TOOL_RESULT, "low"),
                Arguments.of(UPDATE + "," + TOOL_RESULT.replace("\"ok\"}]", "\"ok\"},{\"type\":\"text\",\"text\":\"continue\"}]"), "low"),
                Arguments.of(USER + "," + UPDATE + "," + UPDATE.replace("low", "medium"), "medium")
        );
    }

    @ParameterizedTest
    @MethodSource("activationCases")
    void test_resolvesCurrentEffort_when_claudeConfigurationCrossesTurnBoundaries(String history, String effort) throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(history, true);

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.at("/reasoning/effort").asText()).isEqualTo(effort);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void test_preservesPromptText_when_effortOnlySystemMessagesArePresent(boolean streaming) throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE + "," + UPDATE.replace("[]", "\"\""), streaming);

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.path("input")).isEqualTo(mapper.readTree("""
                [{"type":"message","role":"user","content":[{"type":"input_text","text":"hello"}]}]
                """));
    }

    @Test
    void test_preservesSystemText_when_effortUpdateAlsoCarriesInstructions() throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE.replace("[]", "\"Be concise\""), false);

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.at("/input/1/content/0/text").asText()).isEqualTo("Be concise");
    }

    @Test
    void test_preservesFormat_when_effectiveEffortOverridesTopLevelConfig() throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE, false);
        request.withObject("/output_config").set("format", mapper.readTree("""
                {"type":"json_schema","schema":{"type":"object","properties":{"ok":{"type":"boolean"}}}}
                """));

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.at("/text/format/schema")).isEqualTo(request.at("/output_config/format/schema"));
    }

    @Test
    void test_preservesCallerRequest_when_resolvingEffort() throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE, false);
        JsonNode before = request.deepCopy();

        // Act
        ConversationEffortBridge.resolveClaudeRequest(request);

        // Assert
        assertThat(request).isEqualTo(before);
    }

    @Test
    void test_mapsMaxToSupportedLevel_when_targetIsOlderResponsesModel() throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE.replace("low", "max"), false).put("model", "gpt-5.4");

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.at("/reasoning/effort").asText()).isEqualTo("xhigh");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "42", "{\"format\":{\"type\":\"json_schema\"}}", "{\"task_budget\":{}}",
            "{\"effort\":\"typo\"}", "{\"effort\":2}", "{\"effort\":{}}"})
    void test_rejectsUnsupportedMessageConfig_when_itCannotBeMapped(String config) throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + ",{\"role\":\"system\",\"content\":[],\"output_config\":" + config + "}", false);

        // Act / Assert
        assertThatThrownBy(() -> convert(request, ProtocolType.CLAUDE_MESSAGES))
                .hasMessageContaining("CLAUDE_RESPONSES_");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "assistant"})
    void test_rejectsMessageConfig_when_roleIsNotSystem(String role) throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + "," + UPDATE.replace("system", role), false);

        // Act / Assert
        assertThatThrownBy(() -> convert(request, ProtocolType.CLAUDE_MESSAGES))
                .hasMessageContaining("MESSAGE_OUTPUT_CONFIG_REQUIRES_SYSTEM_ROLE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"effort\":null}"})
    void test_keepsBaseEffort_when_messageConfigHasNoOverride(String config) throws Exception {
        // Arrange
        ObjectNode request = claudeRequest(USER + ",{\"role\":\"system\",\"content\":\"work\",\"output_config\":" + config + "}", false);

        // Act
        JsonNode result = convert(request, ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertThat(result.at("/reasoning/effort").asText()).isEqualTo("high");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void test_appliesLatestUpdate_when_responsesContainsConfigurationHistory(boolean streaming) throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(streaming);
        request.withArray("input").add(mapper.readTree("""
                {"type":"configuration_update","reasoning":{"effort":"low"}}
                """));
        request.withArray("input").addObject().put("role", "user").put("content", "continue");

        // Act
        JsonNode result = convert(request, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(result.at("/output_config/effort").asText()).isEqualTo("low");
    }

    @Test
    void test_preservesConversation_when_responsesConfigurationItemsAreConsumed() throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(false);

        // Act
        JsonNode result = convert(request, ProtocolType.OPENAI_RESPONSES);

        // Assert
        assertThat(result.path("messages")).isEqualTo(mapper.readTree("""
                [{"role":"user","content":[{"type":"text","text":"hello"}]}]
                """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"effort\":null}", "{\"effort\":\"typo\"}", "{\"effort\":\"high\",\"mode\":\"pro\"}"})
    void test_rejectsUnsupportedUpdate_when_responsesConfigurationIsInvalid(String reasoning) throws Exception {
        // Arrange
        ObjectNode request = responsesRequest(false);
        ((ObjectNode) request.at("/input/0")).set("reasoning", mapper.readTree(reasoning));

        // Act / Assert
        assertThatThrownBy(() -> convert(request, ProtocolType.OPENAI_RESPONSES))
                .hasMessageContaining("RESPONSES_CLAUDE_");
    }

    private ObjectNode claudeRequest(String history, boolean streaming) throws Exception {
        ObjectNode request = mapper.createObjectNode().put("model", "gpt-6.1-sol")
                .put("max_tokens", 1024).put("stream", streaming);
        request.putObject("output_config").put("effort", "high");
        request.set("messages", mapper.readTree("[" + history + "]"));
        return request;
    }

    private ObjectNode responsesRequest(boolean streaming) throws Exception {
        ObjectNode request = mapper.createObjectNode().put("model", "claude-sonnet-4-5")
                .put("stream", streaming).put("max_output_tokens", 4096);
        request.putObject("reasoning").put("effort", "low");
        request.set("input", mapper.readTree("""
                [{"type":"configuration_update","reasoning":{"effort":"high"}},
                 {"role":"user","content":"hello"}]
                """));
        return request;
    }

    private JsonNode convert(ObjectNode request, ProtocolType source) throws Exception {
        ProtocolConverterConfiguration configuration = new ProtocolConverterConfiguration(new ProtocolConversionProperties());
        ProtocolJsonSupport json = new ProtocolJsonSupport(mapper);
        ProtocolMessageConverter converter = source == ProtocolType.CLAUDE_MESSAGES
                ? configuration.claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer())
                : configuration.openAIResponsesToClaudeMessagesRequest(json, new SseEventTransformer());
        boolean streaming = request.path("stream").asBoolean();
        return mapper.readTree(converter.convert(ProtocolPayload.of(source, request.toString(), streaming),
                ProtocolConversionRequest.of(streaming, false, true)).body());
    }
}
