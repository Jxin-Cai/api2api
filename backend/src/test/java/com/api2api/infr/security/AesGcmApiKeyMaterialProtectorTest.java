package com.api2api.infr.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.BusinessException;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import org.junit.jupiter.api.Test;

class AesGcmApiKeyMaterialProtectorTest {

    @Test
    void test_returns_plaintext_when_material_was_encrypted_with_current_key() {
        AesGcmApiKeyMaterialProtector protector = protector("current-secure-api-key-material-secret", "");
        String plaintext = "sk-11111111-2222-4333-8333-444444444444";

        EncryptedApiKeyMaterial material = protector.protect(plaintext);

        assertThat(protector.reveal(material)).isEqualTo(plaintext);
    }

    @Test
    void test_returns_plaintext_when_material_was_encrypted_with_legacy_default_key() {
        AesGcmApiKeyMaterialProtector protector = protector("current-secure-api-key-material-secret", "");
        String plaintext = "sk-aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
        EncryptedApiKeyMaterial material = AesGcmApiKeyMaterialProtector.encryptWithConfiguredKey(
                plaintext,
                AesGcmApiKeyMaterialProtector.LEGACY_DEFAULT_ENCRYPTION_KEY,
                1
        );

        assertThat(protector.reveal(material)).isEqualTo(plaintext);
    }

    @Test
    void test_returns_plaintext_when_material_was_encrypted_with_previous_key() {
        AesGcmApiKeyMaterialProtector protector = protector(
                "current-secure-api-key-material-secret",
                "previous-secure-api-key-material-secret"
        );
        String plaintext = "sk-ffffffff-1111-4222-8333-444444444444";
        EncryptedApiKeyMaterial material = AesGcmApiKeyMaterialProtector.encryptWithConfiguredKey(
                plaintext,
                "previous-secure-api-key-material-secret",
                1
        );

        assertThat(protector.reveal(material)).isEqualTo(plaintext);
    }

    @Test
    void test_returns_stored_plaintext_when_ciphertext_is_raw_api_key() {
        AesGcmApiKeyMaterialProtector protector = protector("current-secure-api-key-material-secret", "");
        String plaintext = "sk-plain-key-stored-without-encryption";

        assertThat(protector.reveal(EncryptedApiKeyMaterial.of(plaintext, "dGVzdG5vbmNlMTI=", 1)))
                .isEqualTo(plaintext);
    }

    @Test
    void test_throws_decryption_failed_when_no_key_matches() {
        AesGcmApiKeyMaterialProtector protector = protector("current-secure-api-key-material-secret", "");
        EncryptedApiKeyMaterial material = AesGcmApiKeyMaterialProtector.encryptWithConfiguredKey(
                "sk-99999999-aaaa-4bbb-8ccc-dddddddddddd",
                "unknown-secure-api-key-material-secret",
                1
        );

        assertThatThrownBy(() -> protector.reveal(material))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("API_KEY_MATERIAL_DECRYPTION_FAILED");
    }

    private static AesGcmApiKeyMaterialProtector protector(String currentKey, String previousKeys) {
        ApiKeyMaterialProtectionProperties properties = new ApiKeyMaterialProtectionProperties();
        properties.setEncryptionKey(currentKey);
        properties.setPreviousEncryptionKeys(previousKeys);
        properties.setKeyVersion(1);
        return new AesGcmApiKeyMaterialProtector(properties);
    }
}
