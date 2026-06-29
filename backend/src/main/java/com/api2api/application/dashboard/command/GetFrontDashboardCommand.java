package com.api2api.application.dashboard.command;

import com.api2api.domain.user.model.UserAccountId;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class GetFrontDashboardCommand {

    @NotNull
    private final UserAccountId currentUserId;

    @NotNull
    private final Instant todayStartInclusive;

    @NotNull
    private final Instant todayEndExclusive;

    @NotNull
    private final Instant thirtyDayStartInclusive;

    @NotNull
    private final Instant thirtyDayEndExclusive;

    @NotNull
    private final Instant recentCallsStartInclusive;

    @NotNull
    private final Instant recentCallsEndExclusive;

    @Min(1)
    private final int recentCallsPage;

    private final int recentCallsSize;

    @NotBlank
    private final String zoneId;
}
