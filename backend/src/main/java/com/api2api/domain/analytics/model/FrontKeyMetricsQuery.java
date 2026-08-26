package com.api2api.domain.analytics.model;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import java.util.List;
import java.util.Objects;

/**
 * Query criteria for per-credential dashboard metrics of the current front-portal user.
 */
public final class FrontKeyMetricsQuery {

    private final UserAccountId userAccountId;
    private final AnalyticsTimeWindow todayWindow;
    private final AnalyticsTimeWindow monthWindow;
    private final AnalyticsTimeWindow trendWindow;
    private final List<ApiCredentialId> trendCredentialIds;

    private FrontKeyMetricsQuery(
            UserAccountId userAccountId,
            AnalyticsTimeWindow todayWindow,
            AnalyticsTimeWindow monthWindow,
            AnalyticsTimeWindow trendWindow,
            List<ApiCredentialId> trendCredentialIds
    ) {
        this.userAccountId = Objects.requireNonNull(userAccountId, "Front key metrics user account id must not be null");
        this.todayWindow = Objects.requireNonNull(todayWindow, "Front key metrics today window must not be null");
        this.monthWindow = Objects.requireNonNull(monthWindow, "Front key metrics month window must not be null");
        this.trendWindow = Objects.requireNonNull(trendWindow, "Front key metrics trend window must not be null");
        List<ApiCredentialId> nonNullIds = Objects.requireNonNull(
                trendCredentialIds, "Front key metrics trend credential ids must not be null");
        this.trendCredentialIds = nonNullIds.stream()
                .map(id -> Objects.requireNonNull(id, "Front key metrics trend credential ids must not contain null"))
                .distinct()
                .toList();
    }

    public static FrontKeyMetricsQuery of(
            UserAccountId userAccountId,
            AnalyticsTimeWindow todayWindow,
            AnalyticsTimeWindow monthWindow,
            AnalyticsTimeWindow trendWindow,
            List<ApiCredentialId> trendCredentialIds
    ) {
        return new FrontKeyMetricsQuery(userAccountId, todayWindow, monthWindow, trendWindow, trendCredentialIds);
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

    public AnalyticsTimeWindow trendWindow() {
        return trendWindow;
    }

    public List<ApiCredentialId> trendCredentialIds() {
        return trendCredentialIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontKeyMetricsQuery that)) {
            return false;
        }
        return Objects.equals(userAccountId, that.userAccountId)
                && Objects.equals(todayWindow, that.todayWindow)
                && Objects.equals(monthWindow, that.monthWindow)
                && Objects.equals(trendWindow, that.trendWindow)
                && Objects.equals(trendCredentialIds, that.trendCredentialIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAccountId, todayWindow, monthWindow, trendWindow, trendCredentialIds);
    }
}
