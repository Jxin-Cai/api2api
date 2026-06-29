package com.api2api.domain.analytics.model;

import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.user.model.UserRole;
import java.util.Objects;

/**
 * Query for usage-record summary metrics with the same visibility as record listing.
 */
public final class UsageSummaryQuery {

    private final UsageRecordFilter filter;
    private final UserRole viewerRole;

    private UsageSummaryQuery(UsageRecordFilter filter, UserRole viewerRole) {
        this.filter = Objects.requireNonNull(filter, "Usage summary filter must not be null");
        this.viewerRole = Objects.requireNonNull(viewerRole, "Usage summary viewer role must not be null");
        if (this.filter.viewerRole() != this.viewerRole) {
            throw new IllegalArgumentException("Usage summary viewer role must match filter viewer role");
        }
    }

    public static UsageSummaryQuery of(UsageRecordFilter filter, UserRole viewerRole) {
        return new UsageSummaryQuery(filter, viewerRole);
    }

    public UsageRecordFilter filter() {
        return filter;
    }

    public UserRole viewerRole() {
        return viewerRole;
    }

    public UsageRecordFilter getFilter() {
        return filter;
    }

    public UserRole getViewerRole() {
        return viewerRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageSummaryQuery that)) {
            return false;
        }
        return Objects.equals(filter, that.filter) && viewerRole == that.viewerRole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filter, viewerRole);
    }
}
