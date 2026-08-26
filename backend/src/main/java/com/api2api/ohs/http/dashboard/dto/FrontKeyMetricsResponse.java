package com.api2api.ohs.http.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Front per-credential dashboard metrics response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontKeyMetricsResponse {

    private List<CredentialTokenRankingResponse> dailyTopCredentials;
    private List<CredentialTokenRankingResponse> monthlyTopCredentials;
    private List<CredentialTokenTrendPointResponse> credentialTokenTrends;
}
