package com.api2api.infr.repository.protocolmetadata;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProtocolMetadataRepositoryImplTest {

    @Test
    void test_loads_claude_messages_metadata_when_content_block_section_is_configured() {
        // Arrange
        ProtocolMetadataRepositoryImpl repository = new ProtocolMetadataRepositoryImpl(new ObjectMapper());

        // Act
        repository.loadFromConfig();

        // Assert
        ProtocolMetadata metadata = repository.findByProtocolType(ProtocolType.CLAUDE_MESSAGES).orElseThrow();
        assertThat(metadata.fieldCount()).isEqualTo(72);
    }

    @Test
    void test_groups_content_block_fields_when_claude_messages_metadata_is_loaded() {
        // Arrange
        ProtocolMetadataRepositoryImpl repository = new ProtocolMetadataRepositoryImpl(new ObjectMapper());

        // Act
        repository.loadFromConfig();

        // Assert
        ProtocolMetadata metadata = repository.findByProtocolType(ProtocolType.CLAUDE_MESSAGES).orElseThrow();
        assertThat(metadata.fieldsBySection().get(FieldSection.CONTENT_BLOCK)).hasSize(18);
    }
}
