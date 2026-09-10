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

class ClaudeMessagesResponsesToolCompatibilityTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ProtocolJsonSupport json = new ProtocolJsonSupport(mapper);
    private final ProtocolConverterConfiguration configuration =
            new ProtocolConverterConfiguration(new ProtocolConversionProperties());

    @ParameterizedTest
    @CsvSource({
            "bash_20241022,bash", "bash_20250124,bash",
            "text_editor_20241022,str_replace_editor", "text_editor_20250124,str_replace_editor",
            "text_editor_20250429,str_replace_based_edit_tool", "text_editor_20250728,str_replace_based_edit_tool",
            "memory_20250818,memory"
    })
    void test_exposesExecutableClientTool_when_anthropicSuppliesSchemaImplicitly(String type, String name) throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-6-astra", """
                [{"type":"%s","name":"%s"}]
                """.formatted(type, name));

        // Act
        JsonNode tool = convertRequest(request).at("/tools/0");

        // Assert: the same client executor is exposed with a real input contract.
        assertThat(tool.path("type").asText()).isEqualTo("function");
        assertThat(tool.path("name").asText()).isEqualTo(name);
        assertThat(tool.at("/parameters/properties/command/type").asText()).isEqualTo("string");
    }

    @Test
    void test_keepsRestartCallableWithoutCommand_when_bashSessionNeedsReset() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.5", """
                [{"type":"bash_20250124","name":"bash"}]
                """);

        // Act
        JsonNode parameters = convertRequest(request).at("/tools/0/parameters");

        // Assert
        assertThat(parameters.path("required")).isEmpty();
        assertThat(parameters.at("/properties/restart/type").asText()).isEqualTo("boolean");
    }

    @ParameterizedTest
    @CsvSource({"text_editor_20250124,str_replace_editor,true", "text_editor_20250728,str_replace_based_edit_tool,false"})
    void test_matchesUndoSupport_when_editorVersionsDiffer(String type, String name, boolean undo) throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.5", """
                [{"type":"%s","name":"%s"}]
                """.formatted(type, name));

        // Act
        JsonNode commands = convertRequest(request).at("/tools/0/parameters/properties/command/enum");

        // Assert
        assertThat(commands.toString().contains("undo_edit")).isEqualTo(undo);
    }

    @Test
    void test_keepsRenameContract_when_memoryUsesOldAndNewPaths() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.5", """
                [{"type":"memory_20250818","name":"memory"}]
                """);

        // Act
        JsonNode parameters = convertRequest(request).at("/tools/0/parameters");

        // Assert
        assertThat(parameters.path("required").toString()).isEqualTo("[\"command\"]");
        assertThat(parameters.at("/properties/old_path/type").asText()).isEqualTo("string");
        assertThat(parameters.at("/properties/new_path/type").asText()).isEqualTo("string");
    }

    @Test
    void test_preservesViewLimitInContract_when_editorHasMaxCharacters() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.5", """
                [{"type":"text_editor_20250728","name":"str_replace_based_edit_tool","max_characters":8000}]
                """);

        // Act
        JsonNode tool = convertRequest(request).at("/tools/0");

        // Assert
        assertThat(tool.path("description").asText()).contains("8000 characters", "view_range");
    }

    @Test
    void test_preservesOptionalNativeInputs_when_strictModeCannotRepresentCommandVariants() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.5", """
                [{"type":"memory_20250818","name":"memory","strict":true}]
                """);

        // Act
        JsonNode tool = convertRequest(request).at("/tools/0");

        // Assert
        assertThat(tool.path("strict").asBoolean()).isFalse();
    }

    @Test
    void test_keepsProgrammaticCallerSupport_when_nativeClientToolAllowsIt() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-6-astra", """
                [{"type":"memory_20250818","name":"memory","allowed_callers":["direct","code_execution_20260521"]}]
                """);

        // Act
        JsonNode tools = convertRequest(request).path("tools");

        // Assert
        assertThat(tools.get(0).path("type").asText()).isEqualTo("programmatic_tool_calling");
        assertThat(tools.at("/1/allowed_callers").toString()).isEqualTo("[\"direct\",\"programmatic\"]");
    }

    @Test
    void test_preservesSearchTextAndProvenance_when_toolReturnsSearchResult() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","content":[
                  {"type":"text","text":"Found one result"},
                  {"type":"search_result","source":"https://example.com/docs","title":"API guide",
                   "citations":{"enabled":true},"content":[{"type":"text","text":"First."},{"type":"text","text":"Second."}]}
                ]}
                """);

        // Act
        JsonNode output = convertRequest(request).at("/input/1/output");

        // Assert
        assertThat(output.at("/1/text").asText()).contains("https://example.com/docs", "API guide", "First.", "Second.");
        assertThat(output.at("/0/text").asText()).isEqualTo("Found one result");
    }

    @Test
    void test_preservesBrowserInventory_when_resultContainsBrowserState() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","content":[
                  {"type":"browser_state","tabs":[{"tab_id":"tab_1","title":"Docs","url":"https://example.com","active":true}],
                   "state_changes":[{"type":"download_started","download_id":"d1","url":"https://example.com/file"}]}
                ]}
                """);

        // Act
        String text = convertRequest(request).at("/input/1/output/0/text").asText();

        // Assert
        JsonNode state = mapper.readTree(text.substring("Browser state: ".length()));
        assertThat(state.path("tabs")).isEqualTo(request.at("/messages/1/content/0/content/0/tabs"));
        assertThat(state.path("state_changes")).isEqualTo(request.at("/messages/1/content/0/content/0/state_changes"));
    }

    @Test
    void test_marksFailedResult_when_errorTextLooksLikeSuccess() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","is_error":true,"content":"42"}
                """);

        // Act
        JsonNode output = convertRequest(request).at("/input/1/output");

        // Assert
        assertThat(output.asText()).isEqualTo("[Tool execution failed]\n42");
    }

    @Test
    void test_keepsImageAlongsideErrorMarker_when_failedResultIsMultimodal() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","is_error":true,"content":[
                  {"type":"image","source":{"type":"url","url":"https://example.com/error.png"}}
                ]}
                """);

        // Act
        JsonNode output = convertRequest(request).at("/input/1/output");

        // Assert
        assertThat(output.at("/0/text").asText()).isEqualTo("[Tool execution failed]");
        assertThat(output.at("/1/image_url").asText()).isEqualTo("https://example.com/error.png");
    }

    @Test
    void test_doesNotWrapSuccessfulJson_when_programMayParseResult() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","content":"{\\"answer\\":42}"}
                """);

        // Act
        JsonNode output = convertRequest(request).at("/input/1/output");

        // Assert
        assertThat(output.asText()).isEqualTo("{\"answer\":42}");
    }

    @Test
    void test_loadsOnlyDiscoveredFunction_when_clientReturnsToolReference() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","content":[{"type":"tool_reference","tool_name":"Edit"}]}
                """);
        request.set("tools", mapper.readTree("""
                [{"name":"Search","input_schema":{"type":"object"}},
                 {"name":"Edit","input_schema":{"type":"object"},"defer_loading":true},
                 {"name":"Unrelated","input_schema":{"type":"object"},"defer_loading":true}]
                """));

        // Act
        JsonNode tools = convertRequest(request).path("tools");

        // Assert
        assertThat(tools.at("/2/defer_loading").isMissingNode()).isTrue();
        assertThat(tools.at("/3/defer_loading").asBoolean()).isTrue();
    }

    @Test
    void test_doesNotLoadFailedDiscovery_when_toolReferenceIsInErrorResult() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","is_error":true,"content":[{"type":"tool_reference","tool_name":"Edit"}]}
                """);
        request.set("tools", mapper.readTree("""
                [{"name":"Edit","input_schema":{"type":"object"},"defer_loading":true}]
                """));

        // Act
        JsonNode tools = convertRequest(request).path("tools");

        // Assert
        assertThat(tools.at("/1/defer_loading").asBoolean()).isTrue();
    }

    @Test
    void test_keepsPreviouslyUsedToolLoaded_when_referenceHasLeftHistory() throws Exception {
        // Arrange
        ObjectNode request = requestWithResult("""
                {"type":"tool_result","tool_use_id":"call_1","content":"ok"}
                """);
        request.set("tools", mapper.readTree("""
                [{"name":"Search","input_schema":{"type":"object"},"defer_loading":true}]
                """));

        // Act
        JsonNode tools = convertRequest(request).path("tools");

        // Assert
        assertThat(tools.at("/1/defer_loading").isMissingNode()).isTrue();
    }

    @Test
    void test_eagerlyLoadsFunctionsAndMcp_when_targetLacksNativeToolSearch() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.3-codex", """
                [{"type":"tool_search_tool_bm25_20251119","name":"tool_search_tool_bm25"},
                 {"name":"Edit","input_schema":{"type":"object"},"defer_loading":true},
                 {"type":"mcp_toolset","mcp_server_name":"docs","defer_loading":true,
                  "default_config":{"enabled":false},"configs":{"search":{"enabled":true}}}]
                """);
        request.set("mcp_servers", mapper.readTree("""
                [{"type":"url","name":"docs","url":"https://example.com/mcp"}]
                """));

        // Act
        JsonNode tools = convertRequest(request).path("tools");

        // Assert
        assertThat(tools).noneSatisfy(tool -> assertThat(tool.has("defer_loading")).isTrue());
        assertThat(tools.at("/0/name").asText()).isEqualTo("Edit");
        assertThat(tools.at("/1/allowed_tools").toString()).isEqualTo("[\"search\"]");
        assertThat(tools).noneSatisfy(tool -> assertThat(tool.path("type").asText()).isEqualTo("tool_search"));
    }

    @Test
    void test_loadsForcedFunction_when_namedChoiceTargetsDeferredTool() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-6-astra", """
                [{"name":"Edit","input_schema":{"type":"object"},"defer_loading":true}]
                """);
        request.set("tool_choice", mapper.createObjectNode().put("type", "tool").put("name", "Edit"));

        // Act
        JsonNode mapped = convertRequest(request);

        // Assert
        assertThat(mapped.at("/tools/1/defer_loading").isMissingNode()).isTrue();
        assertThat(mapped.path("tool_choice").toString()).isEqualTo("{\"type\":\"function\",\"name\":\"Edit\"}");
    }

    @ParameterizedTest
    @CsvSource({"web_search_20260318,web_search,web_search", "code_execution_20260521,code_execution,code_interpreter",
            "tool_search_tool_bm25_20251119,tool_search_tool_bm25,tool_search"})
    void test_forcesHostedToolUsingItsActualType_when_claudeSelectsItByName(String type, String name, String target) throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-6-astra", """
                [{"type":"%s","name":"%s"}]
                """.formatted(type, name));
        request.set("tool_choice", mapper.createObjectNode().put("type", "tool").put("name", name));

        // Act
        JsonNode choice = convertRequest(request).path("tool_choice");

        // Assert
        assertThat(choice).isEqualTo(mapper.readTree("""
                {"type":"allowed_tools","mode":"required","tools":[{"type":"%s"}]}
                """.formatted(target)));
    }

    @Test
    void test_preservesCustomToolNamedWebSearch_when_nameMatchesHostedTool() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.5", """
                [{"name":"web_search","input_schema":{"type":"object"}}]
                """);
        request.set("tool_choice", mapper.createObjectNode().put("type", "tool").put("name", "web_search"));

        // Act
        JsonNode choice = convertRequest(request).path("tool_choice");

        // Assert
        assertThat(choice.toString()).isEqualTo("{\"type\":\"function\",\"name\":\"web_search\"}");
    }

    @Test
    void test_rejectsUnavailableForcedTool_when_legacyTargetCannotExecuteToolSearch() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-5.3", """
                [{"type":"tool_search_tool_bm25_20251119","name":"tool_search_tool_bm25"}]
                """);
        request.set("tool_choice", mapper.createObjectNode().put("type", "tool").put("name", "tool_search_tool_bm25"));

        // Act / Assert
        assertThatThrownBy(() -> convertRequest(request)).hasMessageContaining("NAMED_TOOL_CHOICE_NOT_AVAILABLE");
    }

    @Test
    void test_rejectsUnknownNativeVersion_when_inputContractIsUnverified() throws Exception {
        // Arrange
        ObjectNode request = requestWithTools("gpt-6-astra", """
                [{"type":"bash_20990101","name":"bash"}]
                """);

        // Act / Assert
        assertThatThrownBy(() -> convertRequest(request)).hasMessageContaining("SERVER_TOOL_NOT_SUPPORTED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bash", "str_replace_based_edit_tool", "memory"})
    void test_roundTripsNativeCallAndResult_when_responseIsNonStreaming(String name) throws Exception {
        // Arrange
        String arguments = nativeArguments(name);
        ObjectNode upstream = responseWithCall(name, arguments);
        JsonNode claude = convertResponse(upstream);
        ObjectNode request = requestWithTools("gpt-6-astra", "[]");
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "assistant").set("content", claude.path("content"));
        messages.addObject().put("role", "user").putArray("content").addObject()
                .put("type", "tool_result").put("tool_use_id", "call_1").put("content", "ok");

        // Act
        JsonNode input = convertRequest(request).path("input");

        // Assert
        assertThat(input.get(0)).isEqualTo(upstream.at("/output/0"));
        assertThat(input.get(1).toString()).isEqualTo("{\"type\":\"function_call_output\",\"call_id\":\"call_1\",\"output\":\"ok\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bash", "str_replace_based_edit_tool", "memory"})
    void test_streamsExecutableNativeCall_when_argumentsArriveInFragments(String name) throws Exception {
        // Arrange
        String arguments = nativeArguments(name);
        ObjectNode upstream = responseWithCall(name, arguments);
        ObjectNode added = mapper.createObjectNode().put("type", "response.output_item.added").put("output_index", 0);
        ObjectNode item = upstream.at("/output/0").deepCopy();
        item.put("arguments", "");
        added.set("item", item);
        StringBuilder wire = new StringBuilder(sse(added));
        for (String fragment : List.of(arguments.substring(0, 12), arguments.substring(12))) {
            wire.append(sse(mapper.createObjectNode().put("type", "response.function_call_arguments.delta")
                    .put("output_index", 0).put("delta", fragment)));
        }
        wire.append(sse(mapper.createObjectNode().put("type", "response.function_call_arguments.done")
                .put("output_index", 0).put("arguments", arguments)));
        wire.append(sse(mapper.createObjectNode().put("type", "response.completed").set("response", upstream)));
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        new UnifiedStreamingConversionAdapter(mapper).transform(GatewayStreamingConversionContext.of(
                ProtocolType.OPENAI_RESPONSES, ProtocolType.CLAUDE_MESSAGES,
                ModelName.of("claude-client"), ProviderChannelId.of(1L), ModelName.of("gpt-6-astra")),
                new ByteArrayInputStream(wire.toString().getBytes(StandardCharsets.UTF_8)), downstream);

        // Assert: reassemble the exact input the client's executor will receive.
        List<JsonNode> events = dataEvents(downstream.toString(StandardCharsets.UTF_8));
        StringBuilder input = new StringBuilder();
        for (JsonNode event : events) {
            if ("input_json_delta".equals(event.at("/delta/type").asText())) {
                input.append(event.at("/delta/partial_json").asText());
            }
        }
        assertThat(mapper.readTree(input.toString())).isEqualTo(mapper.readTree(arguments));
        assertThat(events).anySatisfy(event -> {
            assertThat(event.at("/content_block/name").asText()).isEqualTo(name);
            assertThat(event.at("/content_block/id").asText()).isEqualTo("call_1");
        });
        assertThat(events).anySatisfy(event -> assertThat(event.at("/delta/stop_reason").asText()).isEqualTo("tool_use"));
    }

    private ObjectNode requestWithTools(String model, String tools) throws Exception {
        ObjectNode request = mapper.createObjectNode().put("model", model).put("max_tokens", 2048);
        request.set("tools", mapper.readTree(tools));
        request.putArray("messages").addObject().put("role", "user").put("content", "Inspect the project");
        return request;
    }

    private ObjectNode requestWithResult(String result) throws Exception {
        ObjectNode request = requestWithTools("gpt-6-astra", "[]");
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "assistant").putArray("content").addObject()
                .put("type", "tool_use").put("id", "call_1").put("name", "Search")
                .set("input", mapper.createObjectNode());
        messages.addObject().put("role", "user").putArray("content").add(mapper.readTree(result));
        return request;
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

    private ObjectNode responseWithCall(String name, String arguments) {
        ObjectNode response = mapper.createObjectNode().put("id", "resp_1").put("model", "gpt-6-astra").put("status", "completed");
        response.putArray("output").addObject().put("type", "function_call").put("call_id", "call_1")
                .put("name", name).put("arguments", arguments);
        response.putObject("usage").put("input_tokens", 3).put("output_tokens", 4);
        return response;
    }

    private String nativeArguments(String name) {
        return "bash".equals(name) ? "{\"restart\":true}"
                : "{\"command\":\"view\",\"path\":\"/memories\"}";
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
