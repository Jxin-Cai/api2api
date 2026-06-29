package com.api2api.application.user.command;

import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for changing a user account display name from the admin back-office.
 */
@Getter
@Builder
public final class ChangeUserDisplayNameCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final UserAccountId targetUserId;

    @NotNull
    private final DisplayName displayName;
}
