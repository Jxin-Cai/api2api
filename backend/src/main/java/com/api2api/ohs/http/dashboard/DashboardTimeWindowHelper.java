package com.api2api.ohs.http.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Generates Instant-based time windows for dashboard queries.
 */
@Component
public class DashboardTimeWindowHelper {

    public static final String DEFAULT_ZONE_ID = "UTC";
    public static final int DEFAULT_RECENT_CALLS_MINUTES = 60;
    public static final int DEFAULT_RECENT_RATE_MINUTES = 5;
    public static final int DEFAULT_TREND_DAYS = 7;

    public Instant getTodayStartInclusive(String zoneId) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant getTodayEndExclusive(String zoneId) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant getThirtyDayStartInclusive(String zoneId) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .minusDays(29)
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant getMonthStartInclusive(String zoneId) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .withDayOfMonth(1)
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant getMonthEndExclusive(String zoneId) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .plusMonths(1)
                .withDayOfMonth(1)
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant getRecentCallsStartInclusive(String zoneId, int minutes) {
        return Instant.now().minus(minutes, ChronoUnit.MINUTES);
    }

    public Instant getRecentCallsEndExclusive(String zoneId) {
        return Instant.now();
    }

    public Instant getRecentRateStartInclusive(String zoneId, int minutes) {
        return Instant.now().minus(minutes, ChronoUnit.MINUTES);
    }

    public Instant getRecentRateEndExclusive(String zoneId) {
        return Instant.now();
    }

    public Instant getTrendStartInclusive(String zoneId, int days) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .minusDays(days - 1)
                .atStartOfDay(zone)
                .toInstant();
    }

    public Instant getTrendEndExclusive(String zoneId) {
        ZoneId zone = parseZoneId(zoneId);
        return LocalDate.now(zone)
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant();
    }

    private ZoneId parseZoneId(String zoneId) {
        String effectiveZoneId = zoneId == null || zoneId.isBlank() ? DEFAULT_ZONE_ID : zoneId;
        return ZoneId.of(effectiveZoneId);
    }
}
