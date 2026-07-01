package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Provider model-list endpoint path relative to the configured host.
 */
public final class ProviderModelsPath {

    public static final ProviderModelsPath DEFAULT = new ProviderModelsPath("/v1/models");

    private final String value;

    private ProviderModelsPath(String value) {
        String trimmed = Objects.requireNonNull(value, "Provider models path must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Provider models path must not be blank");
        }
        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException("Provider models path must start with /");
        }
        this.value = trimmed;
    }

    public static ProviderModelsPath of(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return new ProviderModelsPath(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderModelsPath that)) {
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
        return value;
    }
}
