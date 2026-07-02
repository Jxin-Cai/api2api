package com.api2api.ohs.http.credential;

import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ApiKeyPreview;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Helper for generating plaintext API keys, hashes, and previews.
 * Plaintext keys are only returned once at creation and must never be logged.
 */
@Component
public class ApiKeyMaterialHelper {

    private static final int KEY_ENTROPY_BYTES = 32;
    private static final String KEY_PREFIX = "api2api_";
    private static final int PREVIEW_VISIBLE_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        byte[] randomBytes = new byte[KEY_ENTROPY_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String encodedRandom = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return KEY_PREFIX + encodedRandom;
    }

    public ApiKeyHash hashKey(String plaintextKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plaintextKey.getBytes(StandardCharsets.UTF_8));
            String hashHex = bytesToHex(hashBytes);
            return ApiKeyHash.of(hashHex);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }

    private ApiKeyPreview createPreview(String plaintextKey) {
        if (plaintextKey.length() <= PREVIEW_VISIBLE_LENGTH) {
            return ApiKeyPreview.of(plaintextKey);
        }
        String visiblePart = plaintextKey.substring(0, PREVIEW_VISIBLE_LENGTH);
        return ApiKeyPreview.of(visiblePart + "***");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
