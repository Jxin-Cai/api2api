package com.api2api.domain.usage.model;

import java.util.Objects;
import java.util.Set;

/**
 * Page request specification for usage record queries.
 */
public final class PageRequestSpec {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(50, 100, 200);

    private final int page;
    private final int size;

    private PageRequestSpec(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than or equal to 1");
        }
        if (!ALLOWED_SIZES.contains(size)) {
            throw new IllegalArgumentException("Page size must be one of 50, 100 or 200");
        }
        this.page = page;
        this.size = size;
    }

    public static PageRequestSpec of(int page, int size) {
        return new PageRequestSpec(page, size);
    }

    public long offset() {
        return (long) (page - 1) * size;
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageRequestSpec that)) {
            return false;
        }
        return page == that.page && size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size);
    }
}
