package com.api2api.domain.protocol.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 协议转换能力集合。
 */
public final class ConversionCapability {
    private final boolean supportsStreaming;
    private final boolean supportsToolCalling;
    private final boolean supportsReasoning;
    private final boolean supportsUsageMapping;
    private final boolean supportsCacheTokenMapping;
    private final Set<ContentMappingType> supportedContentTypes;

    private ConversionCapability(
            boolean supportsStreaming,
            boolean supportsToolCalling,
            boolean supportsReasoning,
            boolean supportsUsageMapping,
            boolean supportsCacheTokenMapping,
            Set<ContentMappingType> supportedContentTypes
    ) {
        Objects.requireNonNull(supportedContentTypes, "supportedContentTypes must not be null");
        if (supportsToolCalling && !supportedContentTypes.contains(ContentMappingType.TOOL_CALL)) {
            throw new ProtocolConversionException("tool calling capability requires TOOL_CALL content mapping type");
        }
        this.supportsStreaming = supportsStreaming;
        this.supportsToolCalling = supportsToolCalling;
        this.supportsReasoning = supportsReasoning;
        this.supportsUsageMapping = supportsUsageMapping;
        this.supportsCacheTokenMapping = supportsCacheTokenMapping;
        this.supportedContentTypes = supportedContentTypes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(supportedContentTypes));
    }

    public static ConversionCapability of(
            boolean supportsStreaming,
            boolean supportsToolCalling,
            boolean supportsReasoning,
            boolean supportsUsageMapping,
            boolean supportsCacheTokenMapping,
            Set<ContentMappingType> supportedContentTypes
    ) {
        return new ConversionCapability(
                supportsStreaming,
                supportsToolCalling,
                supportsReasoning,
                supportsUsageMapping,
                supportsCacheTokenMapping,
                supportedContentTypes
        );
    }

    public boolean satisfies(ProtocolConversionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return (!request.streaming() || supportsStreaming)
                && (!request.toolCallingRequired() || supportsToolCalling)
                && (!request.reasoningRequired() || supportsReasoning);
    }

    public boolean satisfies(ConversionRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement must not be null");
        return satisfies(requirement.toProtocolConversionRequest());
    }

    public boolean supportsStreaming() {
        return supportsStreaming;
    }

    public boolean supportsToolCalling() {
        return supportsToolCalling;
    }

    public boolean supportsReasoning() {
        return supportsReasoning;
    }

    public boolean supportsUsageMapping() {
        return supportsUsageMapping;
    }

    public boolean supportsCacheTokenMapping() {
        return supportsCacheTokenMapping;
    }

    public Set<ContentMappingType> supportedContentTypes() {
        return supportedContentTypes;
    }
}
