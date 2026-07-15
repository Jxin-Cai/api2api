package com.api2api.ohs.http.admin.converter;

import com.api2api.domain.protocolmetadata.model.FieldSection;
import com.api2api.domain.protocolmetadata.model.ProtocolFieldDefinition;
import com.api2api.domain.protocolmetadata.model.ProtocolMetadata;
import com.api2api.ohs.http.admin.dto.ProtocolFieldDefinitionItemResponse;
import com.api2api.ohs.http.admin.dto.ProtocolFieldSectionResponse;
import com.api2api.ohs.http.admin.dto.ProtocolMetadataDetailResponse;
import com.api2api.ohs.http.admin.dto.ProtocolMetadataListItemResponse;
import com.api2api.ohs.http.admin.dto.ProtocolMetadataListResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProtocolMetadataHttpConverter {

    private static final List<FieldSection> SECTION_ORDER = List.of(
            FieldSection.MESSAGE, FieldSection.CONTENT_BLOCK, FieldSection.MODEL,
            FieldSection.TOOL, FieldSection.REASONING, FieldSection.STREAMING,
            FieldSection.USAGE, FieldSection.METADATA, FieldSection.OTHER
    );

    public ProtocolMetadataListResponse toListResponse(List<ProtocolMetadata> metadataList) {
        List<ProtocolMetadataListItemResponse> items = metadataList.stream()
                .map(this::toListItemResponse)
                .toList();
        return ProtocolMetadataListResponse.builder().protocols(items).build();
    }

    public ProtocolMetadataListItemResponse toListItemResponse(ProtocolMetadata metadata) {
        return ProtocolMetadataListItemResponse.builder()
                .id(metadata.id().value())
                .protocolType(metadata.protocolType().name())
                .displayName(metadata.displayName())
                .apiSpecVersion(metadata.apiSpecVersion())
                .description(metadata.description())
                .defaultEndpointPath(metadata.defaultEndpointPath())
                .fieldCount(metadata.fieldCount())
                .inputFieldCount(metadata.inputFields().size())
                .outputFieldCount(metadata.outputFields().size())
                .build();
    }

    public ProtocolMetadataDetailResponse toDetailResponse(ProtocolMetadata metadata) {
        Map<FieldSection, List<ProtocolFieldDefinition>> fieldsBySection = metadata.fieldsBySection();

        List<ProtocolFieldSectionResponse> sections = SECTION_ORDER.stream()
                .filter(fieldsBySection::containsKey)
                .map(section -> toSectionResponse(section, fieldsBySection.get(section)))
                .toList();

        return ProtocolMetadataDetailResponse.builder()
                .id(metadata.id().value())
                .protocolType(metadata.protocolType().name())
                .displayName(metadata.displayName())
                .apiSpecVersion(metadata.apiSpecVersion())
                .description(metadata.description())
                .defaultEndpointPath(metadata.defaultEndpointPath())
                .sections(sections)
                .fieldCount(metadata.fieldCount())
                .inputFieldCount(metadata.inputFields().size())
                .outputFieldCount(metadata.outputFields().size())
                .createdAt(metadata.createdAt().toEpochMilli())
                .updatedAt(metadata.updatedAt().toEpochMilli())
                .build();
    }

    private ProtocolFieldSectionResponse toSectionResponse(FieldSection section, List<ProtocolFieldDefinition> fields) {
        List<ProtocolFieldDefinitionItemResponse> fieldResponses = fields.stream()
                .sorted(Comparator.comparingInt(ProtocolFieldDefinition::sortOrder))
                .map(this::toFieldResponse)
                .toList();

        return ProtocolFieldSectionResponse.builder()
                .section(section.name())
                .sectionLabel(section.label())
                .fieldCount(fields.size())
                .fields(fieldResponses)
                .build();
    }

    private ProtocolFieldDefinitionItemResponse toFieldResponse(ProtocolFieldDefinition field) {
        return ProtocolFieldDefinitionItemResponse.builder()
                .id(field.id().value())
                .fieldName(field.fieldName())
                .fieldPath(field.fieldPath())
                .fieldType(field.fieldType().name())
                .required(field.required())
                .usageDirection(field.usageDirection().name())
                .description(field.description())
                .purpose(field.purpose())
                .usageContext(field.usageContext())
                .sortOrder(field.sortOrder())
                .build();
    }
}
