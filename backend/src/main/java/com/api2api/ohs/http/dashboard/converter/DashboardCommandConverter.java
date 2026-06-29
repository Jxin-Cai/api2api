package com.api2api.ohs.http.dashboard.converter;

import com.api2api.application.dashboard.command.GetAdminDashboardCommand;
import com.api2api.application.dashboard.command.GetFrontDashboardCommand;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.dashboard.DashboardTimeWindowHelper;
import com.api2api.ohs.http.dashboard.dto.GetAdminDashboardRequest;
import com.api2api.ohs.http.dashboard.dto.GetFrontDashboardRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds dashboard application commands from HTTP requests using time window helper.
 */
@Component
@RequiredArgsConstructor
public class DashboardCommandConverter {

    private static final int DEFAULT_RECENT_CALLS_PAGE = 1;
    private static final int DEFAULT_RECENT_CALLS_SIZE = 50;

    @NonNull
    private final DashboardTimeWindowHelper timeWindowHelper;

    public GetFrontDashboardCommand toGetFrontDashboardCommand(
            GetFrontDashboardRequest request,
            UserAccountId currentUserId
    ) {
        String zoneId = effectiveZoneId(request.getZoneId());
        return GetFrontDashboardCommand.builder()
                .currentUserId(currentUserId)
                .todayStartInclusive(timeWindowHelper.getTodayStartInclusive(zoneId))
                .todayEndExclusive(timeWindowHelper.getTodayEndExclusive(zoneId))
                .thirtyDayStartInclusive(timeWindowHelper.getThirtyDayStartInclusive(zoneId))
                .thirtyDayEndExclusive(timeWindowHelper.getTodayEndExclusive(zoneId))
                .recentCallsStartInclusive(timeWindowHelper.getRecentCallsStartInclusive(
                        zoneId, DashboardTimeWindowHelper.DEFAULT_RECENT_CALLS_MINUTES))
                .recentCallsEndExclusive(timeWindowHelper.getRecentCallsEndExclusive(zoneId))
                .recentCallsPage(request.getRecentCallsPage() == null
                        ? DEFAULT_RECENT_CALLS_PAGE : request.getRecentCallsPage())
                .recentCallsSize(request.getRecentCallsSize() == null
                        ? DEFAULT_RECENT_CALLS_SIZE : request.getRecentCallsSize())
                .zoneId(zoneId)
                .build();
    }

    public GetAdminDashboardCommand toGetAdminDashboardCommand(
            GetAdminDashboardRequest request,
            UserAccountId operatorUserId
    ) {
        String zoneId = effectiveZoneId(request.getZoneId());
        int recentRateMinutes = request.getRecentRateMinutes() == null
                ? DashboardTimeWindowHelper.DEFAULT_RECENT_RATE_MINUTES : request.getRecentRateMinutes();
        int trendDays = request.getTrendDays() == null
                ? DashboardTimeWindowHelper.DEFAULT_TREND_DAYS : request.getTrendDays();
        return GetAdminDashboardCommand.builder()
                .operatorUserId(operatorUserId)
                .todayStartInclusive(timeWindowHelper.getTodayStartInclusive(zoneId))
                .todayEndExclusive(timeWindowHelper.getTodayEndExclusive(zoneId))
                .monthStartInclusive(timeWindowHelper.getMonthStartInclusive(zoneId))
                .monthEndExclusive(timeWindowHelper.getMonthEndExclusive(zoneId))
                .recentRateStartInclusive(timeWindowHelper.getRecentRateStartInclusive(zoneId, recentRateMinutes))
                .recentRateEndExclusive(timeWindowHelper.getRecentRateEndExclusive(zoneId))
                .trendStartInclusive(timeWindowHelper.getTrendStartInclusive(zoneId, trendDays))
                .trendEndExclusive(timeWindowHelper.getTrendEndExclusive(zoneId))
                .zoneId(zoneId)
                .build();
    }

    private String effectiveZoneId(String zoneId) {
        return zoneId == null || zoneId.isBlank() ? DashboardTimeWindowHelper.DEFAULT_ZONE_ID : zoneId;
    }
}
