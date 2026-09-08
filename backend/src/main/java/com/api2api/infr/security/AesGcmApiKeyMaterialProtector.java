package com.api2api.infr.security;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.ApiKeyMaterialProtector;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AesGcmApiKeyMaterialProtector implements ApiKeyMaterialProtector {

    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Historical default used before API2API_API_KEY_ENCRYPTION_KEY became required.
     * Kept only as a decrypt fallback so existing keys remain copyable.
     */
    static final String LEGACY_DEFAULT_ENCRYPTION_KEY = "api2api-development-api-key-material-secret";

    private final ApiKeyMaterialProtectionProperties properties;
    private final SecretKeySpec keySpec;
    private final List<SecretKeySpec> decryptKeySpecs;

    public AesGcmApiKeyMaterialProtector(ApiKeyMaterialProtectionProperties properties) {
        this.properties = properties;
        if (properties.getEncryptionKey().isBlank()) {
            throw new IllegalStateException(
                    "API key material encryption key must not be blank. "
                            + "Set the API2API_API_KEY_ENCRYPTION_KEY environment variable.");
        }
        if (LEGACY_DEFAULT_ENCRYPTION_KEY.equals(properties.getEncryptionKey())) {
            throw new IllegalStateException(
                    "Insecure default encryption key detected. "
                            + "Set API2API_API_KEY_ENCRYPTION_KEY to a secure random value.");
        }
        this.keySpec = new SecretKeySpec(deriveAesKey(properties.getEncryptionKey()), "AES");
        this.decryptKeySpecs = buildDecryptKeySpecs(properties);
    }

    @Override
    public EncryptedApiKeyMaterial protect(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            throw new IllegalArgumentException("Plaintext API key must not be blank");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonce);
        try {
            byte[] ciphertext = encrypt(plaintextKey.getBytes(StandardCharsets.UTF_8), nonce, keySpec);
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
        Exception lastFailure = null;
        for (int index = 0; index < decryptKeySpecs.size(); index += 1) {
            SecretKeySpec candidate = decryptKeySpecs.get(index);
            try {
                String plaintext = decrypt(encryptedKeyMaterial, candidate);
                if (index > 0) {
                    log.warn("Decrypted stored API key material with a fallback encryption key");
                }
                return plaintext;
            } catch (IllegalArgumentException | GeneralSecurityException exception) {
                lastFailure = exception;
            }
        }
        String ciphertext = encryptedKeyMaterial.ciphertext();
        if (looksLikePlainApiKey(ciphertext)) {
            return ciphertext;
        }
        throw new BusinessException("API_KEY_MATERIAL_DECRYPTION_FAILED", lastFailure);
    }

    private static List<SecretKeySpec> buildDecryptKeySpecs(ApiKeyMaterialProtectionProperties properties) {
        Set<String> configuredKeys = new LinkedHashSet<>();
        configuredKeys.add(properties.getEncryptionKey());
        configuredKeys.addAll(properties.previousEncryptionKeyList());
        configuredKeys.add(LEGACY_DEFAULT_ENCRYPTION_KEY);

        List<SecretKeySpec> keySpecs = new ArrayList<>();
        for (String configuredKey : configuredKeys) {
            keySpecs.add(new SecretKeySpec(deriveAesKey(configuredKey), "AES"));
        }
        return List.copyOf(keySpecs);
    }

    private static byte[] encrypt(byte[] plaintext, byte[] nonce, SecretKeySpec keySpec) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        return cipher.doFinal(plaintext);
    }

    private static String decrypt(
            EncryptedApiKeyMaterial encryptedKeyMaterial,
            SecretKeySpec keySpec
    ) throws GeneralSecurityException {
        byte[] nonce = Base64.getDecoder().decode(encryptedKeyMaterial.nonce());
        byte[] ciphertext = Base64.getDecoder().decode(encryptedKeyMaterial.ciphertext());
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private static boolean looksLikePlainApiKey(String value) {
        return value != null && value.startsWith("sk-") && value.length() >= 10 && value.indexOf(' ') < 0;
    }

    static EncryptedApiKeyMaterial encryptWithConfiguredKey(String plaintextKey, String configuredKey, int version) {
        byte[] nonce = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonce);
        try {
            byte[] ciphertext = encrypt(
                    plaintextKey.getBytes(StandardCharsets.UTF_8),
                    nonce,
                    new SecretKeySpec(deriveAesKey(configuredKey), "AES")
            );
            return EncryptedApiKeyMaterial.of(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(nonce),
                    version
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt API key material for test", exception);
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
