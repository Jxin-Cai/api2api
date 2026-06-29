package com.api2api.domain.protocol.model;

import java.util.Objects;

/**
 * 协议转换定义标识。
 */
public final class ProtocolConversionDefinitionId {
    private final Long value;

    private ProtocolConversionDefinitionId(Long value) {
        if (value == null || value <= 0) {
            throw new ProtocolConversionException("protocol conversion definition id must be greater than 0");
        }
        this.value = value;
    }

    public static ProtocolConversionDefinitionId of(Long value) {
        return new ProtocolConversionDefinitionId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolConversionDefinitionId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
