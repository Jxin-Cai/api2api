package com.api2api.ohs.http.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin dashboard response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private List<ProtocolRequestRateResponse> protocolRequestRates;
    private TokenAmountResponse todayTokens;
    private TokenAmountResponse monthTokens;
    private List<UserTokenRankingResponse> dailyTopUsers;
    private List<UserTokenRankingResponse> monthlyTopUsers;
    private List<ProtocolTokenTrendPointResponse> protocolTokenTrends;
    private List<ChannelTokenTrendPointResponse> channelTokenTrends;
}
