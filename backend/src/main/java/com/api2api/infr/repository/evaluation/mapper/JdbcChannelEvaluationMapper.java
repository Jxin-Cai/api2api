package com.api2api.infr.repository.evaluation.mapper;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;

import com.api2api.domain.evaluation.repository.EvaluationHistoryQuery;
import com.api2api.domain.evaluation.repository.EvaluationSortField;
import com.api2api.infr.repository.evaluation.po.ChannelEvaluationPO;
import com.api2api.infr.repository.evaluation.po.EvaluationScoreSummaryPO;
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
public class JdbcChannelEvaluationMapper implements ChannelEvaluationMapper {

    private static final String COLUMNS = """
            id, provider_channel_id, requested_model, upstream_format, provider_run_id, status, trigger_type,
            score, detected_family, detected_model, detected_confidence, family_mismatch, channel_signature,
            report_url, passed_probe_count, warning_probe_count, failed_probe_count, total_input_tokens,
            total_output_tokens, error_message, report_summary, requested_at, started_at, completed_at,
            created_at, updated_at
            """;

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ChannelEvaluationPO> rowMapper = (rs, rowNum) -> ChannelEvaluationPO.builder()
            .id(rs.getLong("id"))
            .providerChannelId(rs.getLong("provider_channel_id"))
            .requestedModel(rs.getString("requested_model"))
            .upstreamFormat(rs.getString("upstream_format"))
            .providerRunId(rs.getString("provider_run_id"))
            .status(rs.getString("status"))
            .triggerType(rs.getString("trigger_type"))
            .score(decimal(rs, "score"))
            .detectedFamily(rs.getString("detected_family"))
            .detectedModel(rs.getString("detected_model"))
            .detectedConfidence(decimal(rs, "detected_confidence"))
            .familyMismatch(nullableBoolean(rs, "family_mismatch"))
            .channelSignature(rs.getString("channel_signature"))
            .reportUrl(rs.getString("report_url"))
            .passedProbeCount(nullableInteger(rs, "passed_probe_count"))
            .warningProbeCount(nullableInteger(rs, "warning_probe_count"))
            .failedProbeCount(nullableInteger(rs, "failed_probe_count"))
            .totalInputTokens(nullableLong(rs, "total_input_tokens"))
            .totalOutputTokens(nullableLong(rs, "total_output_tokens"))
            .errorMessage(rs.getString("error_message"))
            .reportSummary(rs.getString("report_summary"))
            .requestedAt(instant(rs, "requested_at"))
            .startedAt(instant(rs, "started_at"))
            .completedAt(instant(rs, "completed_at"))
            .createdAt(instant(rs, "created_at"))
            .updatedAt(instant(rs, "updated_at"))
            .build();

    private final RowMapper<EvaluationScoreSummaryPO> summaryRowMapper = (rs, rowNum) -> EvaluationScoreSummaryPO.builder()
            .totalCount(rs.getLong("total_count"))
            .scoredCount(rs.getLong("scored_count"))
            .failedCount(rs.getLong("failed_count"))
            .averageScore(decimal(rs, "average_score"))
            .minScore(decimal(rs, "min_score"))
            .maxScore(decimal(rs, "max_score"))
            .build();

    @Override
    public int insert(ChannelEvaluationPO evaluation) {
        return jdbcTemplate.update("""
                INSERT INTO channel_evaluations (
                    id, provider_channel_id, requested_model, upstream_format, provider_run_id, status, trigger_type,
                    score, detected_family, detected_model, detected_confidence, family_mismatch, channel_signature,
                    report_url, passed_probe_count, warning_probe_count, failed_probe_count, total_input_tokens,
                    total_output_tokens, error_message, report_summary, requested_at, started_at, completed_at,
                    created_at, updated_at
                ) VALUES (
                    :id, :providerChannelId, :requestedModel, :upstreamFormat, :providerRunId, :status, :triggerType,
                    :score, :detectedFamily, :detectedModel, :detectedConfidence, :familyMismatch, :channelSignature,
                    :reportUrl, :passedProbeCount, :warningProbeCount, :failedProbeCount, :totalInputTokens,
                    :totalOutputTokens, :errorMessage, :reportSummary, :requestedAt, :startedAt, :completedAt,
                    :createdAt, :updatedAt
                )
                """, params(evaluation));
    }

    @Override
    public int update(ChannelEvaluationPO evaluation) {
        return jdbcTemplate.update("""
                UPDATE channel_evaluations
                SET provider_run_id = :providerRunId,
                    status = :status,
                    trigger_type = :triggerType,
                    score = :score,
                    detected_family = :detectedFamily,
                    detected_model = :detectedModel,
                    detected_confidence = :detectedConfidence,
                    family_mismatch = :familyMismatch,
                    channel_signature = :channelSignature,
                    report_url = :reportUrl,
                    passed_probe_count = :passedProbeCount,
                    warning_probe_count = :warningProbeCount,
                    failed_probe_count = :failedProbeCount,
                    total_input_tokens = :totalInputTokens,
                    total_output_tokens = :totalOutputTokens,
                    error_message = :errorMessage,
                    report_summary = :reportSummary,
                    started_at = :startedAt,
                    completed_at = :completedAt,
                    updated_at = :updatedAt
                WHERE id = :id
                """, params(evaluation));
    }

