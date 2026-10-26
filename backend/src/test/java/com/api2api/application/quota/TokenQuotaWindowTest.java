package com.api2api.application.quota;

import static org.assertj.core.api.Assertions.assertThat;
import com.api2api.domain.quota.QuotaPeriod;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TokenQuotaWindowTest {
    @ParameterizedTest
    @CsvSource({
        "DAY,2026-10-26T16:00:00Z,2026-10-27T16:00:00Z",
        "WEEK,2026-10-25T16:00:00Z,2026-11-01T16:00:00Z",
        "MONTH,2026-09-30T16:00:00Z,2026-10-31T16:00:00Z"
    })
    void test_uses_calendar_boundaries_when_timezone_is_shanghai(QuotaPeriod period, String start, String end) {
        // Arrange
        var windows = new TokenQuotaWindow(Clock.fixed(Instant.parse("2026-10-26T17:00:00Z"), ZoneOffset.UTC), "Asia/Shanghai");
        // Act
        var window = windows.current(period);
        // Assert
        assertThat(window).isEqualTo(com.api2api.domain.usage.model.UsageTimeRange.of(Instant.parse(start), Instant.parse(end)));
    }

    @ParameterizedTest
    @CsvSource({
        "MONTH,2028-02-20T12:00:00Z,2028-02-29T16:00:00Z",
        "WEEK,2026-12-31T12:00:00Z,2027-01-03T16:00:00Z",
        "DAY,2026-10-26T16:00:00Z,2026-10-27T16:00:00Z"
    })
    void test_resets_correctly_when_period_crosses_calendar_boundary(QuotaPeriod period, String now, String end) {
        // Arrange
        var windows = new TokenQuotaWindow(Clock.fixed(Instant.parse(now), ZoneOffset.UTC), "Asia/Shanghai");
        // Act
        var actual = windows.current(period).endExclusive();
        // Assert
        assertThat(actual).isEqualTo(Instant.parse(end));
    }
}
