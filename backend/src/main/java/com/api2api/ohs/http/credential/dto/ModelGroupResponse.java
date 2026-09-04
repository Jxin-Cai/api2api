package com.api2api.ohs.http.credential.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelGroupResponse {
    Long id;
    String name;
    List<String> modelWhitelist;
    /** Per-model daily caps in weighted tokens. */
    Map<String, Long> modelDailyLimits;
    /** Weighted tokens consumed today per capped model, aggregated across every key of the group. */
    Map<String, BigDecimal> modelDailyUsage;
    /** Models whose daily cap is reached today; every key in the group is throttled for them. */
    List<String> rateLimitedModels;
    /** IANA zone that defines the daily-limit calendar day. */
    String dailyLimitZoneId;
    Instant createdAt;
    Instant updatedAt;
}
