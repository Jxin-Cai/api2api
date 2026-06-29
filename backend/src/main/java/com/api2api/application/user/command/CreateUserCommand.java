package com.api2api.application.user.command;

import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserRole;
import com.api2api.domain.user.model.Username;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for creating a user account from the admin back-office.
 */
@Getter
@Builder
public final class CreateUserCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final UserAccountId userAccountId;

    @NotNull
    private final Username username;

    @NotNull
    private final DisplayName displayName;

    @NotNull
    private final UserRole role;
}
