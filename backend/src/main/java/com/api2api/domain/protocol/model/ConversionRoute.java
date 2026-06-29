package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;

/**
 * 已解析并校验可用的转换路线。
 */
public final class ConversionRoute {
    private final ProtocolConversionDefinition definition;
    private final ProtocolType sourceProtocol;
    private final ProtocolType targetProtocol;
    private final boolean passthrough;

    private ConversionRoute(ProtocolConversionDefinition definition, ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.sourceProtocol = Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        this.targetProtocol = Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        if (!definition.matches(sourceProtocol, targetProtocol)) {
            throw new ProtocolConversionException("route protocols must match conversion definition");
        }
        if (!definition.isEnabledForRouting()) {
            throw new ProtocolConversionException("route definition must be enabled for routing");
        }
        this.passthrough = definition.isPassthrough();
    }

    public static ConversionRoute of(ProtocolConversionDefinition definition, ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return new ConversionRoute(definition, sourceProtocol, targetProtocol);
    }

    public ProtocolConversionDefinition definition() {
        return definition;
    }

    public ProtocolType sourceProtocol() {
        return sourceProtocol;
    }

    public ProtocolType targetProtocol() {
        return targetProtocol;
    }

    public boolean passthrough() {
        return passthrough;
    }
}
