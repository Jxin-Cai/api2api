package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionRoute;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.domain.protocol.service.DefaultProtocolConversionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Spring adapter that connects the domain conversion service with infrastructure converters and usage extractors.
 */
@Service
public class DefaultProtocolConversionServiceAdapter extends DefaultProtocolConversionService {

    private final List<ProtocolMessageConverter> converters;
    private final List<UnifiedUsageExtractor> usageExtractors;
    private final ObjectMapper objectMapper;

    public DefaultProtocolConversionServiceAdapter(
            List<ProtocolMessageConverter> converters,
            List<UnifiedUsageExtractor> usageExtractors,
            ObjectMapper objectMapper
    ) {
        this.converters = List.copyOf(Objects.requireNonNull(converters, "Protocol message converters must not be null"));
        this.usageExtractors = List.copyOf(Objects.requireNonNull(usageExtractors, "Usage extractors must not be null"));
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public ProtocolConversionResult convertRequest(
            ProtocolPayload payload,
            ProtocolType targetProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        payload = ClaudeRequestSanitizer.sanitize(objectMapper, payload, targetProtocol);
        ConversionRoute route = resolve(payload.protocol(), targetProtocol, requirement, definitions);
        if (route.passthrough()) {
            return ProtocolConversionResult.passthrough(payload);
        }
        return findConverter(payload.protocol(), targetProtocol, ProtocolConversionDirection.REQUEST, requirement)
                .convert(payload, requirement);
    }

    @Override
    public ProtocolConversionResult convertResponse(
            ProtocolPayload payload,
            ProtocolType originalClientProtocol,
            ProtocolConversionRequest requirement,
            List<ProtocolConversionDefinition> definitions
    ) {
        ConversionRoute route = resolve(payload.protocol(), originalClientProtocol, requirement, definitions);
        if (route.passthrough()) {
            return ProtocolConversionResult.of(
                    payload.protocol(),
                    originalClientProtocol,
                    payload.body(),
                    true,
                    extractUsage(payload)
            );
        }
        return findConverter(payload.protocol(), originalClientProtocol, ProtocolConversionDirection.RESPONSE, requirement)
                .convert(payload, requirement);
    }

    private ProtocolMessageConverter findConverter(
            ProtocolType sourceProtocol,
            ProtocolType targetProtocol,
            ProtocolConversionDirection direction,
            ProtocolConversionRequest requirement
    ) {
        return converters.stream()
                .filter(converter -> converter.sourceProtocol() == sourceProtocol)
                .filter(converter -> converter.targetProtocol() == targetProtocol)
                .filter(converter -> converter.direction() == direction)
                .filter(converter -> converter.supports(requirement))
                .findFirst()
                .orElseThrow(() -> new ProtocolConversionException("PROTOCOL_CONVERSION_NOT_IMPLEMENTED"));
    }

    private UnifiedTokenUsage extractUsage(ProtocolPayload payload) {
        if (payload.streaming()) {
            return UnifiedTokenUsage.unknown();
        }
        return usageExtractors.stream()
                .filter(extractor -> extractor.protocol() == payload.protocol())
                .findFirst()
                .map(extractor -> extractor.extract(readJson(payload.body())))
                .orElseGet(UnifiedTokenUsage::unknown);
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }
}
