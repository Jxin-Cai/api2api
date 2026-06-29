package com.api2api.domain.analytics.model;

import com.api2api.domain.user.model.UserAccountId;
import java.util.Objects;

/**
 * Query criteria for the administrative analytics dashboard.
 */
public final class AdminDashboardQuery {

    private final UserAccountId viewerUserId;
    private final AnalyticsTimeWindow todayWindow;
    private final AnalyticsTimeWindow monthWindow;
    private final AnalyticsTimeWindow recentRateWindow;
    private final AnalyticsTimeWindow trendWindow;

    private AdminDashboardQuery(
            UserAccountId viewerUserId,
            AnalyticsTimeWindow todayWindow,
            AnalyticsTimeWindow monthWindow,
            AnalyticsTimeWindow recentRateWindow,
            AnalyticsTimeWindow trendWindow
    ) {
        this.viewerUserId = Objects.requireNonNull(viewerUserId, "Admin dashboard viewer user id must not be null");
        this.todayWindow = Objects.requireNonNull(todayWindow, "Admin dashboard today window must not be null");
        this.monthWindow = Objects.requireNonNull(monthWindow, "Admin dashboard month window must not be null");
        this.recentRateWindow = Objects.requireNonNull(recentRateWindow, "Admin dashboard recent rate window must not be null");
        this.trendWindow = Objects.requireNonNull(trendWindow, "Admin dashboard trend window must not be null");
    }

    public static AdminDashboardQuery of(
            UserAccountId viewerUserId,
            AnalyticsTimeWindow todayWindow,
            AnalyticsTimeWindow monthWindow,
            AnalyticsTimeWindow recentRateWindow,
            AnalyticsTimeWindow trendWindow
    ) {
        return new AdminDashboardQuery(viewerUserId, todayWindow, monthWindow, recentRateWindow, trendWindow);
    }

    public UserAccountId viewerUserId() {
        return viewerUserId;
    }

    public AnalyticsTimeWindow todayWindow() {
        return todayWindow;
    }

    public AnalyticsTimeWindow monthWindow() {
        return monthWindow;
    }

    public AnalyticsTimeWindow recentRateWindow() {
        return recentRateWindow;
    }

    public AnalyticsTimeWindow trendWindow() {
        return trendWindow;
    }

    public UserAccountId getViewerUserId() {
        return viewerUserId;
    }

    public AnalyticsTimeWindow getTodayWindow() {
        return todayWindow;
    }

    public AnalyticsTimeWindow getMonthWindow() {
        return monthWindow;
    }

    public AnalyticsTimeWindow getRecentRateWindow() {
        return recentRateWindow;
    }

    public AnalyticsTimeWindow getTrendWindow() {
        return trendWindow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdminDashboardQuery that)) {
            return false;
        }
        return Objects.equals(viewerUserId, that.viewerUserId)
                && Objects.equals(todayWindow, that.todayWindow)
                && Objects.equals(monthWindow, that.monthWindow)
                && Objects.equals(recentRateWindow, that.recentRateWindow)
                && Objects.equals(trendWindow, that.trendWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewerUserId, todayWindow, monthWindow, recentRateWindow, trendWindow);
    }
}