    @Override
    public ChannelEvaluationPO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM channel_evaluations WHERE id = :id",
                Map.of("id", id),
                rowMapper
        ));
    }

    @Override
    public List<ChannelEvaluationPO> selectHistory(EvaluationHistoryQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = whereClause(query, params);
        params.addValue("limit", query.limit());
        params.addValue("offset", query.offset());
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM channel_evaluations " + where
                        + " ORDER BY " + orderBy(query) + " LIMIT :limit OFFSET :offset",
                params,
                rowMapper
        );
    }

    @Override
    public long countHistory(EvaluationHistoryQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM channel_evaluations " + whereClause(query, params),
                params,
                Long.class
        );
        return count == null ? 0 : count;
    }

    @Override
    public EvaluationScoreSummaryPO summarize(EvaluationHistoryQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        EvaluationScoreSummaryPO summary = DataAccessUtils.singleResult(jdbcTemplate.query("""
                SELECT COUNT(*) AS total_count,
                       COUNT(score) AS scored_count,
                       COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_count,
                       AVG(score) AS average_score,
                       MIN(score) AS min_score,
                       MAX(score) AS max_score
                FROM channel_evaluations
                """ + whereClause(query, params), params, summaryRowMapper));
        return summary == null
                ? EvaluationScoreSummaryPO.builder().build()
                : summary;
    }

    @Override
    public List<ChannelEvaluationPO> selectInFlight(int limit) {
        return jdbcTemplate.query("""
                SELECT """ + COLUMNS + """
                FROM channel_evaluations
                WHERE status IN ('PENDING', 'RUNNING')
                ORDER BY requested_at ASC, id ASC
                LIMIT :limit
                """, Map.of("limit", limit), rowMapper);
    }

    @Override
    public int deleteByProviderChannelId(Long providerChannelId) {
        return jdbcTemplate.update(
                "DELETE FROM channel_evaluations WHERE provider_channel_id = :providerChannelId",
                Map.of("providerChannelId", providerChannelId)
        );
    }

    private String whereClause(EvaluationHistoryQuery query, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("provider_channel_id = :providerChannelId");
        params.addValue("providerChannelId", query.providerChannelId().value());
        if (query.requestedModel() != null) {
            conditions.add("requested_model = :requestedModel");
            params.addValue("requestedModel", query.requestedModel().value());
        }
        if (query.status() != null) {
            conditions.add("status = :status");
            params.addValue("status", query.status().name());
        }
        if (query.from() != null) {
            conditions.add("requested_at >= :from");
            params.addValue("from", timestamp(query.from()));
        }
        if (query.to() != null) {
            conditions.add("requested_at < :to");
            params.addValue("to", timestamp(query.to()));
        }
        return "WHERE " + String.join(" AND ", conditions);
    }

    private static String orderBy(EvaluationHistoryQuery query) {
        String direction = query.descending() ? "DESC" : "ASC";
        if (query.sortField() == EvaluationSortField.SCORE) {
            return "score " + direction + " NULLS LAST, requested_at DESC, id DESC";
        }
        return "requested_at " + direction + ", id " + direction;
    }

    private MapSqlParameterSource params(ChannelEvaluationPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("providerChannelId", po.getProviderChannelId())
                .addValue("requestedModel", po.getRequestedModel())
                .addValue("upstreamFormat", po.getUpstreamFormat())
                .addValue("providerRunId", po.getProviderRunId())
                .addValue("status", po.getStatus())
                .addValue("triggerType", po.getTriggerType())
                .addValue("score", po.getScore())
                .addValue("detectedFamily", po.getDetectedFamily())
                .addValue("detectedModel", po.getDetectedModel())
                .addValue("detectedConfidence", po.getDetectedConfidence())
                .addValue("familyMismatch", po.getFamilyMismatch())
                .addValue("channelSignature", po.getChannelSignature())
                .addValue("reportUrl", po.getReportUrl())
                .addValue("passedProbeCount", po.getPassedProbeCount())
                .addValue("warningProbeCount", po.getWarningProbeCount())
                .addValue("failedProbeCount", po.getFailedProbeCount())
                .addValue("totalInputTokens", po.getTotalInputTokens())
                .addValue("totalOutputTokens", po.getTotalOutputTokens())
                .addValue("errorMessage", po.getErrorMessage())
                .addValue("reportSummary", po.getReportSummary())
                .addValue("requestedAt", timestamp(po.getRequestedAt()))
                .addValue("startedAt", timestamp(po.getStartedAt()))
                .addValue("completedAt", timestamp(po.getCompletedAt()))
                .addValue("createdAt", timestamp(po.getCreatedAt()))
                .addValue("updatedAt", timestamp(po.getUpdatedAt()));
    }

    private static BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
