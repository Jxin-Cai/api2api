package com.api2api.domain.analytics.model;

import java.util.Objects;

public record UsageDistributionItem(String name, long value) {
    public UsageDistributionItem {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Distribution name must not be blank");
        if (value < 0) throw new IllegalArgumentException("Distribution value must not be negative");
    }
    public String getName() { return name; }
    public long getValue() { return value; }
}
