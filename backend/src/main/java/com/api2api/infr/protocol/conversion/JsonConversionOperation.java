package com.api2api.infr.protocol.conversion;

import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface JsonConversionOperation {

    JsonNode convert(JsonNode source, ProtocolConversionRequest requirement);
}
