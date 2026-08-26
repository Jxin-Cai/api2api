package com.api2api.application.dashboard.command;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public final class GetFrontKeyMetricsCommand {

    @NotNull
    private final UserAccountId currentUserId;

    @NotNull
    private final Instant todayStartInclusive;

    @NotNull
    private final Instant todayEndExclusive;

    @NotNull
    private final Instant monthStartInclusive;

    @NotNull
    private final Instant monthEndExclusive;

    @NotNull
    private final Instant trendStartInclusive;

    @NotNull
    private final Instant trendEndExclusive;

    @NotNull
    @Singular
    private final List<ApiCredentialId> trendCredentialIds;

    @NotBlank
    private final String zoneId;
}
