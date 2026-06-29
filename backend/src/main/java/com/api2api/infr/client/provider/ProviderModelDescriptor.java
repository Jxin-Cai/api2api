package com.api2api.infr.client.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Raw upstream model descriptor returned by provider /models endpoints.
 */
public final class ProviderModelDescriptor {

    private final String id;
    private final String name;
    private final Map<String, Object> metadata;

    private ProviderModelDescriptor(String id, String name, Map<String, Object> metadata) {
        this.id = normalizeRequiredText(id, "Provider model id must not be blank");
        this.name = hasText(name) ? name.trim() : this.id;
        this.metadata = copyMetadata(metadata);
    }

    public static ProviderModelDescriptor of(String id, String name, Map<String, Object> metadata) {
        return new ProviderModelDescriptor(id, name, metadata);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Map<String, Object> metadata() {
        return Map.copyOf(metadata);
    }

    private static String normalizeRequiredText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, Object> copyMetadata(Map<String, Object> source) {
        Map<String, Object> copied = new LinkedHashMap<>();
        if (source == null) {
            return copied;
        }
        source.forEach((key, value) -> {
            if (key != null) {
                copied.put(key, value);
            }
        });
        return copied;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderModelDescriptor that)) {
            return false;
        }
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, metadata);
    }
}
