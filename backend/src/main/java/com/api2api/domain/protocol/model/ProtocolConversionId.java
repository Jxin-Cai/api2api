package com.api2api.domain.protocol.model;

import java.util.Objects;

/**
 * 设计文档中的转换定义标识别名，语义等同于 ProtocolConversionDefinitionId。
 */
public final class ProtocolConversionId {
    private final Long value;

    private ProtocolConversionId(Long value) {
        if (value == null || value <= 0) {
            throw new ProtocolConversionException("protocol conversion id must be greater than 0");
        }
        this.value = value;
    }

    public static ProtocolConversionId of(Long value) {
        return new ProtocolConversionId(value);
    }

    public ProtocolConversionDefinitionId toDefinitionId() {
        return ProtocolConversionDefinitionId.of(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolConversionId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
