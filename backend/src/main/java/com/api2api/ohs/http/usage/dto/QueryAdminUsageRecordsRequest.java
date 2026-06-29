package com.api2api.ohs.http.usage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query request for admin usage records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryAdminUsageRecordsRequest {

    private Long userAccountId;
    private Long apiCredentialId;
    private String requestedModel;
    private Long providerChannelId;
    private String requestProtocol;
    private Instant startInclusive;
    private Instant endExclusive;

    @Min(1)
    private Integer page;

    @Min(50)
    @Max(200)
    private Integer size;
}
