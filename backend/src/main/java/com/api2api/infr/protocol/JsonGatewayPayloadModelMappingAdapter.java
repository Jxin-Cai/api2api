package com.api2api.infr.protocol;

import com.api2api.application.gateway.GatewayPayloadModelMappingPort;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Rewrites top-level model fields for gateway protocol payloads after route model mapping is selected.
 */
@Component
public class JsonGatewayPayloadModelMappingAdapter implements GatewayPayloadModelMappingPort {

    private final ObjectMapper objectMapper;

    public JsonGatewayPayloadModelMappingAdapter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper must not be null");
    }

    @Override
    public String rewriteModel(ProtocolType protocol, String body, ModelName modelName) {
        Objects.requireNonNull(protocol, "Protocol type must not be null");
        Objects.requireNonNull(modelName, "Model name must not be null");
        if (body == null || body.isBlank()) {
            throw new ProtocolConversionException("MODEL_MAPPING_BODY_EMPTY");
        }
        // Bedrock Converse selects the model exclusively through the URI path.
        // A top-level model field is outside the AWS request schema.
        if (protocol == ProtocolType.AWS_BEDROCK_CONVERSE
                || protocol == ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES) {
            return body;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.isObject()) {
                throw new ProtocolConversionException("MODEL_MAPPING_REQUIRES_JSON_OBJECT");
            }
            ObjectNode object = (ObjectNode) root;
            object.put("model", modelName.value());
            return objectMapper.writeValueAsString(object);
        } catch (ProtocolConversionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProtocolConversionException("MODEL_MAPPING_FAILED");
        }
    }
}
