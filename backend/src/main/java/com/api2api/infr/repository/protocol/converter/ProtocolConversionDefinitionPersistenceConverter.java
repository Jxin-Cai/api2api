package com.api2api.infr.repository.protocol.converter;

import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ConversionImplementationStatus;
import com.api2api.domain.protocol.model.ConversionStatus;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingDirection;
import com.api2api.domain.protocol.model.MappingDocument;
import com.api2api.domain.protocol.model.MappingLossiness;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Converts protocol conversion definition aggregate to persistence object.
 */
@Component
public class ProtocolConversionDefinitionPersistenceConverter {

    public ProtocolConversionDefinitionPO toPO(ProtocolConversionDefinition definition) {
        return ProtocolConversionDefinitionPO.builder()
                .id(definition.id().value())
                .sourceProtocol(definition.sourceProtocol().name())
                .targetProtocol(definition.targetProtocol().name())
                .kind(definition.kind().name())
                .status(definition.status().name())
                .supportsStreaming(definition.capability().supportsStreaming())
                .supportsToolCalling(definition.capability().supportsToolCalling())
                .supportsReasoning(definition.capability().supportsReasoning())
                .supportsUsageMapping(definition.capability().supportsUsageMapping())
                .supportsCacheTokenMapping(definition.capability().supportsCacheTokenMapping())
                .requestMappingJson(toMappingText(definition.requestMapping()))
                .responseMappingJson(toMappingText(definition.responseMapping()))
                .implementationStatus(definition.implementationStatus().name())
                .createdTime(definition.createdAt())
                .updatedTime(definition.updatedAt())
                .build();
    }

    public ProtocolConversionDefinition toDomain(ProtocolConversionDefinitionPO po) {
        ProtocolType source = ProtocolType.valueOf(po.getSourceProtocol());
        ProtocolType target = ProtocolType.valueOf(po.getTargetProtocol());
        return ProtocolConversionDefinition.rehydrate(
                ProtocolConversionDefinitionId.of(po.getId()),
                source,
                target,
                toCapability(po),
                toMapping(MappingDirection.REQUEST, po.getRequestMappingJson()),
                toMapping(MappingDirection.RESPONSE, po.getResponseMappingJson()),
                ConversionImplementationStatus.valueOf(po.getImplementationStatus()),
                ConversionStatus.valueOf(po.getStatus()),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }

    private ConversionCapability toCapability(ProtocolConversionDefinitionPO po) {
        Set<ContentMappingType> contentTypes = EnumSet.of(ContentMappingType.TEXT);
        if (po.isSupportsToolCalling()) {
            contentTypes.add(ContentMappingType.TOOL_CALL);
        }
        if (po.isSupportsReasoning()) {
            contentTypes.add(ContentMappingType.REASONING);
        }
        if (po.isSupportsUsageMapping()) {
            contentTypes.add(ContentMappingType.USAGE);
        }
        if (po.isSupportsCacheTokenMapping()) {
            contentTypes.add(ContentMappingType.CACHE_TOKENS);
        }
        if (po.isSupportsStreaming()) {
            contentTypes.add(ContentMappingType.STREAM_EVENT);
        }
        return ConversionCapability.of(
                po.isSupportsStreaming(),
                po.isSupportsToolCalling(),
                po.isSupportsReasoning(),
                po.isSupportsUsageMapping(),
                po.isSupportsCacheTokenMapping(),
                contentTypes
        );
    }

    private MappingDocument toMapping(MappingDirection direction, String text) {
        String summary = text == null || text.isBlank() ? "Mapping metadata is not configured." : text.trim();
        if (summary.length() > 1000) {
            summary = summary.substring(0, 1000);
        }
        return MappingDocument.of(
                direction,
                direction == MappingDirection.REQUEST ? "Request mapping" : "Response mapping",
                summary,
                List.of(FieldMapping.of("payload", "payload", "MVP passthrough or documented mapping", MappingLossiness.NONE))
        );
    }

    private String toMappingText(MappingDocument mapping) {
        return mapping.summary();
    }
}
