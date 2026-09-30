package com.api2api.infr.protocol.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import com.api2api.domain.protocolcontract.model.ProtocolContractViolationException;
import com.api2api.infr.repository.protocolmetadata.ProtocolMetadataRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolContractRegistryTest {

    private final ProtocolContractRegistry registry = new ProtocolContractRegistry(new ObjectMapper());

    @Test
    void test_acceptsStateReferenceWithoutInput_when_responsesUsesPreviousResponse() {
        // Arrange
        String body = "{\"model\":\"gpt-6-astra\",\"previous_response_id\":\"resp_1\"}";

        // Act
        ParsedGatewayRequest parsed = registry.parseRequest(ProtocolType.OPENAI_RESPONSES, body);

        // Assert
        assertEquals("gpt-6-astra", parsed.model());
    }

    @Test
    void test_detectsToolCapability_when_responsesHistoryContainsToolResultWithoutTools() {
        // Arrange
        String body = """
                {"model":"gpt-6-astra","input":[
                  {"type":"function_call_output","call_id":"call_1","output":"ok"}]}
                """;

        // Act
        ParsedGatewayRequest parsed = registry.parseGatewayRequest(ProtocolType.OPENAI_RESPONSES, body);

        // Assert
        assertTrue(parsed.toolCallingRequired());
    }

    @Test
    void test_detectsReasoningCapability_when_responsesHistoryContainsEncryptedReasoning() {
        // Arrange
        String body = """
                {"model":"gpt-6-astra","input":[
                  {"type":"reasoning","id":"rs_1","encrypted_content":"opaque","summary":[]}]}
                """;

        // Act
        ParsedGatewayRequest parsed = registry.parseGatewayRequest(ProtocolType.OPENAI_RESPONSES, body);

        // Assert
        assertTrue(parsed.reasoningRequired());
    }

    @Test
    void test_acceptsTypedHistoryUnion_when_responsesUsesCurrentItems() {
        // Arrange
        String body = """
                {"model":"gpt-6-astra","input":[
                  {"type":"message","role":"assistant","phase":"commentary","content":"working"},
                  {"type":"configuration_update","reasoning":{"effort":"max"}},
                  {"type":"tool_search_call","arguments":{"query":"find"}},
                  {"type":"computer_call_output","call_id":"call_1",
                   "output":{"type":"computer_screenshot","image_url":"data:image/png;base64,AA=="}},
                  {"type":"function_call_output","call_id":"call_2",
                   "output":[{"type":"input_text","text":"result"}]}],
                 "reasoning":{"context":"all_turns","mode":"pro"},
                 "prompt_cache_options":{"mode":"implicit","ttl":"30m"},
                 "access_programs":{"cyber":"standard"}}
                """;

        // Act
        ParsedGatewayRequest parsed = registry.parseRequest(ProtocolType.OPENAI_RESPONSES, body);

        // Assert
        assertEquals("gpt-6-astra", parsed.model());
    }

    @Test
    void test_keepsNativeContractUnchanged_when_clientContainsCompatibilityExtension() throws Exception {
        // Arrange
        String body = """
                {"model":"gpt-6-astra","input":[{"role":"user","content":"hi","author":"client"}]}
                """;

        // Act
        var parsed = registry.require(ProtocolType.OPENAI_RESPONSES).parseRequestNode(body);

        // Assert
        assertEquals(new ObjectMapper().readTree(body), parsed);
    }

    @Test
    void test_exposesCurrentMessagesFields_when_nativeSchemaIsRequested() {
        // Arrange / Act
        List<String> fields = fieldPaths(ProtocolType.CLAUDE_MESSAGES);

        // Assert
        assertTrue(fields.containsAll(List.of("compaction.type", "compaction.instructions",
                "messages[].clear_at", "messages[].output_config.effort", "response.diagnostics")));
    }

    @Test
    void test_registry_contains_executable_shapes_when_five_protocols_are_registered() {
        assertEquals(5, registry.contracts().size());
        for (ProtocolContract contract : registry.contracts()) {
            assertFalse(contract.fields().isEmpty());
            assertFalse(contract.requestShape().fields().isEmpty());
            assertFalse(contract.responseShape().fields().isEmpty());
            assertFalse(contract.streamEventShape().fields().isEmpty());
        }
    }

    @Test
    void test_parseRequest_reads_model_and_stream_through_contract_fields_when_claude_body_is_valid() {
        String body = """
                {"model":"claude-3-5-sonnet","max_tokens":32,"stream":true,
                 "messages":[{"role":"user","content":[{"type":"text","text":"hello"}]}]}
                """;

        ParsedGatewayRequest parsed = registry.parseRequest(ProtocolType.CLAUDE_MESSAGES, body);

        assertEquals("claude-3-5-sonnet", parsed.model());
        assertEquals(true, parsed.streaming());
    }

    @Test
    void test_parseGatewayRequest_extractsSignalsInOnePass_when_claude_historyContainsMixedContent() {
        String body = """
                {"model":"claude-3-5-sonnet","max_tokens":32,"stream":true,
                 "messages":[
                   {"role":"assistant","content":[{"type":"thinking","thinking":"work"}]},
                   {"role":"assistant","content":[{"type":"tool_use","id":"tool-1","name":"Read","input":{}}]},
                   {"role":"user","content":[{"type":"tool_result","tool_use_id":"tool-1","content":"ok"}]}
                 ]}
                """;

        ParsedGatewayRequest parsed = registry.parseGatewayRequest(ProtocolType.CLAUDE_MESSAGES, body);

        assertTrue(parsed.streaming());
        assertTrue(parsed.toolCallingRequired());
        assertTrue(parsed.reasoningRequired());
    }

    @Test
    void test_parseRequest_rejects_required_model_with_wrong_type_when_claude_body_is_invalid() {
        String body = """
                {"model":123,"max_tokens":32,"messages":[{"role":"user","content":"hello"}]}
                """;

        assertThrows(ProtocolContractViolationException.class,
                () -> registry.parseRequest(ProtocolType.CLAUDE_MESSAGES, body));
    }

    @Test
    void test_parseRequest_reads_model_and_stream_when_images_body_requests_streaming() {
        String body = """
                {"model":"gpt-image-2","prompt":"A cat on a bench","stream":true,"partial_images":2}
                """;

        ParsedGatewayRequest parsed = registry.parseRequest(ProtocolType.OPENAI_IMAGES, body);

        assertEquals("gpt-image-2", parsed.model());
        assertEquals(true, parsed.streaming());
    }

    @Test
    void test_parseRequest_rejects_missing_prompt_when_images_body_omits_required_field() {
        String body = """
                {"model":"gpt-image-2"}
                """;

        assertThrows(ProtocolContractViolationException.class,
                () -> registry.parseRequest(ProtocolType.OPENAI_IMAGES, body));
    }

    @Test
    void test_parseRequest_accepts_string_message_content_when_openai_responses_uses_text_shorthand() {
        String body = """
                {"model":"gpt-5.5","stream":true,
                 "input":[{"type":"message","role":"user","content":"Hello, what can you do?"}],
                 "reasoning":{"effort":"high","summary":"detailed"},
                 "tools":[],"instructions":null}
                """;

        ParsedGatewayRequest parsed = registry.parseRequest(ProtocolType.OPENAI_RESPONSES, body);

        assertEquals("gpt-5.5", parsed.model());
    }

    @Test
    void test_metadata_projects_the_same_field_refs_when_registry_is_the_source() {
        ProtocolMetadataRepositoryImpl repository = new ProtocolMetadataRepositoryImpl(registry);

        for (ProtocolContract contract : registry.contracts()) {
            var metadata = repository.findByProtocolType(contract.protocolType()).orElseThrow();
            List<String> contractPaths = contract.fields().stream().map(ProtocolFieldRef::path).toList();
            List<String> metadataPaths = metadata.fieldDefinitions().stream()
                    .map(field -> field.fieldPath()).toList();
            assertEquals(contractPaths, metadataPaths);
        }
    }

    @Test
    void test_reports_official_api_versions_when_five_protocols_are_registered() {
        assertEquals("Anthropic API 2023-06-01 · schema 2026-09-30",
                registry.require(ProtocolType.CLAUDE_MESSAGES).apiSpecVersion());
        assertEquals("OpenAI API v1 · schema 2026-09-30",
                registry.require(ProtocolType.OPENAI_RESPONSES).apiSpecVersion());
        assertEquals("OpenAI API v1 · SDK 6.47.0",
                registry.require(ProtocolType.OPENAI_CHAT_COMPLETIONS).apiSpecVersion());
        assertEquals("Bedrock Runtime InvokeModel · Claude Messages passthrough",
                registry.require(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES).apiSpecVersion());
        assertEquals("OpenAI API v1 · Images generations / edits / variations",
                registry.require(ProtocolType.OPENAI_IMAGES).apiSpecVersion());
    }

    @Test
    void test_uses_official_flattened_tool_paths_when_responses_contract_is_registered() {
        List<String> fieldPaths = registry.require(ProtocolType.OPENAI_RESPONSES).fields().stream()
                .map(ProtocolFieldRef::path)
                .toList();

        assertTrue(fieldPaths.containsAll(List.of(
                "tools[].name",
                "tools[].description",
                "tools[].parameters",
                "tools[].strict"
        )));
        assertFalse(fieldPaths.stream().anyMatch(path -> path.startsWith("tools[].function.")));
    }

    @Test
    void test_exposes_latest_official_fields_when_sdk_schema_snapshots_are_registered() {
        assertTrue(fieldPaths(ProtocolType.CLAUDE_MESSAGES).containsAll(List.of(
                "fallback_credit_token",
                "response.container",
                "usage.server_tool_use.web_fetch_requests"
        )));
        assertTrue(fieldPaths(ProtocolType.OPENAI_RESPONSES).containsAll(List.of(
                "context_management[].compact_threshold",
                "moderation.model",
                "tools[].skills"
        )));
        assertTrue(fieldPaths(ProtocolType.OPENAI_CHAT_COMPLETIONS).containsAll(List.of(
                "messages[].content[].input_audio.data",
                "prompt_cache_options",
                "usage.prompt_tokens_details.cache_write_tokens"
        )));
        assertTrue(fieldPaths(ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES).containsAll(List.of(
                "anthropic_version",
                "messages",
                "stream.event"
        )));
        assertTrue(fieldPaths(ProtocolType.OPENAI_IMAGES).containsAll(List.of(
                "prompt",
                "image",
                "mask",
                "data[].b64_json",
                "usage.output_tokens_details.image_tokens",
                "stream.event.image_generation.completed",
                "stream.event.image_edit.completed"
        )));
    }

    private List<String> fieldPaths(ProtocolType protocolType) {
        return registry.require(protocolType).fields().stream()
                .map(ProtocolFieldRef::path)
                .toList();
    }
}
