package com.api2api.application.credential.command;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ApiKeyPreview;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.model.TokenLimit;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for creating an API credential without carrying plaintext key material.
 */
@Getter
@Builder
public final class CreateApiCredentialCommand {

    @NotNull
    private final UserAccountId ownerUserId;

    @NotNull
    private final ApiCredentialId apiCredentialId;

    @NotNull
    private final ApiCredentialName name;

    @NotNull
    private final ApiKeyHash keyHash;

    @NotNull
    private final ApiKeyPreview keyPreview;

    @NotNull
    private final EncryptedApiKeyMaterial encryptedKeyMaterial;

    @NotNull
    private final ModelWhitelist modelWhitelist;

    @NotNull
    private final TokenLimit tokenLimit;
}
