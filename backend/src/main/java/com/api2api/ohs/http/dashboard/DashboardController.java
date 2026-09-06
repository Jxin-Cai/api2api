package com.api2api.ohs.http.dashboard;

import com.api2api.application.dashboard.DashboardApplicationService;
import com.api2api.application.dashboard.command.GetAdminDashboardCommand;
import com.api2api.application.dashboard.command.GetFrontDashboardCommand;
import com.api2api.application.dashboard.command.GetFrontKeyMetricsCommand;
import com.api2api.domain.analytics.model.AdminDashboardMetrics;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.analytics.model.FrontKeyMetrics;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.dashboard.converter.DashboardCommandConverter;
import com.api2api.ohs.http.dashboard.converter.DashboardResponseConverter;
import com.api2api.ohs.http.dashboard.dto.AdminDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.FrontDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.FrontKeyMetricsResponse;
import com.api2api.ohs.http.dashboard.dto.GetAdminDashboardRequest;
import com.api2api.ohs.http.dashboard.dto.GetFrontDashboardRequest;
import com.api2api.ohs.http.dashboard.dto.GetFrontKeyMetricsRequest;
import com.api2api.ohs.http.dashboard.dto.UsageDistributionResponse;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for dashboard metrics (frontend and admin).
 */
@RestController
@Validated
@RequiredArgsConstructor
public class DashboardController {

    @NonNull
    private final DashboardApplicationService dashboardApplicationService;

    @NonNull
    private final DashboardCommandConverter dashboardCommandConverter;

    @NonNull
    private final DashboardResponseConverter dashboardResponseConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @NonNull
    private final DashboardTimeWindowHelper dashboardTimeWindowHelper;

    @GetMapping("/api/dashboard")
    public ApiResponse<FrontDashboardResponse> getFrontDashboard(
            @Valid GetFrontDashboardRequest dashboardRequest,
            HttpServletRequest request
    ) {
        UserAccountId currentUserId = currentUserContextResolver.resolveCurrentUserId(request);
        GetFrontDashboardCommand command = dashboardCommandConverter.toGetFrontDashboardCommand(
                dashboardRequest, currentUserId);

        FrontDashboardMetrics metrics = dashboardApplicationService.getFrontDashboard(command);
        long apiKeyCount = dashboardApplicationService.countFrontDashboardApiKeys(command);
        PagedUsageRecords recentCalls = dashboardApplicationService.queryFrontDashboardRecentCalls(command);

        return ApiResponse.success(dashboardResponseConverter.toFrontDashboardResponse(metrics, apiKeyCount, recentCalls));
    }

    @GetMapping("/api/dashboard/key-metrics")
    public ApiResponse<FrontKeyMetricsResponse> getFrontKeyMetrics(
            @Valid GetFrontKeyMetricsRequest metricsRequest,
            HttpServletRequest request
    ) {
        UserAccountId currentUserId = currentUserContextResolver.resolveCurrentUserId(request);
        GetFrontKeyMetricsCommand command = dashboardCommandConverter.toGetFrontKeyMetricsCommand(
                metricsRequest, currentUserId);

        FrontKeyMetrics metrics = dashboardApplicationService.getFrontKeyMetrics(command);

        return ApiResponse.success(dashboardResponseConverter.toFrontKeyMetricsResponse(metrics));
    }

    @GetMapping("/api/dashboard/distributions")
    public ApiResponse<UsageDistributionResponse> getFrontDistributions(GetFrontDashboardRequest request, HttpServletRequest httpRequest) {
        UserAccountId userId = currentUserContextResolver.resolveCurrentUserId(httpRequest);
        AnalyticsTimeWindow window = AnalyticsTimeWindow.of(
                dashboardTimeWindowHelper.getTrendStartInclusive(request.getZoneId(), 7),
                dashboardTimeWindowHelper.getTrendEndExclusive(request.getZoneId()), request.getZoneId());
        return ApiResponse.success(toDistributionResponse(
                dashboardApplicationService.getDistribution(window, userId, true),
                dashboardApplicationService.getDistribution(window, userId, false)));
    }

    @GetMapping("/api/admin/dashboard/distributions")
    public ApiResponse<UsageDistributionResponse> getAdminDistributions(GetAdminDashboardRequest request, HttpServletRequest httpRequest) {
        currentUserContextResolver.resolveOperatorUserId(httpRequest);
        AnalyticsTimeWindow window = AnalyticsTimeWindow.of(
                dashboardTimeWindowHelper.getTrendStartInclusive(request.getZoneId(), request.getTrendDays() == null ? 7 : request.getTrendDays()),
                dashboardTimeWindowHelper.getTrendEndExclusive(request.getZoneId()), request.getZoneId());
        return ApiResponse.success(toDistributionResponse(
                dashboardApplicationService.getDistribution(window, null, true),
                dashboardApplicationService.getDistribution(window, null, false)));
    }

    private UsageDistributionResponse toDistributionResponse(List<com.api2api.domain.analytics.model.UsageDistributionItem> models, List<com.api2api.domain.analytics.model.UsageDistributionItem> channels) {
        return UsageDistributionResponse.builder()
                .models(models.stream().map(item -> UsageDistributionResponse.DistributionItem.builder().name(item.name()).value(item.value()).build()).toList())
                .channels(channels.stream().map(item -> UsageDistributionResponse.DistributionItem.builder().name(item.name()).value(item.value()).build()).toList())
                .build();
    }

    @GetMapping("/api/admin/dashboard")
    public ApiResponse<AdminDashboardResponse> getAdminDashboard(
            @Valid GetAdminDashboardRequest dashboardRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        GetAdminDashboardCommand command = dashboardCommandConverter.toGetAdminDashboardCommand(
                dashboardRequest, operatorUserId);

        AdminDashboardMetrics metrics = dashboardApplicationService.getAdminDashboard(command);

        return ApiResponse.success(dashboardResponseConverter.toAdminDashboardResponse(metrics));
    }
}
