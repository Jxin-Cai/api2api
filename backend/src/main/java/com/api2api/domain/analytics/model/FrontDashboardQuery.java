package com.api2api.domain.analytics.model;

import com.api2api.domain.user.model.UserAccountId;
import java.util.Objects;

/**
 * Query criteria for the regular user dashboard.
 */
public final class FrontDashboardQuery {

    private final UserAccountId userAccountId;
    private final AnalyticsTimeWindow todayWindow;
    private final AnalyticsTimeWindow monthWindow;

    private FrontDashboardQuery(
            UserAccountId userAccountId,
            AnalyticsTimeWindow todayWindow,
            AnalyticsTimeWindow monthWindow
    ) {
        this.userAccountId = Objects.requireNonNull(userAccountId, "Front dashboard user account id must not be null");
        this.todayWindow = Objects.requireNonNull(todayWindow, "Front dashboard today window must not be null");
        this.monthWindow = Objects.requireNonNull(monthWindow, "Front dashboard month window must not be null");
    }

    public static FrontDashboardQuery of(
            UserAccountId userAccountId,
            AnalyticsTimeWindow todayWindow,
            AnalyticsTimeWindow monthWindow
    ) {
        return new FrontDashboardQuery(userAccountId, todayWindow, monthWindow);
    }

    public UserAccountId userAccountId() {
        return userAccountId;
    }

    public AnalyticsTimeWindow todayWindow() {
        return todayWindow;
    }

    public AnalyticsTimeWindow monthWindow() {
        return monthWindow;
    }

    public UserAccountId getUserAccountId() {
        return userAccountId;
    }

    public AnalyticsTimeWindow getTodayWindow() {
        return todayWindow;
    }

    public AnalyticsTimeWindow getMonthWindow() {
        return monthWindow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontDashboardQuery that)) {
            return false;
        }
        return Objects.equals(userAccountId, that.userAccountId)
                && Objects.equals(todayWindow, that.todayWindow)
                && Objects.equals(monthWindow, that.monthWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAccountId, todayWindow, monthWindow);
    }
}
