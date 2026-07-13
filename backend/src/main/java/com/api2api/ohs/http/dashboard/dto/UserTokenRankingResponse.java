package com.api2api.ohs.http.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * User token ranking row for admin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenRankingResponse {

    private int rank;
    private Long userAccountId;
    private String username;
    private BigDecimal totalTokens;
}
