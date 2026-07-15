package com.api2api.infr.protocol.conversion;

import com.api2api.domain.protocol.model.FieldMapping;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface ExecutableConversionRule {

    JsonNode execute(ConversionRuleContext context);

    List<FieldMapping> toFieldMappings();
}
