package com.api2api.application.credential.command;

import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class UpdateModelGroupCommand {
    @NotNull private final UserAccountId ownerUserId;
    @NotNull private final ModelGroupId modelGroupId;
    @NotNull private final ModelGroupName name;
    @NotNull private final ModelWhitelist modelWhitelist;
}
