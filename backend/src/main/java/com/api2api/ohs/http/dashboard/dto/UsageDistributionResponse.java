package com.api2api.ohs.http.dashboard.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UsageDistributionResponse {
    List<DistributionItem> models;
    List<DistributionItem> channels;

    @Value
    @Builder
    public static class DistributionItem {
        String name;
        long value;
    }
}
