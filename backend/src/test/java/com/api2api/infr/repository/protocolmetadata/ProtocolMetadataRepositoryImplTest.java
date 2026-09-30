package com.api2api.infr.repository.protocolmetadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.api2api.infr.protocol.contract.ProtocolFieldRef;
import org.junit.jupiter.api.Test;

class ProtocolMetadataRepositoryImplTest {

    @Test
    void test_loadsAllContractFields_when_claudeMessagesMetadataIsRequested() {
        // Arrange
        ProtocolContractRegistry registry = new ProtocolContractRegistry(new ObjectMapper());
        ProtocolMetadataRepositoryImpl repository = new ProtocolMetadataRepositoryImpl(registry);
        var expectedPaths = registry.contracts().stream()
                .filter(contract -> contract.protocolType() == ProtocolType.CLAUDE_MESSAGES)
                .findFirst().orElseThrow().fields().stream().map(ProtocolFieldRef::path).toList();

        // Act
        ProtocolMetadata metadata = repository.findByProtocolType(ProtocolType.CLAUDE_MESSAGES).orElseThrow();

        // Assert
        assertThat(metadata.fieldDefinitions()).extracting(ProtocolFieldDefinition::fieldPath)
                .containsExactlyElementsOf(expectedPaths);
    }

    @Test
    void test_groups_content_block_fields_when_claude_messages_metadata_is_loaded() {
        // Arrange
        ProtocolMetadataRepositoryImpl repository = new ProtocolMetadataRepositoryImpl(new ProtocolContractRegistry(new ObjectMapper()));

        // Act
        // Assert
        ProtocolMetadata metadata = repository.findByProtocolType(ProtocolType.CLAUDE_MESSAGES).orElseThrow();
        assertThat(metadata.fieldsBySection().get(FieldSection.CONTENT_BLOCK)).hasSize(30);

    }

    @Test
    void test_exposes_claude_code_deferred_tool_field_when_latest_metadata_is_loaded() {
        // Arrange
        ProtocolMetadataRepositoryImpl repository = new ProtocolMetadataRepositoryImpl(new ProtocolContractRegistry(new ObjectMapper()));

        // Act
        ProtocolMetadata metadata = repository.findByProtocolType(ProtocolType.CLAUDE_MESSAGES).orElseThrow();

        // Assert
        assertThat(metadata.fieldDefinitions())
                .anySatisfy(field -> assertThat(field.fieldPath()).isEqualTo("tools[].defer_loading"));
    }
}
