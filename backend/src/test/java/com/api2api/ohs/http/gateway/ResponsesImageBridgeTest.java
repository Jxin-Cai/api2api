package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.gateway.*;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.gateway.model.GatewayInvocationResult;
import com.api2api.domain.gateway.model.InvocationStatus;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.infr.protocol.JsonMultipartFormPayloadCodec;
import com.api2api.infr.protocol.StreamingPassthroughUsageExtractor;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ResponsesImageBridgeTest {
    private final ObjectMapper json = new ObjectMapper();
    private final GatewayInvocationApplicationService gateway = mock(GatewayInvocationApplicationService.class);
    private final ApiCredentialApplicationService credentials = mock(ApiCredentialApplicationService.class);
    private final JsonMultipartFormPayloadCodec multipart = new JsonMultipartFormPayloadCodec(json);
    private final List<InvokeGatewayCommand> calls = new ArrayList<>();
    private MockMvc mvc;
    private String imageResponse = """
            {"data":[{"b64_json":"aW1hZ2U=","size":"1024x1024","output_format":"png"}],
             "usage":{"input_tokens":10,"output_tokens":20,"total_tokens":30}}
            """;
    private String plannerResponse = """
            {"id":"resp_upstream","status":"completed","output":[{"type":"message","id":"msg_1","role":"assistant",
             "status":"completed","content":[{"type":"output_text","text":"Hello","annotations":[]}]}],
             "usage":{"input_tokens":5,"output_tokens":6,"total_tokens":11}}
            """;

    @BeforeEach
    void setUp() {
        GatewayApiKeyHashHelper keyHelper = new GatewayApiKeyHashHelper();
        GatewayIdentifierHelper identifiers = new GatewayIdentifierHelper();
        GatewayRequestMapper requestMapper = new GatewayRequestMapper(keyHelper, identifiers);
        GatewayProtocolErrorBodyBuilder errors = new GatewayProtocolErrorBodyBuilder(json);
        GatewayInvocationResponseMapper responses = new GatewayInvocationResponseMapper(json, errors);
        ProtocolContractRegistry contract = new ProtocolContractRegistry(json);
        ResponsesImageBridge bridge = new ResponsesImageBridge(json, new ResponsesImagePayloadMapper(json, multipart),
                gateway, requestMapper, identifiers, responses, contract, credentials,
                new StreamingPassthroughUsageExtractor(json), "gpt-image-2.5");
        GatewayProtocolController controller = new GatewayProtocolController(credentials, keyHelper, gateway,
                requestMapper, responses, mock(GatewayStreamingResponseMapper.class), contract,
                mock(MultipartFormRequestReader.class), multipart, bridge);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GatewayProtocolExceptionAdvice(errors)).build();
        when(gateway.invokeOutcome(any())).thenAnswer(invocation -> {
            InvokeGatewayCommand command = invocation.getArgument(0);
            calls.add(command);
            return outcome(command, 200, command.getRequestProtocol() == ProtocolType.OPENAI_IMAGES ? imageResponse : plannerResponse, Map.of());
        });
    }

    @Test
    void test_wraps_base64_image_when_image_tool_is_forced() throws Exception {
        // Arrange
        ObjectNode request = forced();
        // Act
        MvcResult result = send(request).andExpect(status().isOk()).andReturn();
        // Assert
        assertThat(json.readTree(result.getResponse().getContentAsString()).at("/output/0/result").asText()).isEqualTo("aW1hZ2U=");
    }

    @Test
    void test_routes_only_images_with_original_credential_when_image_tool_is_forced() throws Exception {
        // Arrange
        ObjectNode request = forced();
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(calls).singleElement().satisfies(command -> {
            assertThat(command.getRequestProtocol()).isEqualTo(ProtocolType.OPENAI_IMAGES);
            assertThat(command.getRequestedModel().value()).isEqualTo("gpt-image-2.5");
            assertThat(command.isToolCallingRequired()).isFalse();
            assertThat(command.isReasoningRequired()).isFalse();
            assertThat(command.getKeyHash()).isEqualTo(new GatewayApiKeyHashHelper().hashBearerToken("Bearer test-key"));
        });
    }

    @Test
    void test_uses_explicit_image_model_when_tool_overrides_default() throws Exception {
        // Arrange
        ObjectNode request = forced();
        ((ObjectNode) request.at("/tools/0")).put("model", "gpt-image-2.5-sunburst");
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(calls.get(0).getRequestedModel().value()).isEqualTo("gpt-image-2.5-sunburst");
    }

    @Test
    void test_leaves_text_response_when_model_does_not_choose_image_tool() throws Exception {
        // Arrange
        ObjectNode request = base().put("input", "Hello");
        // Act
        send(request).andExpect(status().isOk()).andExpect(jsonPath("$.output[0].content[0].text").value("Hello"));
        // Assert
        assertThat(calls).extracting(InvokeGatewayCommand::getRequestProtocol).containsExactly(ProtocolType.OPENAI_RESPONSES);
    }

    @Test
    void test_routes_images_after_planning_when_model_selects_bridge_function() throws Exception {
        // Arrange
        selectImage();
        // Act
        send(base()).andExpect(status().isOk());
        // Assert
        assertThat(calls).extracting(InvokeGatewayCommand::getRequestProtocol)
                .containsExactly(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_IMAGES);
    }

    @Test
    void test_preserves_external_tool_calls_when_image_and_function_are_selected() throws Exception {
        // Arrange
        selectImage();
        ObjectNode plan = (ObjectNode) json.readTree(plannerResponse);
        plan.withArray("output").addObject().put("id", "fc_external").put("type", "function_call")
                .put("call_id", "call_external").put("name", "get_weather").put("arguments", "{}");
        plannerResponse = plan.toString();
        // Act
        MvcResult result = send(base()).andExpect(status().isOk()).andReturn();
        // Assert
        assertThat(json.readTree(result.getResponse().getContentAsString()).at("/output/1/call_id").asText()).isEqualTo("call_external");
    }

    @Test
    void test_combines_main_and_image_usage_when_both_models_run() throws Exception {
        // Arrange
        selectImage();
        // Act
        send(base()).andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.usage.total_tokens").value(41));
    }

    @Test
    void test_removes_image_tool_when_tool_choice_is_none() throws Exception {
        // Arrange
        ObjectNode request = base().put("tool_choice", "none");
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(json.readTree(calls.get(0).getRequestBody()).has("tools")).isFalse();
    }

    @Test
    void test_preserves_regular_responses_when_no_image_tool_is_declared() throws Exception {
        // Arrange
        ObjectNode request = base();
        request.remove("tools");
        // Act
        send(request).andExpect(status().isOk())
                // Assert
                .andExpect(jsonPath("$.id").value("resp_upstream"));
    }

    @Test
    void test_builds_multipart_edit_when_inline_reference_is_present() throws Exception {
        // Arrange
        ObjectNode request = forced();
        var content = request.putArray("input").addObject().put("role", "user").putArray("content");
        content.addObject().put("type", "input_text").put("text", "Make the background blue");
        content.addObject().put("type", "input_image").put("image_url", "data:image/png;base64,aW1hZ2U=");
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(multipart.decode(calls.get(0).getRequestBody()).files()).singleElement().satisfies(file -> {
            assertThat(file.content()).isEqualTo("image".getBytes(StandardCharsets.UTF_8));
            assertThat(file.name()).isEqualTo("image[]");
        });
    }

    @Test
    void test_uses_previous_result_as_edit_input_when_image_history_is_resent() throws Exception {
        // Arrange
        ObjectNode request = forced();
        var input = request.putArray("input");
        input.addObject().put("type", "image_generation_call").put("result", "aW1hZ2U=").put("output_format", "png");
        input.addObject().put("role", "user").put("content", "Make it blue");
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(calls.get(0).getInbound().operation()).isEqualTo(ProtocolOperation.IMAGE_EDITS);
    }

    @Test
    void test_rejects_remote_edit_reference_before_upstream_call_when_url_is_not_inline() throws Exception {
        // Arrange
        ObjectNode request = forced();
        var content = request.putArray("input").addObject().put("role", "user").putArray("content");
        content.addObject().put("type", "input_text").put("text", "Edit");
        content.addObject().put("type", "input_image").put("image_url", "http://127.0.0.1/private");
        // Act
        send(request).andExpect(status().isBadRequest());
        // Assert
        verifyNoInteractions(gateway);
    }

    @Test
    void test_checks_parent_model_permission_before_calling_images_when_forced() throws Exception {
        // Arrange
        when(credentials.authenticateForGateway(any())).thenThrow(new BusinessException("MODEL_NOT_ALLOWED"));
        // Act
        send(forced()).andExpect(status().isForbidden());
        // Assert
        verifyNoInteractions(gateway);
    }

    @Test
    void test_preserves_quota_rejection_when_images_gateway_denies_invocation() throws Exception {
        // Arrange
        doThrow(new BusinessException("MODEL_DAILY_LIMIT_EXCEEDED")).when(gateway).invokeOutcome(any());
        // Act
        send(forced())
                // Assert
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void test_preserves_upstream_status_and_retry_after_when_images_are_rate_limited() throws Exception {
        // Arrange
        doAnswer(invocation -> outcome(invocation.getArgument(0), 429,
                "{\"error\":{\"message\":\"Try later\",\"type\":\"rate_limit_error\"}}", Map.of("Retry-After", List.of("30"))))
                .when(gateway).invokeOutcome(any());
        // Act
        send(forced())
                // Assert
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void test_returns_bad_gateway_when_images_success_contains_no_image() throws Exception {
        // Arrange
        imageResponse = "{\"data\":[]}";
        // Act
        send(forced())
                // Assert
                .andExpect(status().isBadGateway());
    }

    @Test
    void test_emits_responses_partial_image_events_when_images_streams() throws Exception {
        // Arrange
        streamImages("data: {\"type\":\"image_generation.partial_image\",\"b64_json\":\"cGFydA==\",\"partial_image_index\":0}\n\n"
                + "data: {\"type\":\"image_generation.completed\",\"b64_json\":\"aW1hZ2U=\",\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}\n\n");
        // Act
        List<JsonNode> events = stream(forced().put("stream", true));
        // Assert
        assertThat(events.stream().filter(event -> "response.image_generation_call.partial_image".equals(event.path("type").asText()))
                .map(event -> event.path("partial_image_b64").asText())).containsExactly("cGFydA==");
    }

    @Test
    void test_keeps_item_ids_and_sequence_consistent_when_stream_completes() throws Exception {
        // Arrange
        streamImages("event: image_generation.completed\ndata: {\"b64_json\":\"aW1hZ2U=\"}\n\n");
        // Act
        List<JsonNode> events = stream(forced().put("stream", true));
        // Assert
        String imageId = events.stream().filter(event -> event.has("item_id")).findFirst().orElseThrow().path("item_id").asText();
        for (int i = 0; i < events.size(); i++) assertThat(events.get(i).path("sequence_number").asInt()).isEqualTo(i);
        assertThat(events.get(events.size() - 1).at("/response/output/0/id").asText()).isEqualTo(imageId);
    }

    @Test
    void test_records_usage_when_stream_omits_event_name() throws Exception {
        // Arrange
        streamImages("data: {\"type\":\"image_generation.completed\",\"b64_json\":\"aW1hZ2U=\",\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}\n\n");
        // Act
        stream(forced().put("stream", true));
        // Assert
        ArgumentCaptor<UnifiedTokenUsage> usage = ArgumentCaptor.forClass(UnifiedTokenUsage.class);
        verify(gateway).completeStreamingSuccess(any(), usage.capture());
        assertThat(usage.getValue().totalTokens()).isEqualTo(30);
    }

    @Test
    void test_emits_failure_without_success_when_images_stream_is_truncated() throws Exception {
        // Arrange
        streamImages("data: {\"type\":\"image_generation.partial_image\",\"b64_json\":\"cGFydA==\"}\n\n");
        // Act
        List<JsonNode> events = stream(forced().put("stream", true));
        // Assert
        assertThat(events).extracting(event -> event.path("type").asText())
                .contains("response.failed").doesNotContain("response.completed", "response.image_generation_call.completed");
    }

    @Test
    void test_emits_failure_when_upstream_sends_error_event() throws Exception {
        // Arrange
        streamImages("event: error\ndata: {\"error\":{\"type\":\"rate_limit_error\",\"message\":\"No quota\"}}\n\n");
        // Act
        List<JsonNode> events = stream(forced().put("stream", true));
        // Assert
        assertThat(events.get(events.size() - 1).at("/response/error/message").asText()).isEqualTo("No quota");
    }

    @Test
    void test_supports_unversioned_alias_when_client_posts_responses() throws Exception {
        // Arrange
        ObjectNode request = forced();
        // Act
        mvc.perform(post("/responses").header("Authorization", "Bearer test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                // Assert
                .andExpect(status().isOk()).andExpect(jsonPath("$.output[0].type").value("image_generation_call"));
    }

    @Test
    void test_does_not_execute_forbidden_image_call_when_planner_ignores_tool_choice() throws Exception {
        // Arrange
        selectImage();
        // Act
        send(base().put("tool_choice", "none")).andExpect(status().isBadGateway());
        // Assert
        assertThat(calls).extracting(InvokeGatewayCommand::getRequestProtocol).containsExactly(ProtocolType.OPENAI_RESPONSES);
    }

    @Test
    void test_rejects_stateful_reference_when_image_bridge_cannot_restore_history() throws Exception {
        // Arrange
        ObjectNode request = forced().put("previous_response_id", "resp_previous");
        // Act
        send(request).andExpect(status().isBadRequest());
        // Assert
        verifyNoInteractions(gateway);
    }

    @Test
    void test_rejects_edit_without_reference_when_action_is_edit() throws Exception {
        // Arrange
        ObjectNode request = forced();
        ((ObjectNode) request.at("/tools/0")).put("action", "edit");
        // Act
        send(request).andExpect(status().isBadRequest());
        // Assert
        verifyNoInteractions(gateway);
    }

    @Test
    void test_forces_image_when_allowed_tools_requires_only_image_generation() throws Exception {
        // Arrange
        ObjectNode request = base();
        request.putObject("tool_choice").put("type", "allowed_tools").put("mode", "required")
                .putArray("tools").addObject().put("type", "image_generation");
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(calls).extracting(InvokeGatewayCommand::getRequestProtocol).containsExactly(ProtocolType.OPENAI_IMAGES);
    }

    @Test
    void test_maps_allowed_tool_choice_when_image_selection_is_automatic() throws Exception {
        // Arrange
        ObjectNode request = base();
        request.putObject("tool_choice").put("type", "allowed_tools").put("mode", "auto")
                .putArray("tools").addObject().put("type", "image_generation");
        // Act
        send(request).andExpect(status().isOk());
        // Assert
        assertThat(json.readTree(calls.get(0).getRequestBody()).at("/tool_choice/tools/0/name").asText()).isEqualTo("api2api_generate_image");
    }

    @Test
    void test_enforces_image_call_limit_before_side_effects_when_planner_exceeds_budget() throws Exception {
        // Arrange
        selectImage();
        ObjectNode plan = (ObjectNode) json.readTree(plannerResponse);
        plan.withArray("output").add(plan.at("/output/0").deepCopy());
        plannerResponse = plan.toString();
        // Act
        send(base().put("max_tool_calls", 1)).andExpect(status().isBadGateway());
        // Assert
        assertThat(calls).hasSize(1);
    }

    @Test
    void test_preserves_image_model_permission_failure_when_stream_has_started() throws Exception {
        // Arrange
        when(gateway.openStreaming(any())).thenThrow(new IllegalStateException("MODEL_NOT_ALLOWED: requested model is not allowed"));
        // Act
        List<JsonNode> events = stream(forced().put("stream", true));
        // Assert
        assertThat(events.get(events.size() - 1).at("/response/error/code").asText()).isEqualTo("MODEL_NOT_ALLOWED");
    }

    @Test
    void test_passes_quality_and_partial_count_when_streaming_images() throws Exception {
        // Arrange
        ObjectNode request = forced().put("stream", true);
        ((ObjectNode) request.at("/tools/0")).put("quality", "high").put("partial_images", 2);
        streamImages("data: {\"type\":\"image_generation.completed\",\"b64_json\":\"aW1hZ2U=\"}\n\n");
        // Act
        stream(request);
        // Assert
        assertThat(json.readTree(calls.get(0).getRequestBody()).path("partial_images").asInt()).isEqualTo(2);
    }

    @Test
    void test_uses_distinct_invocation_ids_when_main_model_and_images_are_called() throws Exception {
        // Arrange
        selectImage();
        // Act
        send(base()).andExpect(status().isOk());
        // Assert
        assertThat(calls).extracting(InvokeGatewayCommand::getUsageRecordId).doesNotHaveDuplicates();
    }

    private ObjectNode base() {
        ObjectNode request = json.createObjectNode().put("model", "gpt-5.6-sol").put("input", "Draw a blue cat");
        request.putArray("tools").addObject().put("type", "image_generation");
        return request;
    }

    private ObjectNode forced() {
        ObjectNode request = base();
        request.putObject("tool_choice").put("type", "image_generation");
        return request;
    }

    private void selectImage() {
        ObjectNode response = json.createObjectNode().put("status", "completed");
        response.putArray("output").addObject().put("type", "function_call").put("id", "fc_internal")
                .put("call_id", "call_internal").put("name", "api2api_generate_image")
                .put("arguments", "{\"prompt\":\"A blue cat\",\"action\":\"generate\"}");
        response.putObject("usage").put("input_tokens", 5).put("output_tokens", 6).put("total_tokens", 11);
        plannerResponse = response.toString();
    }

    private org.springframework.test.web.servlet.ResultActions send(ObjectNode request) throws Exception {
        return mvc.perform(post("/v1/responses").header("Authorization", "Bearer test-key")
                .contentType(MediaType.APPLICATION_JSON).content(request.toString()));
    }

    private List<JsonNode> stream(ObjectNode request) throws Exception {
        MvcResult initial = send(request).andExpect(request().asyncStarted()).andReturn();
        MvcResult result = mvc.perform(asyncDispatch(initial)).andExpect(status().isOk()).andReturn();
        List<JsonNode> events = new ArrayList<>();
        for (String line : result.getResponse().getContentAsString().split("\n")) {
            if (line.startsWith("data: ")) events.add(json.readTree(line.substring(6)));
        }
        return events;
    }

    private void streamImages(String events) {
        when(gateway.openStreaming(any())).thenAnswer(invocation -> {
            InvokeGatewayCommand command = invocation.getArgument(0);
            calls.add(command);
            return GatewayStreamingInvocation.opened(mock(GatewayInvocation.class), command.getUsageRecordId(),
                    mock(RouteCandidate.class), ProviderStreamingResponse.of(ProtocolType.OPENAI_IMAGES, 200,
                            Map.of("Content-Type", List.of("text/event-stream")),
                            new ByteArrayInputStream(events.getBytes(StandardCharsets.UTF_8))));
        });
    }

    private GatewayInvocationOutcome outcome(InvokeGatewayCommand command, int status, String body, Map<String, List<String>> headers) {
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        GatewayInvocationResult result = mock(GatewayInvocationResult.class);
        when(invocation.result()).thenReturn(result);
        when(invocation.requestId()).thenReturn(command.getGatewayRequestId());
        when(invocation.requestProtocol()).thenReturn(command.getRequestProtocol());
        when(result.status()).thenReturn(status == 200 ? InvocationStatus.SUCCESS : InvocationStatus.FAILED);
        return GatewayInvocationOutcome.of(invocation, ProviderGatewayResponse.of(command.getRequestProtocol(), status, headers, body, false));
    }
}
