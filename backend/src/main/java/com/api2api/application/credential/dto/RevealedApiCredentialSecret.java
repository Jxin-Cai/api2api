package com.api2api.application.credential.dto;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiKeyPreview;
import java.util.Objects;

public final class RevealedApiCredentialSecret {

    private final ApiCredentialId apiCredentialId;
    private final ApiKeyPreview keyPreview;
    private final String plaintextApiKey;

    private RevealedApiCredentialSecret(ApiCredentialId apiCredentialId, ApiKeyPreview keyPreview, String plaintextApiKey) {
        this.apiCredentialId = Objects.requireNonNull(apiCredentialId, "API credential id must not be null");
        this.keyPreview = Objects.requireNonNull(keyPreview, "API key preview must not be null");
        if (plaintextApiKey == null || plaintextApiKey.isBlank()) {
            throw new IllegalArgumentException("Plaintext API key must not be blank");
        }
        this.plaintextApiKey = plaintextApiKey;
    }

    public static RevealedApiCredentialSecret of(ApiCredentialId apiCredentialId, ApiKeyPreview keyPreview, String plaintextApiKey) {
        return new RevealedApiCredentialSecret(apiCredentialId, keyPreview, plaintextApiKey);
    }

    public ApiCredentialId apiCredentialId() {
        return apiCredentialId;
    }

    public ApiKeyPreview keyPreview() {
        return keyPreview;
    }

    public String plaintextApiKey() {
        return plaintextApiKey;
    }

    public ApiCredentialId getApiCredentialId() {
        return apiCredentialId;
    }

    public ApiKeyPreview getKeyPreview() {
        return keyPreview;
    }

    public String getPlaintextApiKey() {
        return plaintextApiKey;
    }
}
