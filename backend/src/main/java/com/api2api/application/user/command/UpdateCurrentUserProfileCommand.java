package com.api2api.application.user.command;

import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for updating the current user's profile.
 */
@Getter
@Builder
public final class UpdateCurrentUserProfileCommand {

    @NotNull
    private final UserAccountId currentUserId;

    @NotNull
    private final DisplayName displayName;
}
