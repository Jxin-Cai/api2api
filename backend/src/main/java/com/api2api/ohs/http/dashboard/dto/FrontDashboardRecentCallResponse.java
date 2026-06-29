package com.api2api.ohs.http.dashboard.dto;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.usage.model.UsageRecordStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recent call row shown on the front dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontDashboardRecentCallResponse {

    private Long id;
    private String requestedModel;
    private ProtocolType requestProtocol;
    private UsageRecordStatus status;
    private long totalTokens;
    private Instant startedAt;
    private Instant endedAt;
}
