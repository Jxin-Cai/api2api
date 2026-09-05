package com.api2api.ohs.http.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider channel ranked by its slowest single response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelLatencyRankingResponse {

    private Integer rank;
    private Long providerChannelId;
    private String providerChannelName;
    private Long maxDurationMillis;
    private Long maxFirstTokenMillis;
    private Long avgFirstTokenMillis;
    private Long avgDurationMillis;
    private Long requestCount;
}
