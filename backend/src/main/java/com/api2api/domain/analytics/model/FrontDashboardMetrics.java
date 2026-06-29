package com.api2api.domain.analytics.model;

import java.util.Objects;

/**
 * Token metrics visible on the regular user dashboard.
 */
public final class FrontDashboardMetrics {

    private final TokenAmount todayTokens;
    private final TokenAmount monthTokens;

    private FrontDashboardMetrics(TokenAmount todayTokens, TokenAmount monthTokens) {
        this.todayTokens = Objects.requireNonNull(todayTokens, "Front dashboard today tokens must not be null");
        this.monthTokens = Objects.requireNonNull(monthTokens, "Front dashboard month tokens must not be null");
    }

    public static FrontDashboardMetrics of(TokenAmount todayTokens, TokenAmount monthTokens) {
        return new FrontDashboardMetrics(todayTokens, monthTokens);
    }

    public TokenAmount todayTokens() {
        return todayTokens;
    }

    public TokenAmount monthTokens() {
        return monthTokens;
    }

    public TokenAmount getTodayTokens() {
        return todayTokens;
    }

    public TokenAmount getMonthTokens() {
        return monthTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontDashboardMetrics that)) {
            return false;
        }
        return Objects.equals(todayTokens, that.todayTokens)
                && Objects.equals(monthTokens, that.monthTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(todayTokens, monthTokens);
    }
}
