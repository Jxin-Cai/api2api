package com.api2api.domain.analytics.repository;

import com.api2api.domain.analytics.model.AnalyticsGranularity;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.ChannelTokenTrendPoint;
import com.api2api.domain.analytics.model.ProtocolRequestRate;
import com.api2api.domain.analytics.model.ProtocolTokenTrendPoint;
import com.api2api.domain.analytics.model.TokenAmount;
import com.api2api.domain.analytics.model.UserTokenRanking;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.UserAccountId;
import java.util.List;

/**
 * Repository contract for dashboard analytics read models derived from persisted usage records.
 */
public interface DashboardAnalyticsRepository {

    /**
     * Sums total tokens for one user inside a left-closed and right-open analytics time window.
     * Implementations must only include usage records whose user account id matches {@code userAccountId} and whose
     * started time is contained in {@code window}; {@code tokenUsage.totalTokens} is the source of truth.
     * Invalid arguments should be rejected as business failures; no matching records should return {@link TokenAmount#zero()}.
     *
     * @param userAccountId user whose records are included
     * @param window analytics time window using left-closed and right-open semantics
     * @return non-null total token amount
     */
    TokenAmount sumUserTotalTokens(UserAccountId userAccountId, AnalyticsTimeWindow window);

    /**
     * Sums platform-wide total tokens inside a left-closed and right-open analytics time window.
     * Implementations must include every persisted usage record in the window, including failed requests that persisted
     * a known token usage; {@code tokenUsage.totalTokens} is the source of truth.
     * Invalid windows should be rejected as business failures; no matching records should return {@link TokenAmount#zero()}.
     *
     * @param window analytics time window using left-closed and right-open semantics
     * @return non-null platform token amount
     */
    TokenAmount sumPlatformTotalTokens(AnalyticsTimeWindow window);

    /**
     * Calculates recent request count and requests-per-minute for every supported protocol.
     * Implementations must group records by request protocol for records whose started time is contained in {@code window},
     * covering {@link ProtocolType#CLAUDE_MESSAGES}, {@link ProtocolType#OPENAI_RESPONSES} and
     * {@link ProtocolType#OPENAI_CHAT_COMPLETIONS}; missing protocols should be returned as zero rates.
     * Invalid windows or non-positive minute windows should be rejected as business failures.
     *
     * @param window analytics time window for rate calculation
     * @return non-null list covering every supported protocol
     */
    List<ProtocolRequestRate> calculateProtocolRequestRates(AnalyticsTimeWindow window);

    /**
     * Finds users with the highest token consumption inside a time window.
     * Implementations must aggregate {@code tokenUsage.totalTokens} by user account, attach the user's username, and sort
     * by total tokens descending and user account id ascending; {@code limit} must be within 1 to 100 and this dashboard
     * uses 10. Invalid arguments should be rejected as business failures; no matching records should return an empty list.
     *
     * @param window analytics time window to aggregate
     * @param limit maximum number of rows to return
     * @return non-null stable ranking rows
     */
    List<UserTokenRanking> findTopUsersByTokens(AnalyticsTimeWindow window, int limit);

    /**
     * Sums token trends by protocol and time bucket.
     * Implementations must split {@code window} into continuous buckets using {@code granularity}, aggregate total tokens
     * by request protocol, and for daily dashboard trends fill every bucket/protocol pair with zero when no records exist.
     * Invalid arguments should be rejected as business failures.
     *
     * @param window analytics time window to bucket
     * @param granularity bucket granularity
     * @return non-null protocol trend points
     */
    List<ProtocolTokenTrendPoint> sumProtocolTokenTrends(AnalyticsTimeWindow window, AnalyticsGranularity granularity);

    /**
     * Sums token trends by provider channel and time bucket.
     * Implementations must split {@code window} into continuous buckets using {@code granularity}, aggregate total tokens
     * by final provider channel id, attach provider channel names, and exclude records without a final provider channel.
     * Invalid arguments should be rejected as business failures; no channel records should return an empty list.
     *
     * @param window analytics time window to bucket
     * @param granularity bucket granularity
     * @return non-null channel trend points for administrative dashboards
     */
    List<ChannelTokenTrendPoint> sumChannelTokenTrends(AnalyticsTimeWindow window, AnalyticsGranularity granularity);

    /**
     * Counts usage records matching a role-aware filter.
     * Implementations must use the same filter conditions and visibility rules as usage-record listing: regular users can
     * count only their own records and cannot filter by provider channel, while administrators can filter by user, API key,
     * model, protocol, provider channel and time range.
     * Invalid filters should be rejected as business failures; no matching records should return {@code 0}.
     *
     * @param filter role-aware usage record filter
     * @return non-negative matching record count
     */
    long countUsageRecords(UsageRecordFilter filter);

    /**
     * Sums token details for usage records matching a role-aware filter.
     * Implementations must use exactly the same filter conditions and visibility rules as {@link #countUsageRecords} and
     * usage-record listing so list totals and summary totals remain consistent.
     * Invalid filters should be rejected as business failures; no matching records should return
     * {@link UsageTokenBreakdown#zeroKnown()}.
     *
     * @param filter role-aware usage record filter
     * @return non-null filtered token breakdown
     */
    UsageTokenBreakdown sumUsageTokens(UsageRecordFilter filter);
}
