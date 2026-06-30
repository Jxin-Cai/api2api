package com.api2api.ohs.http.admin.converter;

import com.api2api.application.protocol.command.ChangeProtocolConversionStatusCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingDocument;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.admin.dto.ProtocolConversionCapabilityResponse;
import com.api2api.ohs.http.admin.dto.ProtocolConversionFieldMappingResponse;
import com.api2api.ohs.http.admin.dto.ProtocolConversionListItemResponse;
import com.api2api.ohs.http.admin.dto.ProtocolConversionListResponse;
import com.api2api.ohs.http.admin.dto.ProtocolConversionMappingResponse;
import com.api2api.ohs.http.admin.dto.ProtocolConversionResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps HTTP requests to application commands and domain objects to HTTP responses for protocol conversions.
 */
@Mapper(config = MapStructConfig.class)
public interface ProtocolConversionHttpConverter {

    default ChangeProtocolConversionStatusCommand toChangeStatusCommand(
            UserAccountId operatorUserId,
            ProtocolConversionDefinitionId definitionId
    ) {
        return ChangeProtocolConversionStatusCommand.builder()
                .operatorUserId(operatorUserId)
                .definitionId(definitionId)
                .build();
    }

    default ProtocolType toProtocolType(String protocol) {
        return ProtocolType.valueOf(protocol.toUpperCase().replace('-', '_'));
    }

    @Mapping(target = "id", expression = "java(definition.id().value())")
    @Mapping(target = "sourceProtocol", expression = "java(definition.sourceProtocol().name())")
    @Mapping(target = "targetProtocol", expression = "java(definition.targetProtocol().name())")
    @Mapping(target = "kind", expression = "java(definition.kind().name())")
    @Mapping(target = "status", expression = "java(definition.status().name())")
    @Mapping(target = "implementationStatus", expression = "java(definition.implementationStatus().name())")
    @Mapping(target = "capability", expression = "java(toCapabilityResponse(definition.capability()))")
    @Mapping(target = "requestMapping", expression = "java(toMappingResponse(definition.requestMapping()))")
    @Mapping(target = "responseMapping", expression = "java(toMappingResponse(definition.responseMapping()))")
    @Mapping(target = "createdAt", expression = "java(definition.createdAt().toEpochMilli())")
    @Mapping(target = "updatedAt", expression = "java(definition.updatedAt().toEpochMilli())")
    ProtocolConversionResponse toResponse(ProtocolConversionDefinition definition);

    default ProtocolConversionListResponse toListResponse(List<ProtocolConversionDefinition> definitions) {
        return ProtocolConversionListResponse.builder()
                .conversions(definitions.stream().map(this::toListItemResponse).toList())
                .build();
    }

    @Mapping(target = "id", expression = "java(definition.id().value())")
    @Mapping(target = "sourceProtocol", expression = "java(definition.sourceProtocol().name())")
    @Mapping(target = "targetProtocol", expression = "java(definition.targetProtocol().name())")
    @Mapping(target = "kind", expression = "java(definition.kind().name())")
    @Mapping(target = "status", expression = "java(definition.status().name())")
    @Mapping(target = "implementationStatus", expression = "java(definition.implementationStatus().name())")
    @Mapping(target = "supportsStreaming", expression = "java(definition.capability().supportsStreaming())")
    @Mapping(target = "supportsToolCalling", expression = "java(definition.capability().supportsToolCalling())")
    @Mapping(target = "supportsReasoning", expression = "java(definition.capability().supportsReasoning())")
    @Mapping(target = "supportsUsageMapping", expression = "java(definition.capability().supportsUsageMapping())")
    @Mapping(target = "supportsCacheTokenMapping", expression = "java(definition.capability().supportsCacheTokenMapping())")
    ProtocolConversionListItemResponse toListItemResponse(ProtocolConversionDefinition definition);

    @Mapping(target = "supportsStreaming", expression = "java(capability.supportsStreaming())")
    @Mapping(target = "supportsToolCalling", expression = "java(capability.supportsToolCalling())")
    @Mapping(target = "supportsReasoning", expression = "java(capability.supportsReasoning())")
    @Mapping(target = "supportsUsageMapping", expression = "java(capability.supportsUsageMapping())")
    @Mapping(target = "supportsCacheTokenMapping", expression = "java(capability.supportsCacheTokenMapping())")
    @Mapping(target = "supportedContentTypes", expression = "java(toEnumNames(capability.supportedContentTypes()))")
    ProtocolConversionCapabilityResponse toCapabilityResponse(ConversionCapability capability);

    @Mapping(target = "direction", expression = "java(mapping.direction().name())")
    @Mapping(target = "title", expression = "java(mapping.title())")
    @Mapping(target = "summary", expression = "java(mapping.summary())")
    @Mapping(target = "fieldMappings", expression = "java(toFieldMappingResponses(mapping.fieldMappings()))")
    ProtocolConversionMappingResponse toMappingResponse(MappingDocument mapping);

    @Mapping(target = "sourceField", expression = "java(fieldMapping.sourceField())")
    @Mapping(target = "targetField", expression = "java(fieldMapping.targetField())")
    @Mapping(target = "ruleDescription", expression = "java(fieldMapping.ruleDescription())")
    @Mapping(target = "lossiness", expression = "java(fieldMapping.lossiness().name())")
    @Mapping(target = "category", expression = "java(fieldMapping.category())")
    @Mapping(target = "mappingType", expression = "java(fieldMapping.mappingType())")
    @Mapping(target = "sourcePath", expression = "java(fieldMapping.sourcePath())")
    @Mapping(target = "targetPath", expression = "java(fieldMapping.targetPath())")
    @Mapping(target = "sourceType", expression = "java(fieldMapping.sourceType())")
    @Mapping(target = "targetType", expression = "java(fieldMapping.targetType())")
    @Mapping(target = "required", expression = "java(fieldMapping.required())")
    @Mapping(target = "supported", expression = "java(fieldMapping.supported())")
    @Mapping(target = "defaultValue", expression = "java(fieldMapping.defaultValue())")
    @Mapping(target = "condition", expression = "java(fieldMapping.condition())")
    @Mapping(target = "notes", expression = "java(fieldMapping.notes())")
    ProtocolConversionFieldMappingResponse toFieldMappingResponse(FieldMapping fieldMapping);

    default List<ProtocolConversionFieldMappingResponse> toFieldMappingResponses(List<FieldMapping> fieldMappings) {
        return fieldMappings.stream().map(this::toFieldMappingResponse).toList();
    }

    default Set<String> toEnumNames(Set<? extends Enum<?>> values) {
        return values.stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
