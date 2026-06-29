package com.api2api.infr.client.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security configuration for resolving provider key references to plaintext secrets.
 */
@ConfigurationProperties(prefix = "api2api.provider-secrets")
public class ProviderSecretProperties {

    /**
     * Default profile entries used when no profile-specific value exists.
     */
    private Map<String, String> keys = new LinkedHashMap<>();

    /**
     * Profile-isolated entries. First key is Spring profile, second key is provider keyRef.
     */
    private Map<String, Map<String, String>> profiles = new LinkedHashMap<>();

    public Map<String, String> getKeys() {
        return Map.copyOf(keys);
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = copyStringMap(keys);
    }

    public Map<String, Map<String, String>> getProfiles() {
        Map<String, Map<String, String>> copied = new LinkedHashMap<>();
        profiles.forEach((profile, values) -> copied.put(profile, Map.copyOf(values)));
        return Map.copyOf(copied);
    }

    public void setProfiles(Map<String, Map<String, String>> profiles) {
        this.profiles = new LinkedHashMap<>();
        if (profiles == null) {
            return;
        }
        profiles.forEach((profile, values) -> this.profiles.put(profile, copyStringMap(values)));
    }

    public String findByRef(String profile, String keyRef) {
        Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        if (profile != null && !profile.isBlank()) {
            Map<String, String> profileKeys = profiles.get(profile.trim());
            if (profileKeys != null) {
                String profileSecret = profileKeys.get(keyRef);
                if (hasText(profileSecret)) {
                    return profileSecret;
                }
            }
        }
        return keys.get(keyRef);
    }

    private static Map<String, String> copyStringMap(Map<String, String> source) {
        Map<String, String> copied = new LinkedHashMap<>();
        if (source == null) {
            return copied;
        }
        source.forEach((key, value) -> {
            if (key != null) {
                copied.put(key.trim(), value);
            }
        });
        return copied;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
