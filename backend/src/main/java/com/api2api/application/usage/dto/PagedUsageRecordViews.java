package com.api2api.application.usage.dto;

import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import java.util.List;
import java.util.Objects;

public final class PagedUsageRecordViews {

    private final List<UsageRecordView> records;
    private final int page;
    private final int size;
    private final long totalElements;
    private final UsageTokenBreakdown filteredTokenTotal;

    private PagedUsageRecordViews(
            List<UsageRecordView> records,
            int page,
            int size,
            long totalElements,
            UsageTokenBreakdown filteredTokenTotal
    ) {
        this.records = List.copyOf(Objects.requireNonNull(records, "Usage record views must not be null"));
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.filteredTokenTotal = Objects.requireNonNull(filteredTokenTotal, "Filtered token total must not be null");
    }

    public static PagedUsageRecordViews of(List<UsageRecordView> records, PagedUsageRecords page) {
        return new PagedUsageRecordViews(
                records,
                page.getPage(),
                page.getSize(),
                page.getTotalElements(),
                page.getFilteredTokenTotal()
        );
    }

    public long totalPages() {
        if (totalElements == 0) {
            return 0;
        }
        return (totalElements + size - 1) / size;
    }

    public List<UsageRecordView> getRecords() {
        return records;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public UsageTokenBreakdown getFilteredTokenTotal() {
        return filteredTokenTotal;
    }
}
