package com.api2api.ohs.http.dashboard.dto;

import com.api2api.domain.channel.model.ProtocolType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Protocol token trend bucket for admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolTokenTrendPointResponse {

    private Instant bucketStart;
    private Instant bucketEnd;
    private ProtocolType protocol;
    private long totalTokens;
}
