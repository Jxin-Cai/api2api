package com.api2api.domain.credential.model;

import java.util.Objects;

/**
 * Encrypted API key material used for controlled secret reveal.
 */
public final class EncryptedApiKeyMaterial {

    private final String ciphertext;
    private final String nonce;
    private final int version;

    private EncryptedApiKeyMaterial(String ciphertext, String nonce, int version) {
        this.ciphertext = normalize(ciphertext);
        this.nonce = normalize(nonce);
        if ((this.ciphertext == null) != (this.nonce == null)) {
            throw new IllegalArgumentException("Encrypted API key material ciphertext and nonce must be present together");
        }
        if (version <= 0) {
            throw new IllegalArgumentException("Encrypted API key material version must be positive");
        }
        this.version = version;
    }

    public static EncryptedApiKeyMaterial of(String ciphertext, String nonce, int version) {
        return new EncryptedApiKeyMaterial(ciphertext, nonce, version);
    }

    public static EncryptedApiKeyMaterial unavailable() {
        return new EncryptedApiKeyMaterial(null, null, 1);
    }

    public boolean isAvailable() {
        return ciphertext != null && nonce != null;
    }

    public String ciphertext() {
        return ciphertext;
    }

    public String nonce() {
        return nonce;
    }

    public int version() {
        return version;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getNonce() {
        return nonce;
    }

    public int getVersion() {
        return version;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EncryptedApiKeyMaterial that)) {
            return false;
        }
        return version == that.version
                && Objects.equals(ciphertext, that.ciphertext)
                && Objects.equals(nonce, that.nonce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ciphertext, nonce, version);
    }
}
