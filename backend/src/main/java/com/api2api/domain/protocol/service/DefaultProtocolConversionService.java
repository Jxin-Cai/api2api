package com.api2api.domain.protocol.service;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionRoute;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import java.util.List;
import java.util.Objects;

public class DefaultProtocolConversionService implements ProtocolConversionService {

    @Override
    public ConversionRoute resolve(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        Objects.requireNonNull(sourceProtocol, "Source protocol must not be null");
        Objects.requireNonNull(targetProtocol, "Target protocol must not be null");
        Objects.requireNonNull(requirement, "Protocol conversion request must not be null");
        Objects.requireNonNull(definitions, "Protocol conversion definitions must not be null");
        return definitions.stream()
                .filter(Objects::nonNull)
                .filter(definition -> definition.matches(sourceProtocol, targetProtocol))
                .filter(ProtocolConversionDefinition::isEnabledForRouting)
                .filter(definition -> definition.capability().satisfies(requirement))
                .findFirst()
                .map(definition -> ConversionRoute.of(definition, sourceProtocol, targetProtocol))
                .orElseThrow(() -> new ProtocolConversionException("No usable protocol conversion definition"));
    }

    @Override
    public ProtocolConversionResult convertRequest(
            ProtocolPayload payload,
            ProtocolType targetProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        Objects.requireNonNull(payload, "Protocol payload must not be null");
        ConversionRoute route = resolve(payload.protocol(), targetProtocol, requirement, definitions);
        if (route.passthrough()) {
            return ProtocolConversionResult.passthrough(payload);
        }
        return ProtocolConversionResult.of(payload.protocol(), targetProtocol, payload.body(), false, null);
    }

    @Override
    public ProtocolConversionResult convertResponse(
            ProtocolPayload payload,
            ProtocolType originalClientProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        Objects.requireNonNull(payload, "Protocol payload must not be null");
        ConversionRoute route = resolve(payload.protocol(), originalClientProtocol, requirement, definitions);
        if (route.passthrough()) {
            return ProtocolConversionResult.of(payload.protocol(), originalClientProtocol, payload.body(), true, UnifiedTokenUsage.unknown());
        }
        return ProtocolConversionResult.of(payload.protocol(), originalClientProtocol, payload.body(), false, UnifiedTokenUsage.unknown());
    }
}
