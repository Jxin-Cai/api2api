package com.api2api.domain.usage.model;

import java.util.List;
import java.util.Objects;

/**
 * Page of usage records and token totals calculated with the same filter.
 */
public final class PagedUsageRecords {

    private final List<UsageRecord> records;
    private final int page;
    private final int size;
    private final long totalElements;
    private final UsageTokenBreakdown filteredTokenTotal;

    private PagedUsageRecords(
            List<UsageRecord> records,
            int page,
            int size,
            long totalElements,
            UsageTokenBreakdown filteredTokenTotal
    ) {
        Objects.requireNonNull(records, "Usage records must not be null");
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 1");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must be greater than or equal to 0");
        }
        this.records = records.stream()
                .map(record -> Objects.requireNonNull(record, "Usage record must not be null"))
                .toList();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.filteredTokenTotal = Objects.requireNonNull(filteredTokenTotal, "Filtered token total must not be null");
    }

    public static PagedUsageRecords of(
            List<UsageRecord> records,
            int page,
            int size,
            long totalElements,
            UsageTokenBreakdown filteredTokenTotal
    ) {
        return new PagedUsageRecords(records, page, size, totalElements, filteredTokenTotal);
    }

    public static PagedUsageRecords empty(PageRequestSpec pageRequest) {
        PageRequestSpec nonNullPageRequest = Objects.requireNonNull(pageRequest, "Page request must not be null");
        return new PagedUsageRecords(
                List.of(),
                nonNullPageRequest.page(),
                nonNullPageRequest.size(),
                0,
                UsageTokenBreakdown.zeroKnown()
        );
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public long totalPages() {
        if (totalElements == 0) {
            return 0;
        }
        return (totalElements + size - 1) / size;
    }

    public List<UsageRecord> records() {
        return List.copyOf(records);
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public long totalElements() {
        return totalElements;
    }

    public UsageTokenBreakdown filteredTokenTotal() {
        return filteredTokenTotal;
    }

    public List<UsageRecord> getRecords() {
        return records();
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
