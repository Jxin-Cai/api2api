package com.api2api.infr.repository.protocol.converter;

import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolConversionDefinitionPersistenceConverterTest {

    private final ProtocolConversionDefinitionPersistenceConverter converter = new ProtocolConversionDefinitionPersistenceConverter();

    @Test
    void toDomainBuildsFieldMappingsFromArrowSummary() {
        ProtocolConversionDefinition definition = converter.toDomain(po(
                "CLAUDE_MESSAGES",
                "OPENAI_RESPONSES",
                "Claude messages/model/max_tokens/system -> OpenAI responses input/model/max_output_tokens/instructions",
                "OpenAI responses output/usage -> Claude messages content/usage"
        ));

        assertThat(definition.requestMapping().fieldMappings())
                .extracting(mapping -> mapping.sourceField() + "->" + mapping.targetField())
                .containsExactly(
                        "messages->input",
                        "model->model",
                        "max_tokens->max_output_tokens",
                        "system->instructions"
                );
        assertThat(definition.responseMapping().fieldMappings())
                .extracting(mapping -> mapping.sourceField() + "->" + mapping.targetField())
                .containsExactly(
                        "output->content",
                        "usage->usage"
                );
    }

    @Test
    void toDomainKeepsPayloadFallbackForUnstructuredSummary() {
        ProtocolConversionDefinition definition = converter.toDomain(po(
                "CLAUDE_MESSAGES",
                "CLAUDE_MESSAGES",
                "Request passthrough",
                "Response passthrough"
        ));

        assertThat(definition.requestMapping().fieldMappings())
                .singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.sourceField()).isEqualTo("payload");
                    assertThat(mapping.targetField()).isEqualTo("payload");
                    assertThat(mapping.ruleDescription()).isEqualTo("Request passthrough");
                });
    }

    private ProtocolConversionDefinitionPO po(
            String sourceProtocol,
            String targetProtocol,
            String requestMappingJson,
            String responseMappingJson
    ) {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        boolean passthrough = sourceProtocol.equals(targetProtocol);
        return ProtocolConversionDefinitionPO.builder()
                .id(1L)
                .sourceProtocol(sourceProtocol)
                .targetProtocol(targetProtocol)
                .kind(passthrough ? "PASSTHROUGH" : "TRANSFORM")
                .status("ENABLED")
                .implementationStatus("IMPLEMENTED")
                .supportsStreaming(false)
                .supportsToolCalling(false)
                .supportsReasoning(false)
                .supportsUsageMapping(true)
                .supportsCacheTokenMapping(true)
                .requestMappingJson(requestMappingJson)
                .responseMappingJson(responseMappingJson)
                .createdTime(now)
                .updatedTime(now)
                .build();
    }
}
