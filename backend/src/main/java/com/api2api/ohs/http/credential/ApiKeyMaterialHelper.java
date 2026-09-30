package com.api2api.ohs.http.credential;

import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ApiKeyPreview;
import com.api2api.ohs.http.ApiKeyHasher;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Helper for generating plaintext API keys, hashes, and previews.
 * Plaintext keys are only returned once at creation and must never be logged.
 */
@Component
public class ApiKeyMaterialHelper {

    private static final String KEY_PREFIX = "sk-";
    private static final int PREVIEW_VISIBLE_LENGTH = 8;

    /**
     * Holds generated key materials together to ensure consistency.
     */
    public static final class ApiKeyMaterial {
        private final String plaintextKey;
        private final ApiKeyHash keyHash;
        private final ApiKeyPreview keyPreview;

        private ApiKeyMaterial(String plaintextKey, ApiKeyHash keyHash, ApiKeyPreview keyPreview) {
            this.plaintextKey = Objects.requireNonNull(plaintextKey, "Plaintext key must not be null");
            this.keyHash = Objects.requireNonNull(keyHash, "Key hash must not be null");
            this.keyPreview = Objects.requireNonNull(keyPreview, "Key preview must not be null");
        }

        public String getPlaintextKey() {
            return plaintextKey;
        }

        public ApiKeyHash getKeyHash() {
            return keyHash;
        }

        public ApiKeyPreview getKeyPreview() {
            return keyPreview;
        }
    }

    /**
     * Generates a new API key material bundle containing plaintext key, hash, and preview.
     * The plaintext key is only returned once and must be delivered to the user immediately.
     */
    public ApiKeyMaterial generateApiKeyMaterial() {
        String plaintextKey = generatePlaintextKey();
        ApiKeyHash keyHash = hashKey(plaintextKey);
        ApiKeyPreview keyPreview = createPreview(plaintextKey);
        return new ApiKeyMaterial(plaintextKey, keyHash, keyPreview);
    }

    private String generatePlaintextKey() {
        return KEY_PREFIX + UUID.randomUUID();
    }

    public ApiKeyHash hashKey(String plaintextKey) {
        return ApiKeyHasher.hash(plaintextKey);
    }

    private ApiKeyPreview createPreview(String plaintextKey) {
        if (plaintextKey.length() <= PREVIEW_VISIBLE_LENGTH) {
            return ApiKeyPreview.of(plaintextKey);
        }
        String visiblePart = plaintextKey.substring(0, PREVIEW_VISIBLE_LENGTH);
        return ApiKeyPreview.of(visiblePart + "***");
    }

}
