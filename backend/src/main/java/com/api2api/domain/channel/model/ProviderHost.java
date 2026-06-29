package com.api2api.domain.channel.model;

import java.util.Objects;

/**
 * Provider upstream host value object.
 */
public final class ProviderHost {

    private final String value;

    private ProviderHost(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Provider host must not be null");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("Provider host must start with http:// or https://");
        }
        this.value = removeTrailingSlashes(trimmed);
    }

    public static ProviderHost of(String value) {
        return new ProviderHost(value);
    }

    public ProviderHost resolvePath(String path) {
        if (path == null || !path.startsWith("/")) {
            throw new IllegalArgumentException("Provider host path must start with /");
        }
        return new ProviderHost(value + path);
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
        if (!(o instanceof ProviderHost that)) {
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
