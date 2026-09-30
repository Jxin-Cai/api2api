package com.api2api.ohs.http;

import com.api2api.domain.credential.model.ApiKeyHash;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Shared fingerprint format for key creation and gateway authentication. */
public final class ApiKeyHasher {

    private ApiKeyHasher() {
    }

    public static ApiKeyHash hash(String plaintextKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintextKey.getBytes(StandardCharsets.UTF_8));
            return ApiKeyHash.of(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
