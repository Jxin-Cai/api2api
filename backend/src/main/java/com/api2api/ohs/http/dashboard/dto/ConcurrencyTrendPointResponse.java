package com.api2api.ohs.http.dashboard.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Platform-wide peak concurrency inside one time bucket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrencyTrendPointResponse {

    private Instant bucketStart;
    private Instant bucketEnd;
    private Integer peakConcurrency;
}
