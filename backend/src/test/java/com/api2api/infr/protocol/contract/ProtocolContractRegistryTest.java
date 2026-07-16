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
    void test_registry_contains_executable_shapes_when_four_protocols_are_registered() {
        assertEquals(4, registry.contracts().size());
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
    void test_parseRequest_rejects_required_model_with_wrong_type_when_claude_body_is_invalid() {
        String body = """
                {"model":123,"max_tokens":32,"messages":[{"role":"user","content":"hello"}]}
                """;

        assertThrows(ProtocolContractViolationException.class,
                () -> registry.parseRequest(ProtocolType.CLAUDE_MESSAGES, body));
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
    void test_reports_official_api_versions_when_four_protocols_are_registered() {
        assertEquals("Anthropic API 2023-06-01", registry.require(ProtocolType.CLAUDE_MESSAGES).apiSpecVersion());
        assertEquals("OpenAI API v1", registry.require(ProtocolType.OPENAI_RESPONSES).apiSpecVersion());
        assertEquals("OpenAI API v1", registry.require(ProtocolType.OPENAI_CHAT_COMPLETIONS).apiSpecVersion());
        assertEquals("Bedrock Runtime 2023-09-30", registry.require(ProtocolType.AWS_BEDROCK_CONVERSE).apiSpecVersion());
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
}
