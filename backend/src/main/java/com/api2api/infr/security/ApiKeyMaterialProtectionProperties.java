package com.api2api.infr.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api2api.security.api-key-material")
public class ApiKeyMaterialProtectionProperties {

    private String encryptionKey = "";
    private int keyVersion = 1;

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey == null ? "" : encryptionKey.trim();
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
