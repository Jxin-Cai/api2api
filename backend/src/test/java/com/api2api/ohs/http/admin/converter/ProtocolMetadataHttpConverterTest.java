package com.api2api.ohs.http.admin.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.FieldType;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinition;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinitionId;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadataId;
import com.api2api.domain.protocolmetadata.model.UsageDirection;
import com.api2api.ohs.http.admin.dto.ProtocolFieldSectionResponse;
import com.api2api.ohs.http.admin.dto.ProtocolMetadataDetailResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolMetadataHttpConverterTest {

    private final ProtocolMetadataHttpConverter converter = new ProtocolMetadataHttpConverter();

    @Test
    void test_orders_content_block_after_message_when_metadata_contains_content_block_fields() {
        // Arrange
        ProtocolMetadata metadata = metadata(List.of(
                field(1L, FieldSection.MESSAGE),
                field(2L, FieldSection.CONTENT_BLOCK),
                field(3L, FieldSection.MODEL)
        ));

        // Act
        ProtocolMetadataDetailResponse response = converter.toDetailResponse(metadata);

        // Assert
        assertThat(response.getSections())
                .extracting(ProtocolFieldSectionResponse::getSection, ProtocolFieldSectionResponse::getSectionLabel)
                .containsExactly(
                        tuple("MESSAGE", "消息内容"),
                        tuple("CONTENT_BLOCK", "内容块"),
                        tuple("MODEL", "模型参数")
                );
    }

    @Test
    void test_preserves_all_fields_when_mapping_metadata_detail_response() {
        // Arrange
        ProtocolMetadata metadata = metadata(List.of(
                field(1L, FieldSection.MESSAGE),
                field(2L, FieldSection.CONTENT_BLOCK),
                field(3L, FieldSection.CONTENT_BLOCK),
                field(4L, FieldSection.MODEL)
        ));

        // Act
        ProtocolMetadataDetailResponse response = converter.toDetailResponse(metadata);

        // Assert
        int sectionFieldCount = response.getSections().stream()
                .mapToInt(ProtocolFieldSectionResponse::getFieldCount)
                .sum();
        assertThat(sectionFieldCount).isEqualTo(response.getFieldCount());
    }

    private ProtocolMetadata metadata(List<ProtocolFieldDefinition> fields) {
        Instant now = Instant.parse("2026-07-15T00:00:00Z");
        return ProtocolMetadata.rehydrate(
                ProtocolMetadataId.of(1L),
                ProtocolType.CLAUDE_MESSAGES,
                "Claude Messages",
                "Messages API 2024-01-01",
                "Claude protocol metadata",
                "/v1/messages",
                fields,
                now,
                now
        );
    }

    private ProtocolFieldDefinition field(long id, FieldSection section) {
        return ProtocolFieldDefinition.of(
                ProtocolFieldDefinitionId.of(id),
                "field-" + id,
                "field." + id,
                FieldType.STRING,
                false,
                section,
                UsageDirection.INPUT,
                "Description",
                "Purpose",
                "Usage context",
                Math.toIntExact(id)
        );
    }
}
