package com.api2api.infr.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api2api.security.api-key-material")
public class ApiKeyMaterialProtectionProperties {

    private String encryptionKey = "";
    private String previousEncryptionKeys = "";
    private int keyVersion = 1;

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey.trim();
    }

    public String getPreviousEncryptionKeys() {
        return previousEncryptionKeys;
    }

    public void setPreviousEncryptionKeys(String previousEncryptionKeys) {
        this.previousEncryptionKeys = previousEncryptionKeys == null ? "" : previousEncryptionKeys.trim();
    }

    public List<String> previousEncryptionKeyList() {
        if (previousEncryptionKeys.isBlank()) {
            return List.of();
        }
        return Arrays.stream(previousEncryptionKeys.split("[,;]"))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .toList();
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(int keyVersion) {
        if (keyVersion <= 0) {
            throw new IllegalArgumentException("API key material key version must be positive");
        }
        this.keyVersion = keyVersion;
    }
}
