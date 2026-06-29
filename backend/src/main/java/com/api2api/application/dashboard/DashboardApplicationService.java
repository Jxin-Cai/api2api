package com.api2api.application.dashboard;

import com.api2api.application.BusinessException;
import com.api2api.application.dashboard.command.GetAdminDashboardCommand;
import com.api2api.application.dashboard.command.GetFrontDashboardCommand;
import com.api2api.domain.analytics.model.AdminDashboardMetrics;
import com.api2api.domain.analytics.model.AdminDashboardQuery;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.analytics.model.FrontDashboardQuery;
import com.api2api.domain.analytics.repository.DashboardAnalyticsRepository;
import com.api2api.domain.analytics.service.DashboardAnalyticsService;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.usage.model.PageRequestSpec;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageTimeRange;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    @NonNull
    private final UserAccountRepository userAccountRepository;

    @NonNull
    private final DashboardAnalyticsRepository dashboardAnalyticsRepository;

    @NonNull
    private final DashboardAnalyticsService dashboardAnalyticsService;

    @NonNull
    private final ApiCredentialRepository apiCredentialRepository;

    @NonNull
    private final UsageRecordRepository usageRecordRepository;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public FrontDashboardMetrics getFrontDashboard(GetFrontDashboardCommand command) {
        UserAccount currentUser = loadFrontUser(command);
        currentUser.assertCanAccess(AccessScope.USER_PORTAL);

        AnalyticsTimeWindow todayWindow = AnalyticsTimeWindow.of(
                command.getTodayStartInclusive(),
                command.getTodayEndExclusive(),
                command.getZoneId()
        );
        AnalyticsTimeWindow thirtyDayWindow = AnalyticsTimeWindow.of(
                command.getThirtyDayStartInclusive(),
                command.getThirtyDayEndExclusive(),
                command.getZoneId()
        );
        FrontDashboardQuery query = FrontDashboardQuery.of(
                command.getCurrentUserId(),
                todayWindow,
                thirtyDayWindow
        );

        return dashboardAnalyticsService.buildFrontMetrics(query, dashboardAnalyticsRepository);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public long countFrontDashboardApiKeys(GetFrontDashboardCommand command) {
        UserAccount currentUser = loadFrontUser(command);
        currentUser.assertCanAccess(AccessScope.USER_PORTAL);

        List<ApiCredential> credentials = apiCredentialRepository.findByOwnerUserId(command.getCurrentUserId());
        if (credentials == null) {
            throw new BusinessException("API_CREDENTIAL_QUERY_FAILED");
        }
        return credentials.size();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PagedUsageRecords queryFrontDashboardRecentCalls(GetFrontDashboardCommand command) {
        UserAccount currentUser = loadFrontUser(command);
        currentUser.assertCanAccess(AccessScope.USER_PORTAL);

        UsageTimeRange timeRange = UsageTimeRange.of(
                command.getRecentCallsStartInclusive(),
                command.getRecentCallsEndExclusive()
        );
        PageRequestSpec pageRequest = PageRequestSpec.of(command.getRecentCallsPage(), command.getRecentCallsSize());
        UsageRecordFilter filter = UsageRecordFilter.forUserPortal(
                command.getCurrentUserId(),
                null,
                null,
                null,
                timeRange
        );
        PagedUsageRecords page = usageRecordRepository.query(filter, pageRequest);
        List<UsageRecord> redactedRecords = page.getRecords().stream()
                .map(UsageRecord::redactForUserPortal)
                .toList();

        return PagedUsageRecords.of(
                redactedRecords,
                page.getPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getFilteredTokenTotal()
        );
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminDashboardMetrics getAdminDashboard(GetAdminDashboardCommand command) {
        UserAccount operator = userAccountRepository.findById(command.getOperatorUserId())
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"));
        operator.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);

        AnalyticsTimeWindow todayWindow = AnalyticsTimeWindow.of(
                command.getTodayStartInclusive(),
                command.getTodayEndExclusive(),
                command.getZoneId()
        );
        AnalyticsTimeWindow monthWindow = AnalyticsTimeWindow.of(
                command.getMonthStartInclusive(),
                command.getMonthEndExclusive(),
                command.getZoneId()
        );
        AnalyticsTimeWindow recentRateWindow = AnalyticsTimeWindow.of(
                command.getRecentRateStartInclusive(),
                command.getRecentRateEndExclusive(),
                command.getZoneId()
        );
        AnalyticsTimeWindow trendWindow = AnalyticsTimeWindow.of(
                command.getTrendStartInclusive(),
                command.getTrendEndExclusive(),
                command.getZoneId()
        );
        AdminDashboardQuery query = AdminDashboardQuery.of(
                command.getOperatorUserId(),
                todayWindow,
                monthWindow,
                recentRateWindow,
                trendWindow
        );

        return dashboardAnalyticsService.buildAdminMetrics(query, operator, dashboardAnalyticsRepository);
    }

    private UserAccount loadFrontUser(GetFrontDashboardCommand command) {
        return userAccountRepository.findById(command.getCurrentUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
    }
}
