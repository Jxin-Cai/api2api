package com.api2api.ohs.http.dashboard;

import com.api2api.application.dashboard.DashboardApplicationService;
import com.api2api.application.dashboard.command.GetAdminDashboardCommand;
import com.api2api.application.dashboard.command.GetFrontDashboardCommand;
import com.api2api.domain.analytics.model.AdminDashboardMetrics;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.dashboard.converter.DashboardCommandConverter;
import com.api2api.ohs.http.dashboard.converter.DashboardResponseConverter;
import com.api2api.ohs.http.dashboard.dto.AdminDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.FrontDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.GetAdminDashboardRequest;
import com.api2api.ohs.http.dashboard.dto.GetFrontDashboardRequest;
import jakarta.servlet.http.HttpServletRequest;
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
