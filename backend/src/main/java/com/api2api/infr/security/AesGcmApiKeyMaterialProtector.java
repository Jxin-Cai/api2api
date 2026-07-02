package com.api2api.infr.security;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.ApiKeyMaterialProtector;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesGcmApiKeyMaterialProtector implements ApiKeyMaterialProtector {

    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyMaterialProtectionProperties properties;
    private final SecretKeySpec keySpec;

    public AesGcmApiKeyMaterialProtector(ApiKeyMaterialProtectionProperties properties) {
        this.properties = properties;
        if (properties.getEncryptionKey().isBlank()) {
            throw new IllegalStateException("API key material encryption key must not be blank");
        }
        this.keySpec = new SecretKeySpec(deriveAesKey(properties.getEncryptionKey()), "AES");
    }

    @Override
    public EncryptedApiKeyMaterial protect(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            throw new IllegalArgumentException("Plaintext API key must not be blank");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintextKey.getBytes(StandardCharsets.UTF_8));
            return EncryptedApiKeyMaterial.of(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(nonce),
                    properties.getKeyVersion()
            );
        } catch (GeneralSecurityException exception) {
            throw new BusinessException("API_KEY_MATERIAL_ENCRYPTION_FAILED", exception);
        }
    }

    @Override
    public String reveal(EncryptedApiKeyMaterial encryptedKeyMaterial) {
        if (encryptedKeyMaterial == null || !encryptedKeyMaterial.isAvailable()) {
            throw new BusinessException("API_KEY_MATERIAL_UNAVAILABLE");
        }
        try {
            byte[] nonce = Base64.getDecoder().decode(encryptedKeyMaterial.nonce());
            byte[] ciphertext = Base64.getDecoder().decode(encryptedKeyMaterial.ciphertext());
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new BusinessException("API_KEY_MATERIAL_DECRYPTION_FAILED", exception);
        }
    }

    private static byte[] deriveAesKey(String configuredKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(configuredKey.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
