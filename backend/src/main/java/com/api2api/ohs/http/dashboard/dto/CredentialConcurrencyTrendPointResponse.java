package com.api2api.ohs.http.dashboard.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Peak concurrency of one API credential inside one time bucket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialConcurrencyTrendPointResponse {

    private Instant bucketStart;
    private Instant bucketEnd;
    private Long credentialId;
    private String credentialName;
    private Integer peakConcurrency;
}
