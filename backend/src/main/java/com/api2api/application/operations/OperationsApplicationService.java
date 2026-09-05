package com.api2api.application.operations;

import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.ChannelLatencyRanking;
import com.api2api.domain.analytics.model.ConcurrencyTrendPoint;
import com.api2api.domain.analytics.repository.DashboardAnalyticsRepository;
import com.api2api.application.BusinessException;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import com.api2api.infr.monitoring.SystemMetricsCollector;
import com.api2api.infr.monitoring.SystemSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationsApplicationService {
    private final UserAccountRepository users;
    private final DashboardAnalyticsRepository analytics;
    private final SystemMetricsCollector collector;

    public record TrafficSnapshot(Instant sampledAt, String zoneId,
            List<ConcurrencyTrendPoint> todayConcurrencyTrends, List<ChannelLatencyRanking> dailySlowestChannels) { }

    public SystemSnapshot system(UserAccountId operatorId) {
        assertAdmin(operatorId);
        return collector.snapshot();
    }

    @Transactional(readOnly = true)
    public TrafficSnapshot traffic(UserAccountId operatorId, ZoneId zone) {
        assertAdmin(operatorId);
        Instant now = Instant.now();
        var date = now.atZone(zone).toLocalDate();
        var today = AnalyticsTimeWindow.of(date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant(), zone.getId());
        return new TrafficSnapshot(now, zone.getId(),
                analytics.calculateConcurrencyTrends(today, Duration.ofMinutes(5), now),
                analytics.findSlowestChannels(today, 5));
    }

    private void assertAdmin(UserAccountId operatorId) {
        users.findById(operatorId).orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND"))
                .assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }
}
