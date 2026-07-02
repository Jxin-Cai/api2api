package com.api2api.application.credential;

import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;

public interface ApiKeyMaterialProtector {

    EncryptedApiKeyMaterial protect(String plaintextKey);

    String reveal(EncryptedApiKeyMaterial encryptedKeyMaterial);
}
