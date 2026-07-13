package com.api2api.application.credential.command;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for soft deleting an API credential owned by the current user.
 */
@Getter
@Builder
public final class DeleteApiCredentialCommand {

    @NotNull
    private final UserAccountId ownerUserId;

    @NotNull
    private final ApiCredentialId apiCredentialId;
}
