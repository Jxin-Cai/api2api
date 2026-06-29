package com.api2api.ohs.http.usage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query request for current user's usage records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryMyUsageRecordsRequest {

    private Long apiCredentialId;
    private String requestedModel;
    private String requestProtocol;
    private Instant startInclusive;
    private Instant endExclusive;

    @Min(1)
    private Integer page;

    @Min(50)
    @Max(200)
    private Integer size;
}
