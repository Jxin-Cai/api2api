package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaudeResponsesToolsetBridgeTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(mapper);
    private final ProtocolConverterConfiguration configuration =
            new ProtocolConverterConfiguration(new ProtocolConversionProperties());

    @Test
    void test_preservesToolsetIdentity_when_namespacedCallIsReplayed() throws Exception {
        // Arrange
        JsonNode item = mapper.readTree("""
                {"type":"function_call","call_id":"call_1","name":"screenshot","namespace":"computer","arguments":"{}"}
                """);
        JsonNode block = convertResponse(response(item)).path("content").get(0);
        ObjectNode request = request();
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "assistant").putArray("content").add(block);
        messages.addObject().put("role", "user").putArray("content").addObject()
                .put("type", "tool_result").put("tool_use_id", "call_1").put("content", "screenshot result");
        // Act
        JsonNode result = convertRequest(request);
        // Assert
        assertThat(result.path("input")).allSatisfy(input ->
                assertThat(input.path("namespace").asText()).isEqualTo("computer"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"computer", "browser"})
    void test_emitsToolsetIdentity_when_namespacedCallStreams(String namespace) throws Exception {
        // Arrange
        ObjectNode item = mapper.createObjectNode().put("type", "function_call").put("call_id", "call_1")
                .put("name", "screenshot").put("namespace", namespace).put("arguments", "{}");
        ObjectNode added = mapper.createObjectNode().put("type", "response.output_item.added").put("output_index", 0);
        added.set("item", item);
        // Act
        List<JsonNode> events = stream(item, sse(added));
        // Assert
        assertThat(events).anySatisfy(event ->
                assertThat(event.at("/content_block/toolset_name").asText()).isEqualTo(namespace));
    }

    @Test
    void test_exposesComputerMembers_when_clientDeclaresComputerToolset() throws Exception {
        // Arrange
        ObjectNode source = request();
        source.putArray("tools").addObject().put("type", "computer_toolset_20260801");
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/0/tools")).hasSize(17);
    }

    @Test
    void test_withholdsDisabledComputerMember_when_executorDisablesZoom() throws Exception {
        // Arrange
        ObjectNode source = request();
        source.putArray("tools").addObject().put("type", "computer_toolset_20260801")
                .putObject("configs").putObject("zoom").put("enabled", false);
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/0/tools")).noneMatch(tool -> "zoom".equals(tool.path("name").asText()));
    }

    @Test
    void test_buffersMemberInput_when_responsesStreamsArgumentFragments() throws Exception {
        // Arrange
        ObjectNode item = mapper.createObjectNode().put("type", "function_call").put("call_id", "call_1")
                .put("namespace", "computer").put("name", "type").put("arguments", "{\"text\":\"hello\"}");
        ObjectNode added = mapper.createObjectNode().put("type", "response.output_item.added").put("output_index", 0);
        added.set("item", item);
        String prefix = sse(added);
        for (String fragment : List.of("{\"text\":", "\"hello\"}")) {
            prefix += sse(mapper.createObjectNode().put("type", "response.function_call_arguments.delta")
                    .put("output_index", 0).put("delta", fragment));
        }
        // Act
        List<JsonNode> events = stream(item, prefix);
        // Assert
        assertThat(events.stream().filter(event -> "input_json_delta".equals(event.at("/delta/type").asText()))
                .map(event -> event.at("/delta/partial_json").asText()).toList()).containsExactly("{\"text\":\"hello\"}");
    }

    @Test
    void test_exposesDefaultBrowserMembers_when_clientDeclaresBrowserToolset() throws Exception {
        // Arrange
        ObjectNode source = request();
        source.putArray("tools").addObject().put("type", "browser_toolset_20260801");
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/0/tools")).hasSize(27);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript_exec", "file_upload", "read_console", "read_network"})
    void test_withholdsOptionalBrowserMember_when_notExplicitlyEnabled(String member) throws Exception {
        // Arrange
        ObjectNode source = request();
        source.putArray("tools").addObject().put("type", "browser_toolset_20260801");
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/0/tools")).noneMatch(tool -> member.equals(tool.path("name").asText()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript_exec", "file_upload", "read_console", "read_network"})
    void test_exposesOptionalBrowserMember_when_explicitlyEnabled(String member) throws Exception {
        // Arrange
        ObjectNode source = request();
        source.putArray("tools").addObject().put("type", "browser_toolset_20260801")
                .putObject("configs").putObject(member).put("enabled", true);
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/0/tools")).anyMatch(tool -> member.equals(tool.path("name").asText()));
    }

    @Test
    void test_eagerlyLoadsNestedFunctions_when_modelLacksToolSearch() throws Exception {
        // Arrange
        ObjectNode source = deferredComputerRequest();
        source.put("model", "gpt-5.3-codex");
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/0/tools")).allSatisfy(tool -> assertThat(tool.has("defer_loading")).isFalse());
    }

    @Test
    void test_activatesWholeToolset_when_historyInvokesOneMember() throws Exception {
        // Arrange
        ObjectNode source = deferredComputerRequest();
        source.withArray("messages").addObject().put("role", "assistant").putArray("content").addObject()
                .put("type", "tool_use").put("name", "screenshot").put("toolset_name", "computer").put("id", "call_1")
                .set("input", mapper.createObjectNode());
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/1/tools")).allSatisfy(tool -> assertThat(tool.has("defer_loading")).isFalse());
    }

    @Test
    void test_preservesUnrelatedDeferredTool_when_memberHasSameName() throws Exception {
        // Arrange
        ObjectNode source = deferredComputerRequest();
        source.withArray("tools").addObject().put("name", "screenshot").put("defer_loading", true)
                .set("input_schema", mapper.readTree("{\"type\":\"object\",\"properties\":{}}"));
        source.withArray("messages").addObject().put("role", "assistant").putArray("content").addObject()
                .put("type", "tool_use").put("name", "screenshot").put("toolset_name", "computer").put("id", "call_1")
                .set("input", mapper.createObjectNode());
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        assertThat(result.at("/tools/2/defer_loading").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"configs\":{\"zoom\":{\"defer_loading\":true}}}",
            "{\"allowed_callers\":[\"code_execution_20260521\"]}", "{\"configs\":{\"unknown_action\":{}}}",
            "{\"configs\":{\"zoom\":{\"enabled\":\"false\"}}}"})
    void test_rejectsUnsupportedToolsetConfiguration_when_contractWouldChange(String config) throws Exception {
        // Arrange
        ObjectNode source = request();
        ObjectNode tool = (ObjectNode) mapper.readTree(config);
        tool.put("type", "computer_toolset_20260801");
        source.putArray("tools").add(tool);
        // Act / Assert
        assertThatThrownBy(() -> convertRequest(source)).isInstanceOf(com.api2api.domain.protocol.model.ProtocolConversionException.class);
    }

    private ObjectNode deferredComputerRequest() throws Exception {
        ObjectNode source = request();
        ObjectNode tool = source.putArray("tools").addObject().put("type", "computer_toolset_20260801");
        JsonNode mapped = convertRequest(source);
        ObjectNode configs = tool.putObject("configs");
        for (JsonNode member : mapped.at("/tools/0/tools")) {
            configs.putObject(member.path("name").asText()).put("defer_loading", true);
        }
        return source;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void test_preservesDocumentText_when_contentSourceIsString(boolean inToolResult) throws Exception {
        // Arrange
        ObjectNode source = request();
        ArrayNode content = source.putArray("messages").addObject().put("role", "user").putArray("content");
        if (inToolResult) {
            content = content.addObject().put("type", "tool_result").put("tool_use_id", "call_1").putArray("content");
        }
        content.addObject().put("type", "document").putObject("source").put("type", "content")
                .put("content", "The document body");
        // Act
        JsonNode result = convertRequest(source);
        // Assert
        String path = inToolResult ? "/input/0/output/0/text" : "/input/0/content/0/text";
        assertThat(result.at(path).asText()).isEqualTo("The document body");
    }

    @Test
    void test_keepsDistinctToolsetOperations_when_membersShareNameAndIntent() {
        // Arrange
        ArrayNode messages = screenshotHistory(List.of("computer", "browser", "computer"));
        // Act
        JsonNode protectedMessages = ClaudeConversationContextOptimizer.protectAgainstRepeatedToolCalls(messages);
        // Assert
        assertThat(protectedMessages).isSameAs(messages);
    }

    @Test
    void test_stillRejectsRepeatedOperation_when_sameToolsetAndInputRepeat() {
        // Arrange
        ArrayNode messages = screenshotHistory(List.of("browser", "browser", "browser"));
        // Act / Assert
        assertThatThrownBy(() -> ClaudeConversationContextOptimizer.protectAgainstRepeatedToolCalls(messages))
                .hasMessageContaining("CLAUDE_REPEATED_SUCCESSFUL_TOOL_CALL");
    }

    private ArrayNode screenshotHistory(List<String> toolsets) {
        ArrayNode messages = mapper.createArrayNode();
        for (int index = 0; index < toolsets.size(); index++) {
            ArrayNode content = messages.addObject().put("role", "assistant").putArray("content");
            content.addObject().put("type", "text").put("text", "Inspect the current application state before deciding what to do next.");
            content.addObject().put("type", "tool_use").put("id", "call_" + index)
                    .put("name", "screenshot").put("toolset_name", toolsets.get(index)).set("input", mapper.createObjectNode());
            messages.addObject().put("role", "user").putArray("content").addObject().put("type", "tool_result")
                    .put("tool_use_id", "call_" + index).put("content", "Screenshot returned");
        }
        return messages;
    }

    private ObjectNode request() {
        ObjectNode request = mapper.createObjectNode().put("model", "gpt-6-astra").put("max_tokens", 2048);
        request.putArray("messages").addObject().put("role", "user").put("content", "Use the provided tools");
        return request;
    }

    private ObjectNode response(JsonNode item) {
        ObjectNode result = mapper.createObjectNode().put("id", "resp_1").put("model", "gpt-6-astra").put("status", "completed");
        result.putArray("output").add(item);
        return result;
    }

    private List<JsonNode> stream(JsonNode item, String prefix) throws Exception {
        ObjectNode done = mapper.createObjectNode().put("type", "response.output_item.done").put("output_index", 0);
        done.set("item", item);
        ObjectNode completed = mapper.createObjectNode().put("type", "response.completed");
        completed.set("response", response(item));
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();
        new UnifiedStreamingConversionAdapter(mapper).transform(
                GatewayStreamingConversionContext.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("claude-client"), ProviderChannelId.of(1L), ModelName.of("gpt-6-astra")),
                new ByteArrayInputStream((prefix + sse(done) + sse(completed)).getBytes(StandardCharsets.UTF_8)), downstream);
        return dataEvents(downstream.toString(StandardCharsets.UTF_8));
    }

    private String deltaText(List<JsonNode> events, String field) {
        return events.stream().map(event -> event.path("delta").path(field).asText("")).reduce("", String::concat);
    }

    private JsonNode convertRequest(JsonNode source) throws Exception {
        return mapper.readTree(configuration.claudeMessagesToOpenAIResponsesRequest(json, new SseEventTransformer())
                .convert(ProtocolPayload.of(ProtocolType.CLAUDE_MESSAGES, source.toString(), false),
                        ProtocolConversionRequest.of(false, true, false)).body());
    }

    private JsonNode convertResponse(JsonNode source) throws Exception {
        return mapper.readTree(configuration.openAIResponsesToClaudeMessagesResponse(json, new OpenAIResponsesUsageExtractor(),
                        new SseEventTransformer())
                .convert(ProtocolPayload.of(ProtocolType.OPENAI_RESPONSES, source.toString(), false),
                        ProtocolConversionRequest.of(false, true, false)).body());
    }

    private String sse(JsonNode event) {
        return "event: " + event.path("type").asText() + "\ndata: " + event + "\n\n";
    }

    private List<JsonNode> dataEvents(String wire) throws Exception {
        List<JsonNode> events = new ArrayList<>();
        for (String line : wire.split("\n")) {
            if (line.startsWith("data: ")) {
                events.add(mapper.readTree(line.substring(6)));
            }
        }
        return events;
    }
}
