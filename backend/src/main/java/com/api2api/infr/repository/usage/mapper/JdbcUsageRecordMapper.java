package com.api2api.infr.repository.usage.mapper;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;

import com.api2api.infr.repository.usage.po.UsageRecordPO;
import com.api2api.infr.repository.usage.po.UsageRecordQueryPO;
import com.api2api.infr.repository.usage.po.UsageTokenSummaryPO;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcUsageRecordMapper implements UsageRecordMapper {

    private static final String ACTUAL_TOKENS_SQL = "input_tokens::numeric + output_tokens::numeric * 5 + cache_read_input_tokens::numeric * 0.1 + cache_creation_input_tokens::numeric * 1.25";

    private static final String COLUMNS = "id, request_id, user_account_id, api_credential_id, requested_model, upstream_model, request_protocol, upstream_protocol, provider_channel_id, status, input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens, total_tokens, usage_known, streaming, started_at, ended_at, duration_millis, error_type, error_message, route_failures_json, created_at, updated_at, deleted";

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<UsageRecordPO> rowMapper = (rs, rowNum) -> UsageRecordPO.builder()
            .id(rs.getLong("id"))
            .requestId(rs.getString("request_id"))
            .userAccountId(rs.getLong("user_account_id"))
            .apiCredentialId(rs.getLong("api_credential_id"))
            .requestedModel(rs.getString("requested_model"))
            .upstreamModel(rs.getString("upstream_model"))
            .requestProtocol(rs.getString("request_protocol"))
            .upstreamProtocol(rs.getString("upstream_protocol"))
            .providerChannelId(nullableLong(rs, "provider_channel_id"))
            .status(rs.getString("status"))
            .inputTokens(rs.getLong("input_tokens"))
            .outputTokens(rs.getLong("output_tokens"))
            .cacheCreationInputTokens(rs.getLong("cache_creation_input_tokens"))
            .cacheReadInputTokens(rs.getLong("cache_read_input_tokens"))
            .totalTokens(rs.getLong("total_tokens"))
            .usageKnown(rs.getBoolean("usage_known"))
            .streaming(rs.getBoolean("streaming"))
            .startedTime(instant(rs, "started_at"))
            .endedTime(instant(rs, "ended_at"))
            .durationMillis(rs.getLong("duration_millis"))
            .errorType(rs.getString("error_type"))
            .errorMessage(rs.getString("error_message"))
            .routeFailuresJson(rs.getString("route_failures_json"))
            .createdTime(instant(rs, "created_at"))
            .updatedTime(instant(rs, "updated_at"))
            .deleted(rs.getBoolean("deleted"))
            .build();

    private final RowMapper<UsageTokenSummaryPO> tokenSummaryRowMapper = (rs, rowNum) -> UsageTokenSummaryPO.builder()
            .inputTokens(rs.getLong("input_tokens"))
            .outputTokens(rs.getLong("output_tokens"))
            .cacheCreationInputTokens(rs.getLong("cache_creation_input_tokens"))
            .cacheReadInputTokens(rs.getLong("cache_read_input_tokens"))
            .totalTokens(rs.getLong("total_tokens"))
            .usageKnown(rs.getBoolean("usage_known"))
            .build();

    @Override
    public int insert(UsageRecordPO usageRecord) {
        return jdbcTemplate.update("""
                INSERT INTO usage_records (id, request_id, user_account_id, api_credential_id, requested_model, upstream_model, request_protocol, upstream_protocol, provider_channel_id, status, input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens, total_tokens, usage_known, streaming, started_at, ended_at, duration_millis, error_type, error_message, route_failures_json, created_at, updated_at, deleted)
                VALUES (:id, :requestId, :userAccountId, :apiCredentialId, :requestedModel, :upstreamModel, :requestProtocol, :upstreamProtocol, :providerChannelId, :status, :inputTokens, :outputTokens, :cacheCreationInputTokens, :cacheReadInputTokens, :totalTokens, :usageKnown, :streaming, :startedTime, :endedTime, :durationMillis, :errorType, :errorMessage, :routeFailuresJson, :createdTime, :updatedTime, :deleted)
                """, params(usageRecord));
    }

    @Override
    public UsageRecordPO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM usage_records WHERE id = :id AND deleted = FALSE",
                Map.of("id", id), rowMapper));
    }

    @Override
    public UsageRecordPO selectByRequestId(String requestId) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM usage_records WHERE request_id = :requestId AND deleted = FALSE",
                Map.of("requestId", requestId), rowMapper));
    }

    @Override
    public List<UsageRecordPO> selectByFilter(UsageRecordQueryPO query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = whereClause(query, params);
        int pageSize = query.getPageSize() <= 0 ? 50 : query.getPageSize();
        long offset = Math.max(0, (long) (Math.max(1, query.getPageNumber()) - 1) * pageSize);
        params.addValue("pageSize", pageSize);
        params.addValue("offset", offset);
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM usage_records " + where + " ORDER BY started_at DESC, id DESC LIMIT :pageSize OFFSET :offset",
                params,
                rowMapper
        );
    }

    @Override
    public long countByFilter(UsageRecordQueryPO query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_records " + whereClause(query, params), params, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long sumTotalTokensByApiCredential(Long apiCredentialId) {
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(total_tokens), 0)
                FROM usage_records
                WHERE api_credential_id = :apiCredentialId AND deleted = FALSE
                """, Map.of("apiCredentialId", apiCredentialId), Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public BigDecimal sumActualTokensByApiCredential(Long apiCredentialId) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(" + ACTUAL_TOKENS_SQL + "), 0) FROM usage_records "
                        + "WHERE api_credential_id = :apiCredentialId AND deleted = FALSE",
                Map.of("apiCredentialId", apiCredentialId),
                BigDecimal.class
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public UsageTokenSummaryPO sumTokensByFilter(UsageRecordQueryPO query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        return DataAccessUtils.singleResult(jdbcTemplate.query("""
                SELECT COALESCE(SUM(input_tokens), 0) AS input_tokens,
                       COALESCE(SUM(output_tokens), 0) AS output_tokens,
                       COALESCE(SUM(cache_creation_input_tokens), 0) AS cache_creation_input_tokens,
                       COALESCE(SUM(cache_read_input_tokens), 0) AS cache_read_input_tokens,
                       COALESCE(SUM(total_tokens), 0) AS total_tokens,
                       COALESCE(BOOL_AND(usage_known), TRUE) AS usage_known
                FROM usage_records
                """ + whereClause(query, params), params, tokenSummaryRowMapper));
    }

    private String whereClause(UsageRecordQueryPO query, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("deleted = FALSE");
        if (query.getUserId() != null) {
            conditions.add("user_account_id = :userId");
            params.addValue("userId", query.getUserId());
        }
        if (query.getApiCredentialId() != null) {
            conditions.add("api_credential_id = :apiCredentialId");
            params.addValue("apiCredentialId", query.getApiCredentialId());
        }
        if (query.getProviderChannelId() != null) {
            conditions.add("provider_channel_id = :providerChannelId");
            params.addValue("providerChannelId", query.getProviderChannelId());
        }
        if (query.getModel() != null && !query.getModel().isBlank()) {
            conditions.add("requested_model = :model");
            params.addValue("model", query.getModel());
        }
        if (query.getProtocol() != null && !query.getProtocol().isBlank()) {
            conditions.add("request_protocol = :protocol");
            params.addValue("protocol", query.getProtocol());
        }
        if (query.getStartTime() != null) {
            conditions.add("started_at >= :startTime");
            params.addValue("startTime", timestamp(query.getStartTime()));
        }
        if (query.getEndTime() != null) {
            conditions.add("started_at < :endTime");
            params.addValue("endTime", timestamp(query.getEndTime()));
        }
        return "WHERE " + String.join(" AND ", conditions);
    }

    private MapSqlParameterSource params(UsageRecordPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("requestId", po.getRequestId())
                .addValue("userAccountId", po.getUserAccountId())
                .addValue("apiCredentialId", po.getApiCredentialId())
                .addValue("requestedModel", po.getRequestedModel())
                .addValue("upstreamModel", po.getUpstreamModel())
                .addValue("requestProtocol", po.getRequestProtocol())
                .addValue("upstreamProtocol", po.getUpstreamProtocol())
                .addValue("providerChannelId", po.getProviderChannelId())
                .addValue("status", po.getStatus())
                .addValue("inputTokens", po.getInputTokens())
                .addValue("outputTokens", po.getOutputTokens())
                .addValue("cacheCreationInputTokens", po.getCacheCreationInputTokens())
                .addValue("cacheReadInputTokens", po.getCacheReadInputTokens())
                .addValue("totalTokens", po.getTotalTokens())
                .addValue("usageKnown", po.isUsageKnown())
                .addValue("streaming", po.isStreaming())
                .addValue("startedTime", timestamp(po.getStartedTime()))
                .addValue("endedTime", timestamp(po.getEndedTime()))
                .addValue("durationMillis", po.getDurationMillis())
                .addValue("errorType", po.getErrorType())
                .addValue("errorMessage", po.getErrorMessage())
                .addValue("routeFailuresJson", po.getRouteFailuresJson())
                .addValue("createdTime", timestamp(po.getCreatedTime()))
                .addValue("updatedTime", timestamp(po.getUpdatedTime()))
                .addValue("deleted", po.isDeleted());
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

}
