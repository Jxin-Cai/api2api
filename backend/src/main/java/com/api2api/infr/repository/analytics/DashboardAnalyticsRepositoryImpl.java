package com.api2api.infr.repository.analytics;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;
import static com.api2api.infr.repository.common.UsageTokenSqlFragments.ACTUAL_TOKENS_SQL;
import static com.api2api.infr.repository.common.UsageTokenSqlFragments.withPrefix;

import com.api2api.domain.analytics.model.AnalyticsGranularity;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.ChannelTokenTrendPoint;
import com.api2api.domain.analytics.model.CredentialTokenRanking;
import com.api2api.domain.analytics.model.CredentialTokenTrendPoint;
import com.api2api.domain.analytics.model.ProtocolRequestRate;
import com.api2api.domain.analytics.model.ProtocolTokenTrendPoint;
import com.api2api.domain.analytics.model.TokenAmount;
import com.api2api.domain.analytics.model.UserTokenRanking;
import com.api2api.domain.analytics.repository.DashboardAnalyticsRepository;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.usage.model.UsageRecordFilter;
import com.api2api.domain.usage.model.UsageTokenBreakdown;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.Username;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DashboardAnalyticsRepositoryImpl implements DashboardAnalyticsRepository {

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public TokenAmount sumUserTotalTokens(UserAccountId userAccountId, AnalyticsTimeWindow window) {
        return TokenAmount.of(sumUserTokens(userAccountId, window).actualTokens());
    }

    @Override
    public UsageTokenBreakdown sumUserTokens(UserAccountId userAccountId, AnalyticsTimeWindow window) {
        Objects.requireNonNull(userAccountId, "User account id must not be null");
        Objects.requireNonNull(window, "Analytics time window must not be null");
        UsageTokenBreakdown breakdown = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(input_tokens), 0) AS input_tokens,
                       COALESCE(SUM(output_tokens), 0) AS output_tokens,
                       COALESCE(SUM(cache_creation_input_tokens), 0) AS cache_creation_input_tokens,
                       COALESCE(SUM(cache_read_input_tokens), 0) AS cache_read_input_tokens,
                       COALESCE(BOOL_AND(usage_known), TRUE) AS usage_known
                FROM usage_records
                WHERE deleted = FALSE
                  AND user_account_id = :userAccountId
                  AND started_at >= :startTime
                  AND started_at < :endTime
                """, windowParams(window).addValue("userAccountId", userAccountId.getValue()), (rs, rowNum) -> {
            long inputTokens = rs.getLong("input_tokens");
            long outputTokens = rs.getLong("output_tokens");
            long cacheCreationInputTokens = rs.getLong("cache_creation_input_tokens");
            long cacheReadInputTokens = rs.getLong("cache_read_input_tokens");
            return UsageTokenBreakdown.of(
                    inputTokens,
                    outputTokens,
                    cacheCreationInputTokens,
                    cacheReadInputTokens,
                    inputTokens + outputTokens + cacheCreationInputTokens + cacheReadInputTokens,
                    rs.getBoolean("usage_known")
            );
        });
        return breakdown == null ? UsageTokenBreakdown.zeroKnown() : breakdown;
    }

    @Override
    public TokenAmount sumPlatformTotalTokens(AnalyticsTimeWindow window) {
        Objects.requireNonNull(window, "Analytics time window must not be null");
        String sql = "SELECT COALESCE(SUM(" + ACTUAL_TOKENS_SQL + "), 0) FROM usage_records "
                + "WHERE deleted = FALSE AND started_at >= :startTime AND started_at < :endTime";
        BigDecimal total = jdbcTemplate.queryForObject(sql, windowParams(window), BigDecimal.class);
        return TokenAmount.of(total == null ? BigDecimal.ZERO : total);
    }

    @Override
    public List<ProtocolRequestRate> calculateProtocolRequestRates(AnalyticsTimeWindow window) {
        Objects.requireNonNull(window, "Analytics time window must not be null");
        Map<ProtocolType, Long> counts = new EnumMap<>(ProtocolType.class);
        jdbcTemplate.query("""
                SELECT request_protocol, COUNT(*) AS request_count
                FROM usage_records
                WHERE deleted = FALSE
                  AND started_at >= :startTime
                  AND started_at < :endTime
                GROUP BY request_protocol
                """, windowParams(window), rs -> {
            counts.put(ProtocolType.valueOf(rs.getString("request_protocol")), rs.getLong("request_count"));
        });
        List<ProtocolRequestRate> rates = new ArrayList<>();
        for (ProtocolType protocol : ProtocolType.values()) {
            rates.add(ProtocolRequestRate.calculate(protocol, window, counts.getOrDefault(protocol, 0L)));
        }
        return rates;
    }

    @Override
    public List<UserTokenRanking> findTopUsersByTokens(AnalyticsTimeWindow window, int limit) {
        Objects.requireNonNull(window, "Analytics time window must not be null");
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Top user limit must be between 1 and 100");
        }
        MapSqlParameterSource params = windowParams(window).addValue("limit", limit);
        List<UserTokenRow> rows = jdbcTemplate.query("""
                SELECT u.id AS user_account_id,
                       u.username AS username,
                       COALESCE(SUM(r.total_tokens), 0) AS total_tokens
                FROM usage_records r
                JOIN user_accounts u ON u.id = r.user_account_id
                WHERE r.deleted = FALSE
                  AND u.deleted = FALSE
                  AND r.started_at >= :startTime
                  AND r.started_at < :endTime
                GROUP BY u.id, u.username
                ORDER BY total_tokens DESC, u.id ASC
                LIMIT :limit
                """, params, (rs, rowNum) -> new UserTokenRow(
                rs.getLong("user_account_id"),
                rs.getString("username"),
                rs.getBigDecimal("total_tokens")
        ));
        List<UserTokenRanking> rankings = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            UserTokenRow row = rows.get(index);
            rankings.add(UserTokenRanking.of(
                    index + 1,
                    UserAccountId.of(row.userAccountId()),
                    Username.of(row.username()),
                    TokenAmount.of(row.totalTokens())
            ));
        }
        return rankings;
    }

    @Override
    public List<ProtocolTokenTrendPoint> sumProtocolTokenTrends(AnalyticsTimeWindow window, AnalyticsGranularity granularity) {
        Objects.requireNonNull(window, "Analytics time window must not be null");
        AnalyticsGranularity.requireSupported(granularity);
        List<Bucket> buckets = buckets(window, granularity);
        Map<ProtocolBucketKey, BigDecimal> totals = new LinkedHashMap<>();
        String sql = "SELECT request_protocol, started_at, " + ACTUAL_TOKENS_SQL + " AS actual_tokens "
                + "FROM usage_records WHERE deleted = FALSE "
                + "AND started_at >= :startTime AND started_at < :endTime";
        jdbcTemplate.query(sql, windowParams(window), rs -> {
            ProtocolType protocol = ProtocolType.valueOf(rs.getString("request_protocol"));
            Instant startedAt = instant(rs, "started_at");
            Bucket bucket = findBucket(buckets, startedAt);
            if (bucket != null) {
                ProtocolBucketKey key = new ProtocolBucketKey(protocol, bucket.start());
                totals.merge(key, rs.getBigDecimal("actual_tokens"), BigDecimal::add);
            }
        });
        List<ProtocolTokenTrendPoint> points = new ArrayList<>();
        for (Bucket bucket : buckets) {
            for (ProtocolType protocol : ProtocolType.values()) {
                BigDecimal total = totals.getOrDefault(new ProtocolBucketKey(protocol, bucket.start()), BigDecimal.ZERO);
                points.add(total.signum() == 0
                        ? ProtocolTokenTrendPoint.zero(bucket.start(), bucket.end(), protocol)
                        : ProtocolTokenTrendPoint.of(bucket.start(), bucket.end(), protocol, TokenAmount.of(total)));
            }
        }
        return points;
    }

    @Override
    public List<ChannelTokenTrendPoint> sumChannelTokenTrends(AnalyticsTimeWindow window, AnalyticsGranularity granularity) {
        Objects.requireNonNull(window, "Analytics time window must not be null");
        AnalyticsGranularity.requireSupported(granularity);
        List<Bucket> buckets = buckets(window, granularity);
        Map<ChannelBucketKey, ChannelBucketTotal> totals = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT r.provider_channel_id,
                       COALESCE(c.name, 'Unknown Channel') AS provider_channel_name,
                       r.started_at,
                       %s AS actual_tokens
                FROM usage_records r
                LEFT JOIN provider_channels c ON c.id = r.provider_channel_id
                WHERE r.deleted = FALSE
                  AND r.provider_channel_id IS NOT NULL
                  AND r.started_at >= :startTime
                  AND r.started_at < :endTime
                """.formatted(withPrefix("r.")), windowParams(window), rs -> {
            Instant startedAt = instant(rs, "started_at");
            Bucket bucket = findBucket(buckets, startedAt);
            if (bucket != null) {
                long channelId = rs.getLong("provider_channel_id");
                String channelName = rs.getString("provider_channel_name");
                BigDecimal totalTokens = rs.getBigDecimal("actual_tokens");
                ChannelBucketKey key = new ChannelBucketKey(channelId, bucket.start());
                totals.compute(key, (ignored, existing) -> existing == null
                        ? new ChannelBucketTotal(channelId, channelName, bucket.start(), bucket.end(), totalTokens)
                        : existing.plus(totalTokens));
            }
        });
        return totals.values().stream()
                .map(total -> ChannelTokenTrendPoint.of(
                        total.bucketStart(),
                        total.bucketEnd(),
                        ProviderChannelId.of(total.providerChannelId()),
                        ProviderChannelName.of(total.providerChannelName()),
                        TokenAmount.of(total.actualTokens())
                ))
                .toList();
    }

    @Override
    public List<CredentialTokenRanking> findTopCredentialsByTokens(
            UserAccountId userAccountId,
            AnalyticsTimeWindow window,
            int limit
    ) {
        Objects.requireNonNull(userAccountId, "User account id must not be null");
        Objects.requireNonNull(window, "Analytics time window must not be null");
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Top credential limit must be between 1 and 100");
        }
        MapSqlParameterSource params = windowParams(window)
                .addValue("userAccountId", userAccountId.getValue())
                .addValue("limit", limit);
        List<CredentialTokenRow> rows = jdbcTemplate.query("""
                SELECT k.id AS api_credential_id,
                       k.name AS credential_name,
                       COALESCE(SUM(r.total_tokens), 0) AS total_tokens
                FROM usage_records r
                JOIN api_credentials k ON k.id = r.api_credential_id
                WHERE r.deleted = FALSE
                  AND k.deleted = FALSE
                  AND r.user_account_id = :userAccountId
                  AND r.started_at >= :startTime
                  AND r.started_at < :endTime
                GROUP BY k.id, k.name
                ORDER BY total_tokens DESC, k.id ASC
                LIMIT :limit
                """, params, (rs, rowNum) -> new CredentialTokenRow(
                rs.getLong("api_credential_id"),
                rs.getString("credential_name"),
                rs.getBigDecimal("total_tokens")
        ));
        List<CredentialTokenRanking> rankings = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            CredentialTokenRow row = rows.get(index);
            rankings.add(CredentialTokenRanking.of(
                    index + 1,
                    ApiCredentialId.of(row.apiCredentialId()),
                    ApiCredentialName.of(row.credentialName()),
                    TokenAmount.of(row.totalTokens())
            ));
        }
        return rankings;
    }

    @Override
    public List<CredentialTokenTrendPoint> sumCredentialTokenTrends(
            UserAccountId userAccountId,
            List<ApiCredentialId> credentialIds,
            AnalyticsTimeWindow window,
            AnalyticsGranularity granularity
    ) {
        Objects.requireNonNull(userAccountId, "User account id must not be null");
        Objects.requireNonNull(credentialIds, "Credential ids must not be null");
        Objects.requireNonNull(window, "Analytics time window must not be null");
        AnalyticsGranularity.requireSupported(granularity);

        Map<Long, String> credentialNames = loadCredentialNames(userAccountId, credentialIds);
        List<Bucket> buckets = buckets(window, granularity);
        Map<CredentialBucketKey, BigDecimal> totals = new LinkedHashMap<>();

        MapSqlParameterSource params = windowParams(window).addValue("userAccountId", userAccountId.getValue());
        String credentialCondition = "";
        if (!credentialIds.isEmpty()) {
            credentialCondition = " AND r.api_credential_id IN (:credentialIds)";
            params.addValue("credentialIds", credentialIds.stream().map(ApiCredentialId::value).toList());
        }
        String sql = ("""
                SELECT r.api_credential_id,
                       r.started_at,
                       %s AS actual_tokens
                FROM usage_records r
                WHERE r.deleted = FALSE
                  AND r.user_account_id = :userAccountId
                  AND r.started_at >= :startTime
                  AND r.started_at < :endTime
                """).formatted(withPrefix("r.")) + credentialCondition;
        jdbcTemplate.query(sql, params, rs -> {
            Instant startedAt = instant(rs, "started_at");
            Bucket bucket = findBucket(buckets, startedAt);
            if (bucket != null) {
                long credentialId = rs.getLong("api_credential_id");
                CredentialBucketKey key = new CredentialBucketKey(credentialId, bucket.start());
                totals.merge(key, rs.getBigDecimal("actual_tokens"), BigDecimal::add);
            }
        });

        List<Long> orderedCredentialIds = new ArrayList<>(credentialNames.keySet());
        List<CredentialTokenTrendPoint> points = new ArrayList<>();
        for (Bucket bucket : buckets) {
            for (Long credentialId : orderedCredentialIds) {
                ApiCredentialId id = ApiCredentialId.of(credentialId);
                ApiCredentialName name = ApiCredentialName.of(credentialNames.get(credentialId));
                BigDecimal total = totals.getOrDefault(new CredentialBucketKey(credentialId, bucket.start()), BigDecimal.ZERO);
                points.add(total.signum() == 0
                        ? CredentialTokenTrendPoint.zero(bucket.start(), bucket.end(), id, name)
                        : CredentialTokenTrendPoint.of(bucket.start(), bucket.end(), id, name, TokenAmount.of(total)));
            }
        }
        return points;
    }

    private Map<Long, String> loadCredentialNames(UserAccountId userAccountId, List<ApiCredentialId> credentialIds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ownerUserId", userAccountId.getValue());
        String condition = "";
        if (!credentialIds.isEmpty()) {
            condition = " AND id IN (:credentialIds)";
            params.addValue("credentialIds", credentialIds.stream().map(ApiCredentialId::value).toList());
        }
        Map<Long, String> names = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT id, name FROM api_credentials WHERE deleted = FALSE AND owner_user_id = :ownerUserId"
                        + condition + " ORDER BY id ASC",
                params,
                rs -> {
                    names.put(rs.getLong("id"), rs.getString("name"));
                }
        );
        return names;
    }

    @Override
    public long countUsageRecords(UsageRecordFilter filter) {
        Objects.requireNonNull(filter, "Usage record filter must not be null");
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_records " + whereClause(filter, params), params, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public UsageTokenBreakdown sumUsageTokens(UsageRecordFilter filter) {
        Objects.requireNonNull(filter, "Usage record filter must not be null");
        MapSqlParameterSource params = new MapSqlParameterSource();
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(input_tokens), 0) AS input_tokens,
                       COALESCE(SUM(output_tokens), 0) AS output_tokens,
                       COALESCE(SUM(cache_creation_input_tokens), 0) AS cache_creation_input_tokens,
                       COALESCE(SUM(cache_read_input_tokens), 0) AS cache_read_input_tokens,
                       COALESCE(SUM(total_tokens), 0) AS total_tokens,
                       COALESCE(BOOL_AND(usage_known), TRUE) AS usage_known
                FROM usage_records
                """ + whereClause(filter, params), params, (rs, rowNum) -> UsageTokenBreakdown.of(
                rs.getLong("input_tokens"),
                rs.getLong("output_tokens"),
                rs.getLong("cache_creation_input_tokens"),
                rs.getLong("cache_read_input_tokens"),
                rs.getLong("total_tokens"),
                rs.getBoolean("usage_known")
        ));
    }

    private String whereClause(UsageRecordFilter filter, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("deleted = FALSE");
        conditions.add("started_at >= :startTime");
        conditions.add("started_at < :endTime");
        params.addValue("startTime", timestamp(filter.timeRange().startInclusive()));
        params.addValue("endTime", timestamp(filter.timeRange().endExclusive()));
        if (filter.userAccountId() != null) {
            conditions.add("user_account_id = :userAccountId");
            params.addValue("userAccountId", filter.userAccountId().getValue());
        }
        if (filter.apiCredentialId() != null) {
            conditions.add("api_credential_id = :apiCredentialId");
            params.addValue("apiCredentialId", filter.apiCredentialId().value());
        }
        if (filter.providerChannelId() != null) {
            conditions.add("provider_channel_id = :providerChannelId");
            params.addValue("providerChannelId", filter.providerChannelId().value());
        }
        if (filter.requestedModel() != null) {
            conditions.add("requested_model = :requestedModel");
            params.addValue("requestedModel", filter.requestedModel().value());
        }
        if (filter.requestProtocol() != null) {
            conditions.add("request_protocol = :requestProtocol");
            params.addValue("requestProtocol", filter.requestProtocol().name());
        }
        return "WHERE " + String.join(" AND ", conditions);
    }

    private MapSqlParameterSource windowParams(AnalyticsTimeWindow window) {
        return new MapSqlParameterSource()
                .addValue("startTime", timestamp(window.startInclusive()))
                .addValue("endTime", timestamp(window.endExclusive()));
    }

    private List<Bucket> buckets(AnalyticsTimeWindow window, AnalyticsGranularity granularity) {
        TemporalUnit unit = switch (granularity) {
            case DAY -> ChronoUnit.DAYS;
            case HOUR -> ChronoUnit.HOURS;
            case MINUTE -> ChronoUnit.MINUTES;
        };
        List<Bucket> buckets = new ArrayList<>();
        Instant start = window.startInclusive();
        while (start.isBefore(window.endExclusive())) {
            Instant next = start.plus(1, unit);
            Instant end = next.isAfter(window.endExclusive()) ? window.endExclusive() : next;
            buckets.add(new Bucket(start, end));
            start = end;
        }
        return buckets;
    }

    private Bucket findBucket(List<Bucket> buckets, Instant instant) {
        for (Bucket bucket : buckets) {
            if (!instant.isBefore(bucket.start()) && instant.isBefore(bucket.end())) {
                return bucket;
            }
        }
        return null;
    }


    private record Bucket(Instant start, Instant end) {
    }

    private record ProtocolBucketKey(ProtocolType protocol, Instant bucketStart) {
    }

    private record ChannelBucketKey(long providerChannelId, Instant bucketStart) {
    }

    private record CredentialBucketKey(long apiCredentialId, Instant bucketStart) {
    }

    private record CredentialTokenRow(long apiCredentialId, String credentialName, BigDecimal totalTokens) {
    }

    private record UserTokenRow(long userAccountId, String username, BigDecimal totalTokens) {
    }

    private record ChannelBucketTotal(
            long providerChannelId,
            String providerChannelName,
            Instant bucketStart,
            Instant bucketEnd,
            BigDecimal actualTokens
    ) {
        private ChannelBucketTotal plus(BigDecimal tokens) {
            return new ChannelBucketTotal(providerChannelId, providerChannelName, bucketStart, bucketEnd, actualTokens.add(tokens));
        }
    }
}
