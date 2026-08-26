package com.api2api.ohs.http.dashboard.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API credential token ranking row for the front dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialTokenRankingResponse {

    private int rank;
    private Long credentialId;
    private String credentialName;
    private BigDecimal totalTokens;
}
