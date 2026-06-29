package com.api2api.application.user.command;

import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for changing a user account role from the admin back-office.
 */
@Getter
@Builder
public final class ChangeUserRoleCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final UserAccountId targetUserId;

    @NotNull
    private final UserRole newRole;
}
