package com.api2api.domain.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.analytics.model.FrontDashboardQuery;
import com.api2api.domain.analytics.repository.DashboardAnalyticsRepository;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.UserAccountId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DashboardAnalyticsServiceTest {

    @Test
    void test_exposes_actual_and_total_token_sums_when_building_front_metrics() {
        DashboardAnalyticsRepository repository = mock(DashboardAnalyticsRepository.class);
        UserAccountId userAccountId = UserAccountId.of(2L);
        AnalyticsTimeWindow todayWindow = AnalyticsTimeWindow.of(
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z"),
                "Asia/Shanghai"
        );
        AnalyticsTimeWindow monthWindow = AnalyticsTimeWindow.of(
                Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"),
                "Asia/Shanghai"
        );
        when(repository.sumUserTokens(userAccountId, todayWindow))
                .thenReturn(UsageTokenBreakdown.known(100L, 20L, 0L, 0L));
        when(repository.sumUserTokens(userAccountId, monthWindow))
                .thenReturn(UsageTokenBreakdown.known(200L, 40L, 0L, 0L));
        DashboardAnalyticsService service = new DashboardAnalyticsService();

        FrontDashboardMetrics metrics = service.buildFrontMetrics(
                FrontDashboardQuery.of(userAccountId, todayWindow, monthWindow),
                repository
        );

        assertThat(metrics.getTodayActualTokens().getTokens()).isEqualByComparingTo(new BigDecimal("200"));
        assertThat(metrics.getTodayTotalTokens().getTokens()).isEqualByComparingTo(new BigDecimal("120"));
        assertThat(metrics.getMonthActualTokens().getTokens()).isEqualByComparingTo(new BigDecimal("400"));
        assertThat(metrics.getMonthTotalTokens().getTokens()).isEqualByComparingTo(new BigDecimal("240"));
    }
}
