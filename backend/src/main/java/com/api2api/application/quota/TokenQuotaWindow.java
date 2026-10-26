package com.api2api.application.quota;

import com.api2api.domain.quota.QuotaPeriod;
import com.api2api.domain.usage.model.UsageTimeRange;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenQuotaWindow {
    private final Clock clock;
    private final ZoneId zone;

    public TokenQuotaWindow(Clock clock,
            @Value("${api2api.gateway.model-daily-limit-zone:Asia/Shanghai}") String zoneId) {
        this.clock = clock;
        this.zone = ZoneId.of(zoneId);
    }

    public UsageTimeRange current(QuotaPeriod period) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate start = switch (period) {
            case DAY -> today;
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> today.withDayOfMonth(1);
            case LIFETIME -> LocalDate.of(1970, 1, 1);
        };
        LocalDate end = switch (period) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case LIFETIME -> LocalDate.of(9999, 1, 1);
        };
        return UsageTimeRange.of(start.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant());
    }

    public String zoneId() { return zone.getId(); }
}
