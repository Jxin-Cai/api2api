package com.api2api.ohs.http.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Front user dashboard response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontDashboardResponse {

    private TokenAmountResponse todayTokens;
    private TokenAmountResponse todayActualTokens;
    private TokenAmountResponse todayTotalTokens;
    private TokenAmountResponse monthTokens;
    private TokenAmountResponse monthActualTokens;
    private TokenAmountResponse monthTotalTokens;
    private long apiKeyCount;
    private List<FrontDashboardRecentCallResponse> recentCalls;
}
