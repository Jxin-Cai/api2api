package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;

/**
 * 协议转换类型：同协议透传或异协议结构转换。
 */
public enum ConversionKind {
    PASSTHROUGH,
    TRANSFORM;

    public static ConversionKind from(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
        return sourceProtocol == targetProtocol ? PASSTHROUGH : TRANSFORM;
    }

    public boolean matches(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return this == from(sourceProtocol, targetProtocol);
    }
}
