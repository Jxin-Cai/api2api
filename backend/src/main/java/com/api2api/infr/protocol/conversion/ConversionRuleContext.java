package com.api2api.infr.protocol.conversion;

import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public record ConversionRuleContext(JsonNode source, ProtocolConversionRequest requirement) {

    public ConversionRuleContext {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(requirement, "requirement must not be null");
    }
}
