package com.api2api.domain.analytics.service;

import com.api2api.domain.analytics.model.AdminDashboardMetrics;
import com.api2api.domain.analytics.model.AdminDashboardQuery;
import com.api2api.domain.analytics.model.AnalyticsGranularity;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.ChannelTokenTrendPoint;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.analytics.model.FrontDashboardQuery;
import com.api2api.domain.analytics.model.ProtocolRequestRate;
import com.api2api.domain.analytics.model.ProtocolTokenTrendPoint;
import com.api2api.domain.analytics.model.TokenAmount;
import com.api2api.domain.analytics.model.UsageSummaryMetrics;
import com.api2api.domain.analytics.model.UsageSummaryQuery;
import com.api2api.domain.analytics.model.UserTokenRanking;
import com.api2api.domain.analytics.repository.DashboardAnalyticsRepository;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Domain service that assembles dashboard analytics read models and enforces role-specific visibility rules.
 */
@Service
public final class DashboardAnalyticsService {

    private static final int DASHBOARD_TOP_USER_LIMIT = 10;

    public FrontDashboardMetrics buildFrontMetrics(
            FrontDashboardQuery query,
            DashboardAnalyticsRepository repository
    ) {
        FrontDashboardQuery nonNullQuery = Objects.requireNonNull(query, "Front dashboard query must not be null");
        DashboardAnalyticsRepository nonNullRepository = requireRepository(repository);

        TokenAmount todayTokens = requireTokenAmount(nonNullRepository.sumUserTotalTokens(
                nonNullQuery.userAccountId(),
                nonNullQuery.todayWindow()
        ), "Front dashboard today tokens");
        TokenAmount monthTokens = requireTokenAmount(nonNullRepository.sumUserTotalTokens(
                nonNullQuery.userAccountId(),
                nonNullQuery.monthWindow()
        ), "Front dashboard month tokens");

        return FrontDashboardMetrics.of(todayTokens, monthTokens);
    }

    public AdminDashboardMetrics buildAdminMetrics(
            AdminDashboardQuery query,
            UserAccount viewer,
            DashboardAnalyticsRepository repository
    ) {
        AdminDashboardQuery nonNullQuery = Objects.requireNonNull(query, "Admin dashboard query must not be null");
        UserAccount nonNullViewer = Objects.requireNonNull(viewer, "Admin dashboard viewer must not be null");
        DashboardAnalyticsRepository nonNullRepository = requireRepository(repository);

        nonNullViewer.assertActive();
        nonNullViewer.assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
        if (!nonNullViewer.getId().equals(nonNullQuery.viewerUserId())) {
            throw new IllegalArgumentException("Admin dashboard viewer id must match query viewer user id");
        }

        List<ProtocolRequestRate> protocolRequestRates = normalizeProtocolRequestRates(
                nonNullRepository.calculateProtocolRequestRates(nonNullQuery.recentRateWindow()),
                nonNullQuery.recentRateWindow()
        );
        TokenAmount todayTokens = requireTokenAmount(
                nonNullRepository.sumPlatformTotalTokens(nonNullQuery.todayWindow()),
                "Admin dashboard today tokens"
        );
        TokenAmount monthTokens = requireTokenAmount(
                nonNullRepository.sumPlatformTotalTokens(nonNullQuery.monthWindow()),
                "Admin dashboard month tokens"
        );
        List<UserTokenRanking> dailyTopUsers = normalizeTopUsers(
                nonNullRepository.findTopUsersByTokens(nonNullQuery.todayWindow(), DASHBOARD_TOP_USER_LIMIT),
                "Daily top users"
        );
        List<UserTokenRanking> monthlyTopUsers = normalizeTopUsers(
                nonNullRepository.findTopUsersByTokens(nonNullQuery.monthWindow(), DASHBOARD_TOP_USER_LIMIT),
                "Monthly top users"
        );
        List<ProtocolTokenTrendPoint> protocolTokenTrends = requireList(
                nonNullRepository.sumProtocolTokenTrends(nonNullQuery.trendWindow(), AnalyticsGranularity.DAY),
                "Protocol token trends"
        );
        List<ChannelTokenTrendPoint> channelTokenTrends = requireList(
                nonNullRepository.sumChannelTokenTrends(nonNullQuery.trendWindow(), AnalyticsGranularity.DAY),
                "Channel token trends"
        );

        return AdminDashboardMetrics.of(
                protocolRequestRates,
                todayTokens,
                monthTokens,
                dailyTopUsers,
                monthlyTopUsers,
                protocolTokenTrends,
                channelTokenTrends
        );
    }

    public UsageSummaryMetrics summarizeUsage(
            UsageSummaryQuery query,
            DashboardAnalyticsRepository repository
    ) {
        UsageSummaryQuery nonNullQuery = Objects.requireNonNull(query, "Usage summary query must not be null");
        DashboardAnalyticsRepository nonNullRepository = requireRepository(repository);

        UsageTokenBreakdown filteredTokenTotal = Objects.requireNonNull(
                nonNullRepository.sumUsageTokens(nonNullQuery.filter()),
                "Usage summary filtered token total must not be null"
        );
        long totalRecords = nonNullRepository.countUsageRecords(nonNullQuery.filter());
        if (totalRecords < 0) {
            throw new IllegalArgumentException("Usage summary total records must be greater than or equal to 0");
        }

        return UsageSummaryMetrics.of(filteredTokenTotal, totalRecords);
    }

    private static DashboardAnalyticsRepository requireRepository(DashboardAnalyticsRepository repository) {
        return Objects.requireNonNull(repository, "Dashboard analytics repository must not be null");
    }

    private static TokenAmount requireTokenAmount(TokenAmount tokenAmount, String name) {
        return Objects.requireNonNull(tokenAmount, name + " must not be null");
    }

    private static List<ProtocolRequestRate> normalizeProtocolRequestRates(
            List<ProtocolRequestRate> rates,
            AnalyticsTimeWindow window
    ) {
        AnalyticsTimeWindow nonNullWindow = Objects.requireNonNull(window, "Protocol request rate window must not be null");
        Map<ProtocolType, ProtocolRequestRate> byProtocol = requireList(rates, "Protocol request rates").stream()
                .collect(Collectors.toMap(
                        ProtocolRequestRate::protocol,
                        rate -> rate,
                        (left, right) -> {
                            throw new IllegalArgumentException("Protocol request rates must not contain duplicate protocols");
                        },
                        () -> new EnumMap<>(ProtocolType.class)
                ));

        return EnumSet.allOf(ProtocolType.class).stream()
                .map(protocol -> byProtocol.getOrDefault(protocol, ProtocolRequestRate.zero(protocol, nonNullWindow)))
                .toList();
    }

    private static List<UserTokenRanking> normalizeTopUsers(List<UserTokenRanking> rankings, String name) {
        List<UserTokenRanking> copiedRankings = requireList(rankings, name);
        if (copiedRankings.size() > DASHBOARD_TOP_USER_LIMIT) {
            throw new IllegalArgumentException(name + " must contain at most 10 rows");
        }
        List<UserTokenRanking> sortedRankings = copiedRankings.stream()
                .sorted(UserTokenRanking.STABLE_TOKEN_DESC_USER_ASC.thenComparingInt(UserTokenRanking::rank))
                .toList();
        if (!copiedRankings.equals(sortedRankings)) {
            throw new IllegalArgumentException(name + " must be sorted by total tokens descending and user account id ascending");
        }
        return copiedRankings;
    }

    private static <T> List<T> requireList(List<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        return values.stream()
                .map(value -> Objects.requireNonNull(value, name + " must not contain null elements"))
                .toList();
    }
}
