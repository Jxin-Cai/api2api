package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.time.Instant;
import java.util.Objects;

/**
 * 协议转换定义聚合根。
 */
public class ProtocolConversionDefinition {
    private final ProtocolConversionDefinitionId id;
    private final ProtocolType sourceProtocol;
    private final ProtocolType targetProtocol;
    private final ConversionKind kind;
    private ConversionStatus status;
    private ConversionCapability capability;
    private MappingDocument requestMapping;
    private MappingDocument responseMapping;
    private ConversionImplementationStatus implementationStatus;
    private final Instant createdAt;
    private Instant updatedAt;

    private ProtocolConversionDefinition(
            ProtocolConversionDefinitionId id,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ConversionCapability capability,
            MappingDocument requestMapping,
            MappingDocument responseMapping,
            ConversionImplementationStatus implementationStatus,
            Instant now
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sourceProtocol = Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        this.targetProtocol = Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        this.kind = ConversionKind.from(sourceProtocol, targetProtocol);
        this.capability = Objects.requireNonNull(capability, "capability must not be null");
        this.requestMapping = requireDirection(requestMapping, MappingDirection.REQUEST, "requestMapping");
        this.responseMapping = requireDirection(responseMapping, MappingDirection.RESPONSE, "responseMapping");
        this.implementationStatus = Objects.requireNonNull(implementationStatus, "implementationStatus must not be null");
        this.status = implementationStatus == ConversionImplementationStatus.NOT_IMPLEMENTED
                ? ConversionStatus.NOT_IMPLEMENTED
                : ConversionStatus.ENABLED;
        this.createdAt = requireNow(now);
        this.updatedAt = this.createdAt;
    }

    public static ProtocolConversionDefinition create(
            ProtocolConversionDefinitionId id,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ConversionCapability capability,
            MappingDocument requestMapping,
            MappingDocument responseMapping,
            ConversionImplementationStatus implementationStatus,
            Instant now
    ) {
        return new ProtocolConversionDefinition(
                id,
                sourceProtocol,
                targetProtocol,
                capability,
                requestMapping,
                responseMapping,
                implementationStatus,
                now
        );
    }

    public static ProtocolConversionDefinition rehydrate(
            ProtocolConversionDefinitionId id,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ConversionCapability capability,
            MappingDocument requestMapping,
            MappingDocument responseMapping,
            ConversionImplementationStatus implementationStatus,
            ConversionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        ProtocolConversionDefinition definition = new ProtocolConversionDefinition(
                id,
                sourceProtocol,
                targetProtocol,
                capability,
                requestMapping,
                responseMapping,
                implementationStatus,
                createdAt
        );
        definition.status = Objects.requireNonNull(status, "status must not be null");
        definition.updatedAt = requireNow(updatedAt);
        return definition;
    }

    public static ProtocolConversionDefinition create(
            ProtocolConversionId id,
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ConversionCapability capability,
            MappingMetadata requestMapping,
            MappingMetadata responseMapping,
            ConversionImplementationStatus implementationStatus,
            Instant now
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(requestMapping, "requestMapping must not be null");
        Objects.requireNonNull(responseMapping, "responseMapping must not be null");
        return create(
                id.toDefinitionId(),
                sourceProtocol,
                targetProtocol,
                capability,
                requestMapping.toDocument(),
                responseMapping.toDocument(),
                implementationStatus,
                now
        );
    }

    public void enable(Instant now) {
        if (status == ConversionStatus.ENABLED) {
            return;
        }
        if (status == ConversionStatus.NOT_IMPLEMENTED || implementationStatus == ConversionImplementationStatus.NOT_IMPLEMENTED) {
            throw new ProtocolConversionException("not implemented conversion definition cannot be enabled");
        }
        this.status = ConversionStatus.ENABLED;
        touch(now);
    }

    public void disable(Instant now) {
        if (status == ConversionStatus.DISABLED) {
            return;
        }
        this.status = ConversionStatus.DISABLED;
        touch(now);
    }

    public void markNotImplemented(Instant now) {
        this.status = ConversionStatus.NOT_IMPLEMENTED;
        this.implementationStatus = ConversionImplementationStatus.NOT_IMPLEMENTED;
        touch(now);
    }

    public void updateMetadata(
            ConversionCapability capability,
            MappingDocument requestMapping,
            MappingDocument responseMapping,
            ConversionImplementationStatus implementationStatus,
            Instant now
    ) {
        this.capability = Objects.requireNonNull(capability, "capability must not be null");
        this.requestMapping = requireDirection(requestMapping, MappingDirection.REQUEST, "requestMapping");
        this.responseMapping = requireDirection(responseMapping, MappingDirection.RESPONSE, "responseMapping");
        this.implementationStatus = Objects.requireNonNull(implementationStatus, "implementationStatus must not be null");
        if (implementationStatus == ConversionImplementationStatus.NOT_IMPLEMENTED) {
            this.status = ConversionStatus.NOT_IMPLEMENTED;
        }
        touch(now);
    }

    public void updateMetadata(
            ConversionCapability capability,
            MappingMetadata requestMapping,
            MappingMetadata responseMapping,
            ConversionImplementationStatus implementationStatus,
            Instant now
    ) {
        Objects.requireNonNull(requestMapping, "requestMapping must not be null");
        Objects.requireNonNull(responseMapping, "responseMapping must not be null");
        updateMetadata(capability, requestMapping.toDocument(), responseMapping.toDocument(), implementationStatus, now);
    }

    public boolean isPassthrough() {
        return kind == ConversionKind.PASSTHROUGH;
    }

    public boolean isTransform() {
        return kind == ConversionKind.TRANSFORM;
    }

    public boolean isEnabledForRouting() {
        return status == ConversionStatus.ENABLED && implementationStatus.isRoutable();
    }

    public boolean supportsStreaming() {
        return capability.supportsStreaming();
    }

    public boolean supportsToolCalling() {
        return capability.supportsToolCalling();
    }

    public void assertUsableFor(ProtocolConversionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (!isEnabledForRouting()) {
            throw new ProtocolConversionException("protocol conversion definition is not enabled for routing");
        }
        if (!capability.satisfies(request)) {
            throw new ProtocolConversionException("protocol conversion capability does not satisfy request requirement");
        }
    }

    public void assertUsableFor(ConversionRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement must not be null");
        assertUsableFor(requirement.toProtocolConversionRequest());
    }

    public boolean matches(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return this.sourceProtocol == Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null")
                && this.targetProtocol == Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
    }

    private void touch(Instant now) {
        this.updatedAt = requireNow(now);
    }

    private static Instant requireNow(Instant now) {
        return Objects.requireNonNull(now, "now must not be null");
    }

    private static MappingDocument requireDirection(MappingDocument mapping, MappingDirection expected, String fieldName) {
        Objects.requireNonNull(mapping, fieldName + " must not be null");
        if (mapping.direction() != expected) {
            throw new ProtocolConversionException(fieldName + " direction must be " + expected);
        }
        return mapping;
    }

    public ProtocolConversionDefinitionId id() {
        return id;
    }

    public ProtocolType sourceProtocol() {
        return sourceProtocol;
    }

    public ProtocolType targetProtocol() {
        return targetProtocol;
    }

    public ConversionKind kind() {
        return kind;
    }

    public ConversionStatus status() {
        return status;
    }

    public ConversionCapability capability() {
        return capability;
    }

    public MappingDocument requestMapping() {
        return requestMapping;
    }

    public MappingDocument responseMapping() {
        return responseMapping;
    }

    public ConversionImplementationStatus implementationStatus() {
        return implementationStatus;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
