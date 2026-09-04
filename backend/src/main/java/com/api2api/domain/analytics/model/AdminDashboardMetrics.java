package com.api2api.domain.analytics.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Administrative dashboard analytics metrics assembled from usage facts.
 */
public final class AdminDashboardMetrics {

    private static final int TOP_USER_LIMIT = 10;

    private final List<ProtocolRequestRate> protocolRequestRates;
    private final TokenAmount todayTokens;
    private final TokenAmount monthTokens;
    private final List<UserTokenRanking> dailyTopUsers;
    private final List<UserTokenRanking> monthlyTopUsers;
    private final List<ProtocolTokenTrendPoint> protocolTokenTrends;
    private final List<ChannelTokenTrendPoint> channelTokenTrends;
    private final List<ConcurrencyTrendPoint> todayConcurrencyTrends;
    private final List<ChannelLatencyRanking> dailySlowestChannels;

    private AdminDashboardMetrics(
            List<ProtocolRequestRate> protocolRequestRates,
            TokenAmount todayTokens,
            TokenAmount monthTokens,
            List<UserTokenRanking> dailyTopUsers,
            List<UserTokenRanking> monthlyTopUsers,
            List<ProtocolTokenTrendPoint> protocolTokenTrends,
            List<ChannelTokenTrendPoint> channelTokenTrends,
            List<ConcurrencyTrendPoint> todayConcurrencyTrends,
            List<ChannelLatencyRanking> dailySlowestChannels
    ) {
        this.protocolRequestRates = copyRequired(protocolRequestRates, "Protocol request rates");
        ensureProtocolRateCoverage(this.protocolRequestRates);
        this.todayTokens = Objects.requireNonNull(todayTokens, "Admin dashboard today tokens must not be null");
        this.monthTokens = Objects.requireNonNull(monthTokens, "Admin dashboard month tokens must not be null");
        this.dailyTopUsers = copyRequired(dailyTopUsers, "Daily top users");
        this.monthlyTopUsers = copyRequired(monthlyTopUsers, "Monthly top users");
        ensureTopUserLimit(this.dailyTopUsers, "Daily top users");
        ensureTopUserLimit(this.monthlyTopUsers, "Monthly top users");
        this.protocolTokenTrends = copyRequired(protocolTokenTrends, "Protocol token trends");
        this.channelTokenTrends = copyRequired(channelTokenTrends, "Channel token trends");
        this.todayConcurrencyTrends = copyRequired(todayConcurrencyTrends, "Today concurrency trends");
        this.dailySlowestChannels = copyRequired(dailySlowestChannels, "Daily slowest channels");
    }

    public static AdminDashboardMetrics of(
            List<ProtocolRequestRate> protocolRequestRates,
            TokenAmount todayTokens,
            TokenAmount monthTokens,
            List<UserTokenRanking> dailyTopUsers,
            List<UserTokenRanking> monthlyTopUsers,
            List<ProtocolTokenTrendPoint> protocolTokenTrends,
            List<ChannelTokenTrendPoint> channelTokenTrends,
            List<ConcurrencyTrendPoint> todayConcurrencyTrends,
            List<ChannelLatencyRanking> dailySlowestChannels
    ) {
        return new AdminDashboardMetrics(
                protocolRequestRates,
                todayTokens,
                monthTokens,
                dailyTopUsers,
                monthlyTopUsers,
                protocolTokenTrends,
                channelTokenTrends,
                todayConcurrencyTrends,
                dailySlowestChannels
        );
    }

    private static <T> List<T> copyRequired(List<T> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        return values.stream()
                .map(value -> Objects.requireNonNull(value, name + " must not contain null elements"))
                .toList();
    }

    private static void ensureProtocolRateCoverage(List<ProtocolRequestRate> rates) {
        Set<ProtocolType> coveredProtocols = EnumSet.noneOf(ProtocolType.class);
        for (ProtocolRequestRate rate : rates) {
            if (!coveredProtocols.add(rate.protocol())) {
                throw new IllegalArgumentException("Protocol request rates must not contain duplicate protocols");
            }
        }
        if (!coveredProtocols.containsAll(EnumSet.allOf(ProtocolType.class))) {
            throw new IllegalArgumentException("Protocol request rates must cover all supported protocols");
        }
    }

    private static void ensureTopUserLimit(List<UserTokenRanking> rankings, String name) {
        if (rankings.size() > TOP_USER_LIMIT) {
            throw new IllegalArgumentException(name + " must contain at most 10 rows");
        }
    }

    public List<ProtocolRequestRate> protocolRequestRates() {
        return List.copyOf(protocolRequestRates);
    }

    public TokenAmount todayTokens() {
        return todayTokens;
    }

    public TokenAmount monthTokens() {
        return monthTokens;
    }

    public List<UserTokenRanking> dailyTopUsers() {
        return List.copyOf(dailyTopUsers);
    }

    public List<UserTokenRanking> monthlyTopUsers() {
        return List.copyOf(monthlyTopUsers);
    }

    public List<ProtocolTokenTrendPoint> protocolTokenTrends() {
        return List.copyOf(protocolTokenTrends);
    }

    public List<ChannelTokenTrendPoint> channelTokenTrends() {
        return List.copyOf(channelTokenTrends);
    }

    public List<ConcurrencyTrendPoint> todayConcurrencyTrends() {
        return List.copyOf(todayConcurrencyTrends);
    }

    public List<ChannelLatencyRanking> dailySlowestChannels() {
        return List.copyOf(dailySlowestChannels);
    }

    public List<ProtocolRequestRate> getProtocolRequestRates() {
        return protocolRequestRates();
    }

    public TokenAmount getTodayTokens() {
        return todayTokens;
    }

    public TokenAmount getMonthTokens() {
        return monthTokens;
    }

    public List<UserTokenRanking> getDailyTopUsers() {
        return dailyTopUsers();
    }

    public List<UserTokenRanking> getMonthlyTopUsers() {
        return monthlyTopUsers();
    }

    public List<ProtocolTokenTrendPoint> getProtocolTokenTrends() {
        return protocolTokenTrends();
    }

    public List<ChannelTokenTrendPoint> getChannelTokenTrends() {
        return channelTokenTrends();
    }

    public List<ConcurrencyTrendPoint> getTodayConcurrencyTrends() {
        return todayConcurrencyTrends();
    }

    public List<ChannelLatencyRanking> getDailySlowestChannels() {
        return dailySlowestChannels();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdminDashboardMetrics that)) {
            return false;
        }
        return Objects.equals(protocolRequestRates, that.protocolRequestRates)
                && Objects.equals(todayTokens, that.todayTokens)
                && Objects.equals(monthTokens, that.monthTokens)
                && Objects.equals(dailyTopUsers, that.dailyTopUsers)
                && Objects.equals(monthlyTopUsers, that.monthlyTopUsers)
                && Objects.equals(protocolTokenTrends, that.protocolTokenTrends)
                && Objects.equals(channelTokenTrends, that.channelTokenTrends)
                && Objects.equals(todayConcurrencyTrends, that.todayConcurrencyTrends)
                && Objects.equals(dailySlowestChannels, that.dailySlowestChannels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                protocolRequestRates,
                todayTokens,
                monthTokens,
                dailyTopUsers,
                monthlyTopUsers,
                protocolTokenTrends,
                channelTokenTrends,
                todayConcurrencyTrends,
                dailySlowestChannels
        );
    }
}
