package com.api2api.application.user.command;

import com.api2api.domain.user.model.Username;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for loading a user account during login.
 */
@Getter
@Builder
public final class LoginCommand {

    @NotNull
    private final Username username;
}
