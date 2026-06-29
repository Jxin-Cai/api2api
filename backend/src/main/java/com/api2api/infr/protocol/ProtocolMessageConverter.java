package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ProtocolConversionRequest;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;

/**
 * 基础设施协议消息转换策略 SPI。
 */
public interface ProtocolMessageConverter {

    ProtocolType sourceProtocol();

    ProtocolType targetProtocol();

    ProtocolConversionDirection direction();

    ConversionCapability capability();

    default boolean supports(ProtocolConversionRequest requirement) {
        return capability().satisfies(requirement);
    }

    ProtocolConversionResult convert(ProtocolPayload payload, ProtocolConversionRequest requirement);
}
