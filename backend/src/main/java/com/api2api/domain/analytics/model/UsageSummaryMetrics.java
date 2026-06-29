package com.api2api.domain.analytics.model;

import com.api2api.domain.usage.model.UsageTokenBreakdown;
import java.util.Objects;

/**
 * Usage-record count and token total calculated with one filter.
 */
public final class UsageSummaryMetrics {

    private final UsageTokenBreakdown filteredTokenTotal;
    private final long totalRecords;

    private UsageSummaryMetrics(UsageTokenBreakdown filteredTokenTotal, long totalRecords) {
        this.filteredTokenTotal = Objects.requireNonNull(filteredTokenTotal, "Usage summary filtered token total must not be null");
        if (totalRecords < 0) {
            throw new IllegalArgumentException("Usage summary total records must be greater than or equal to 0");
        }
        this.totalRecords = totalRecords;
    }

    public static UsageSummaryMetrics of(UsageTokenBreakdown filteredTokenTotal, long totalRecords) {
        return new UsageSummaryMetrics(filteredTokenTotal, totalRecords);
    }

    public UsageTokenBreakdown filteredTokenTotal() {
        return filteredTokenTotal;
    }

    public long totalRecords() {
        return totalRecords;
    }

    public UsageTokenBreakdown getFilteredTokenTotal() {
        return filteredTokenTotal;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsageSummaryMetrics that)) {
            return false;
        }
        return totalRecords == that.totalRecords
                && Objects.equals(filteredTokenTotal, that.filteredTokenTotal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filteredTokenTotal, totalRecords);
    }
}
