package com.api2api.ohs.http.dashboard.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Channel token trend bucket for admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelTokenTrendPointResponse {

    private Instant bucketStart;
    private Instant bucketEnd;
    private Long providerChannelId;
    private String providerChannelName;
    private BigDecimal totalTokens;
}
