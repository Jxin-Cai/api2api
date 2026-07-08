package com.api2api.application.gateway;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;

/**
 * Application port for rewriting protocol payload model names according to route model mappings.
 */
public interface GatewayPayloadModelMappingPort {

    String rewriteModel(ProtocolType protocol, String body, ModelName modelName);
}
