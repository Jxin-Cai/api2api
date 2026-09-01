package com.api2api.domain.analytics.model;

import com.api2api.domain.usage.model.UsageTokenBreakdown;
import java.util.Objects;

/**
 * Token metrics visible on the regular user dashboard.
 */
public final class FrontDashboardMetrics {

    private final UsageTokenBreakdown todayTokenUsage;
    private final UsageTokenBreakdown monthTokenUsage;

    private FrontDashboardMetrics(UsageTokenBreakdown todayTokenUsage, UsageTokenBreakdown monthTokenUsage) {
        this.todayTokenUsage = Objects.requireNonNull(todayTokenUsage, "Front dashboard today tokens must not be null");
        this.monthTokenUsage = Objects.requireNonNull(monthTokenUsage, "Front dashboard month tokens must not be null");
    }

    public static FrontDashboardMetrics of(UsageTokenBreakdown todayTokenUsage, UsageTokenBreakdown monthTokenUsage) {
        return new FrontDashboardMetrics(todayTokenUsage, monthTokenUsage);
    }

    public TokenAmount todayActualTokens() {
        return TokenAmount.of(todayTokenUsage.actualTokens());
    }

    public TokenAmount todayTotalTokens() {
        return TokenAmount.of(todayTokenUsage.totalTokens());
    }

    public TokenAmount monthActualTokens() {
        return TokenAmount.of(monthTokenUsage.actualTokens());
    }

    public TokenAmount monthTotalTokens() {
        return TokenAmount.of(monthTokenUsage.totalTokens());
    }

    public TokenAmount todayTokens() {
        return todayActualTokens();
    }

    public TokenAmount monthTokens() {
        return monthActualTokens();
    }

    public TokenAmount getTodayTokens() {
        return todayActualTokens();
    }

    public TokenAmount getMonthTokens() {
        return monthActualTokens();
    }

    public TokenAmount getTodayActualTokens() {
        return todayActualTokens();
    }

    public TokenAmount getTodayTotalTokens() {
        return todayTotalTokens();
    }

    public TokenAmount getMonthActualTokens() {
        return monthActualTokens();
    }

    public TokenAmount getMonthTotalTokens() {
        return monthTotalTokens();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontDashboardMetrics that)) {
            return false;
        }
        return Objects.equals(todayTokenUsage, that.todayTokenUsage)
                && Objects.equals(monthTokenUsage, that.monthTokenUsage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(todayTokenUsage, monthTokenUsage);
    }
}
