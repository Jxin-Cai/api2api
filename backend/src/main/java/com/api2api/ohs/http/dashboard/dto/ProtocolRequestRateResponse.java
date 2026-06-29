package com.api2api.ohs.http.dashboard.dto;

import com.api2api.domain.channel.model.ProtocolType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Protocol request rate row for admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolRequestRateResponse {

    private ProtocolType protocol;
    private long requestCount;
    private double requestsPerMinute;
}
