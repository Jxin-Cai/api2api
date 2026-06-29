package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Provider upstream endpoint value object.
 */
public final class ProviderEndpoint {

    private final String value;

    private ProviderEndpoint(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Provider endpoint must not be null");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("Provider endpoint must start with http:// or https://");
        }
        this.value = removeTrailingSlashes(trimmed);
    }

    public static ProviderEndpoint of(String value) {
        return new ProviderEndpoint(value);
    }

    public ProviderEndpoint resolvePath(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Endpoint path must start with /");
        }
        return new ProviderEndpoint(value + path);
    }

    public String value() {
        return value;
    }

    private static String removeTrailingSlashes(String value) {
        String normalized = value;
        while (normalized.length() > "https://".length() && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderEndpoint that)) {
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
