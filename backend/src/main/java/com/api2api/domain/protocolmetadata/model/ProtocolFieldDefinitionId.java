package com.api2api.domain.protocolmetadata.model;

import java.util.Objects;

public final class ProtocolFieldDefinitionId {
    private final Long value;

    private ProtocolFieldDefinitionId(Long value) {
        if (value == null || value <= 0) {
            throw new ProtocolMetadataException("protocol field definition id must be greater than 0");
        }
        this.value = value;
    }

    public static ProtocolFieldDefinitionId of(Long value) {
        return new ProtocolFieldDefinitionId(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolFieldDefinitionId that)) {
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
