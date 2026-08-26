package com.api2api.ohs.http.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API credential token trend bucket for the front dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialTokenTrendPointResponse {

    private Instant bucketStart;
    private Instant bucketEnd;
    private Long credentialId;
    private String credentialName;
    private BigDecimal totalTokens;
}
