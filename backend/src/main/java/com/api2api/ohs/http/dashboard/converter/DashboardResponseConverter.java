package com.api2api.ohs.http.dashboard.converter;

import com.api2api.domain.analytics.model.AdminDashboardMetrics;
import com.api2api.domain.analytics.model.ChannelTokenTrendPoint;
import com.api2api.domain.analytics.model.CredentialTokenRanking;
import com.api2api.domain.analytics.model.CredentialTokenTrendPoint;
import com.api2api.domain.analytics.model.FrontDashboardMetrics;
import com.api2api.domain.analytics.model.FrontKeyMetrics;
import com.api2api.domain.analytics.model.ProtocolRequestRate;
import com.api2api.domain.analytics.model.ProtocolTokenTrendPoint;
import com.api2api.domain.analytics.model.TokenAmount;
import com.api2api.domain.analytics.model.UserTokenRanking;
import com.api2api.domain.usage.model.PagedUsageRecords;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.ohs.http.converter.MapStructConfig;
import com.api2api.ohs.http.dashboard.dto.AdminDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.ChannelTokenTrendPointResponse;
import com.api2api.ohs.http.dashboard.dto.CredentialTokenRankingResponse;
import com.api2api.ohs.http.dashboard.dto.CredentialTokenTrendPointResponse;
import com.api2api.ohs.http.dashboard.dto.FrontDashboardRecentCallResponse;
import com.api2api.ohs.http.dashboard.dto.FrontDashboardResponse;
import com.api2api.ohs.http.dashboard.dto.FrontKeyMetricsResponse;
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

    public FrontKeyMetricsResponse toFrontKeyMetricsResponse(FrontKeyMetrics metrics) {
        return FrontKeyMetricsResponse.builder()
                .dailyTopCredentials(metrics.getDailyTopCredentials().stream()
                        .map(this::toCredentialTokenRankingResponse).toList())
                .monthlyTopCredentials(metrics.getMonthlyTopCredentials().stream()
                        .map(this::toCredentialTokenRankingResponse).toList())
                .credentialTokenTrends(metrics.getCredentialTokenTrends().stream()
                        .map(this::toCredentialTokenTrendPointResponse).toList())
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
    @Mapping(target = "apiCredentialId", source = "apiCredentialId.value")
    @Mapping(target = "requestedModel", expression = "java(record.getRequestedModel().value())")
    @Mapping(target = "inputTokens", source = "tokenUsage.inputTokens")
    @Mapping(target = "outputTokens", source = "tokenUsage.outputTokens")
    @Mapping(target = "cacheCreationInputTokens", source = "tokenUsage.cacheCreationInputTokens")
    @Mapping(target = "cacheReadInputTokens", source = "tokenUsage.cacheReadInputTokens")
    @Mapping(target = "totalTokens", source = "tokenUsage.totalTokens")
    @Mapping(target = "actualTokens", source = "tokenUsage.actualTokens")
    @Mapping(target = "usageKnown", source = "tokenUsage.usageKnown")
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

    @Mapping(target = "credentialId", expression = "java(ranking.getCredentialId().value())")
    @Mapping(target = "credentialName", expression = "java(ranking.getCredentialName().value())")
    @Mapping(target = "totalTokens", source = "totalTokens.tokens")
    protected abstract CredentialTokenRankingResponse toCredentialTokenRankingResponse(CredentialTokenRanking ranking);

    @Mapping(target = "credentialId", expression = "java(point.getCredentialId().value())")
    @Mapping(target = "credentialName", expression = "java(point.getCredentialName().value())")
    @Mapping(target = "totalTokens", source = "totalTokens.tokens")
    protected abstract CredentialTokenTrendPointResponse toCredentialTokenTrendPointResponse(CredentialTokenTrendPoint point);
}
