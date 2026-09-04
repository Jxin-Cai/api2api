package com.api2api.application.credential;

import com.api2api.domain.usage.model.UsageTimeRange;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Defines the calendar day used by model-group daily limits. Enforcement at the gateway and the
 * "rate limited" status shown in the portal must share this definition, so it lives in one place.
 * Configurable via {@code api2api.gateway.model-daily-limit-zone} (IANA zone id).
 */
@Component
public class ModelDailyLimitWindow {

    private final Clock clock;
    private final ZoneId zone;

    public ModelDailyLimitWindow(Clock clock,
                                 @Value("${api2api.gateway.model-daily-limit-zone:UTC}") String zoneId) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.zone = ZoneId.of(zoneId == null || zoneId.isBlank() ? "UTC" : zoneId.trim());
    }

    public UsageTimeRange today() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        return UsageTimeRange.of(start, end);
    }

    public ZoneId zone() {
        return zone;
    }
}
