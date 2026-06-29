package com.api2api.application.credential.command;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for replacing an API credential model whitelist.
 */
@Getter
@Builder
public final class ReplaceModelWhitelistCommand {

    @NotNull
    private final UserAccountId ownerUserId;

    @NotNull
    private final ApiCredentialId apiCredentialId;

    @NotNull
    private final ModelWhitelist modelWhitelist;
}
