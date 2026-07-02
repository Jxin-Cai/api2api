package com.api2api.application.credential.command;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class RevealApiCredentialSecretCommand {

    @NotNull
    private final UserAccountId ownerUserId;

    @NotNull
    private final ApiCredentialId apiCredentialId;
}
