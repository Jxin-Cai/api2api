package com.api2api.application.user.command;

import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for enabling or disabling a user account from the admin back-office.
 */
@Getter
@Builder
public final class ChangeUserStatusCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final UserAccountId targetUserId;
}
