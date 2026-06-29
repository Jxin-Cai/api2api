package com.api2api.application.protocol.command;

import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Command for enabling or disabling a protocol conversion definition.
 */
@Getter
@Builder
public final class ChangeProtocolConversionStatusCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final ProtocolConversionDefinitionId definitionId;
}
