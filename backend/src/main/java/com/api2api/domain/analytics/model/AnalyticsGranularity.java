package com.api2api.domain.analytics.model;

import java.util.Objects;

/**
 * Supported analytics bucket granularity.
 */
public enum AnalyticsGranularity {
    DAY,
    HOUR,
    MINUTE;

    public static AnalyticsGranularity requireSupported(AnalyticsGranularity granularity) {
        return Objects.requireNonNull(granularity, "Analytics granularity must not be null");
    }

    public boolean isDaily() {
        return this == DAY;
    }
}
