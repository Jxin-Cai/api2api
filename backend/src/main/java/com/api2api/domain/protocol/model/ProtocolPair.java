package com.api2api.domain.protocol.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;

/**
 * 源协议与目标协议方向对。
 */
public final class ProtocolPair {
    private final ProtocolType sourceProtocol;
    private final ProtocolType targetProtocol;

    private ProtocolPair(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        this.sourceProtocol = Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null");
        this.targetProtocol = Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
    }

    public static ProtocolPair of(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return new ProtocolPair(sourceProtocol, targetProtocol);
    }

    public ConversionKind kind() {
        return ConversionKind.from(sourceProtocol, targetProtocol);
    }

    public boolean isPassthrough() {
        return sourceProtocol == targetProtocol;
    }

    public boolean matches(ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return this.sourceProtocol == Objects.requireNonNull(sourceProtocol, "sourceProtocol must not be null")
                && this.targetProtocol == Objects.requireNonNull(targetProtocol, "targetProtocol must not be null");
    }

    public ProtocolType sourceProtocol() {
        return sourceProtocol;
    }

    public ProtocolType targetProtocol() {
        return targetProtocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolPair that)) {
            return false;
        }
        return sourceProtocol == that.sourceProtocol && targetProtocol == that.targetProtocol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceProtocol, targetProtocol);
    }
}
