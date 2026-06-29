package com.api2api.application.dashboard.command;

import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class GetAdminDashboardCommand {

    @NotNull
    private final UserAccountId operatorUserId;

    @NotNull
    private final Instant todayStartInclusive;

    @NotNull
    private final Instant todayEndExclusive;

    @NotNull
    private final Instant monthStartInclusive;

    @NotNull
    private final Instant monthEndExclusive;

    @NotNull
    private final Instant recentRateStartInclusive;

    @NotNull
    private final Instant recentRateEndExclusive;

    @NotNull
    private final Instant trendStartInclusive;

    @NotNull
    private final Instant trendEndExclusive;

    @NotBlank
    private final String zoneId;
}
