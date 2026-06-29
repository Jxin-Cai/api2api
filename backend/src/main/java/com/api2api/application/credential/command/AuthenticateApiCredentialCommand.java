package com.api2api.application.credential.command;

import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ModelName;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for gateway API credential authentication and preflight authorization checks.
 */
@Getter
@Builder
public final class AuthenticateApiCredentialCommand {

    @NotNull
    private final ApiKeyHash keyHash;

    @NotNull
    private final ModelName requestedModel;
}
