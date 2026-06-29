package com.api2api.ohs.http.dashboard.converter;

import com.api2api.domain.analytics.model.AdminDashboardMetrics;
import com.api2api.domain.analytics.model.ChannelTokenTrendPoint;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.analytics.model.ProtocolRequestRate;
import com.api2api.domain.analytics.model.ProtocolTokenTrendPoint;
import com.api2api.domain.analytics.model.TokenAmount;
import com.api2api.domain.analytics.model.UserTokenRanking;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.dashboard.dto.AdminDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.ChannelTokenTrendPointResponse;
import com.api2api.ohs.http.dashboard.dto.FrontDashboardRecentCallResponse;
import com.api2api.ohs.http.dashboard.dto.FrontDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.ProtocolRequestRateResponse;
import com.api2api.ohs.http.dashboard.dto.ProtocolTokenTrendPointResponse;
import com.api2api.ohs.http.dashboard.dto.TokenAmountResponse;
import com.api2api.ohs.http.dashboard.dto.UserTokenRankingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts dashboard domain metrics to HTTP responses.
 */
@Mapper(config = MapStructConfig.class)
public abstract class DashboardResponseConverter {

    public FrontDashboardResponse toFrontDashboardResponse(
            FrontDashboardMetrics metrics,
            long apiKeyCount,
            PagedUsageRecords recentCalls
    ) {
        return FrontDashboardResponse.builder()
                .todayTokens(toTokenAmountResponse(metrics.getTodayTokens()))
                .monthTokens(toTokenAmountResponse(metrics.getMonthTokens()))
                .apiKeyCount(apiKeyCount)
                .recentCalls(recentCalls.getRecords().stream().map(this::toRecentCallResponse).toList())
                .build();
    }

    public AdminDashboardResponse toAdminDashboardResponse(AdminDashboardMetrics metrics) {
        return AdminDashboardResponse.builder()
                .protocolRequestRates(metrics.getProtocolRequestRates().stream()
                        .map(this::toProtocolRequestRateResponse).toList())
                .todayTokens(toTokenAmountResponse(metrics.getTodayTokens()))
                .monthTokens(toTokenAmountResponse(metrics.getMonthTokens()))
                .dailyTopUsers(metrics.getDailyTopUsers().stream().map(this::toUserTokenRankingResponse).toList())
                .monthlyTopUsers(metrics.getMonthlyTopUsers().stream().map(this::toUserTokenRankingResponse).toList())
                .protocolTokenTrends(metrics.getProtocolTokenTrends().stream()
                        .map(this::toProtocolTokenTrendPointResponse).toList())
                .channelTokenTrends(metrics.getChannelTokenTrends().stream()
                        .map(this::toChannelTokenTrendPointResponse).toList())
                .build();
    }

    @Mapping(target = "tokens", source = "tokens")
    @Mapping(target = "millions", expression = "java(tokenAmount.toMillions())")
    protected abstract TokenAmountResponse toTokenAmountResponse(TokenAmount tokenAmount);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "requestedModel", expression = "java(record.getRequestedModel().value())")
    @Mapping(target = "totalTokens", expression = "java(record.totalTokens())")
    protected abstract FrontDashboardRecentCallResponse toRecentCallResponse(UsageRecord record);

    protected abstract ProtocolRequestRateResponse toProtocolRequestRateResponse(ProtocolRequestRate rate);

    @Mapping(target = "userAccountId", source = "userAccountId.value")
    @Mapping(target = "username", source = "username.value")
    @Mapping(target = "totalTokens", source = "totalTokens.tokens")
    protected abstract UserTokenRankingResponse toUserTokenRankingResponse(UserTokenRanking ranking);

    @Mapping(target = "totalTokens", source = "totalTokens.tokens")
    protected abstract ProtocolTokenTrendPointResponse toProtocolTokenTrendPointResponse(ProtocolTokenTrendPoint point);

    @Mapping(target = "providerChannelId", expression = "java(point.getProviderChannelId().value())")
    @Mapping(target = "providerChannelName", expression = "java(point.getProviderChannelName().value())")
    @Mapping(target = "totalTokens", source = "totalTokens.tokens")
    protected abstract ChannelTokenTrendPointResponse toChannelTokenTrendPointResponse(ChannelTokenTrendPoint point);
}
