package com.api2api.infr.repository.evaluation.mapper;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;

import com.api2api.infr.repository.evaluation.po.ChannelEvaluationSchedulePO;
import java.time.Instant;
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
public class JdbcChannelEvaluationScheduleMapper implements ChannelEvaluationScheduleMapper {

    private static final String COLUMNS = """
            id, provider_channel_id, cron_expression, zone_id, models, enabled,
            last_triggered_at, next_trigger_at, created_at, updated_at
            """;

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ChannelEvaluationSchedulePO> rowMapper = (rs, rowNum) -> ChannelEvaluationSchedulePO.builder()
            .id(rs.getLong("id"))
            .providerChannelId(rs.getLong("provider_channel_id"))
            .cronExpression(rs.getString("cron_expression"))
            .zoneId(rs.getString("zone_id"))
            .models(rs.getString("models"))
            .enabled(rs.getBoolean("enabled"))
            .lastTriggeredAt(instant(rs, "last_triggered_at"))
            .nextTriggerAt(instant(rs, "next_trigger_at"))
            .createdAt(instant(rs, "created_at"))
            .updatedAt(instant(rs, "updated_at"))
            .build();

    @Override
    public int insert(ChannelEvaluationSchedulePO schedule) {
        return jdbcTemplate.update("""
                INSERT INTO channel_evaluation_schedules (
                    id, provider_channel_id, cron_expression, zone_id, models, enabled,
                    last_triggered_at, next_trigger_at, created_at, updated_at
                ) VALUES (
                    :id, :providerChannelId, :cronExpression, :zoneId, :models, :enabled,
                    :lastTriggeredAt, :nextTriggerAt, :createdAt, :updatedAt
                )
                """, params(schedule));
    }

    @Override
    public int update(ChannelEvaluationSchedulePO schedule) {
        return jdbcTemplate.update("""
                UPDATE channel_evaluation_schedules
                SET cron_expression = :cronExpression,
                    zone_id = :zoneId,
                    models = :models,
                    enabled = :enabled,
                    last_triggered_at = :lastTriggeredAt,
                    next_trigger_at = :nextTriggerAt,
                    updated_at = :updatedAt
                WHERE id = :id
                """, params(schedule));
    }

    @Override
    public ChannelEvaluationSchedulePO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM channel_evaluation_schedules WHERE id = :id",
                Map.of("id", id),
                rowMapper
        ));
    }

    @Override
    public ChannelEvaluationSchedulePO selectByProviderChannelId(Long providerChannelId) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM channel_evaluation_schedules WHERE provider_channel_id = :providerChannelId",
                Map.of("providerChannelId", providerChannelId),
                rowMapper
        ));
    }

    @Override
    public List<ChannelEvaluationSchedulePO> selectDue(Instant now, int limit) {
        return jdbcTemplate.query("""
                SELECT """ + COLUMNS + """
                FROM channel_evaluation_schedules
                WHERE enabled = TRUE
                  AND next_trigger_at IS NOT NULL
                  AND next_trigger_at <= :now
                ORDER BY next_trigger_at ASC, id ASC
                LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("now", timestamp(now))
                .addValue("limit", limit), rowMapper);
    }

    @Override
    public int deleteByProviderChannelId(Long providerChannelId) {
        return jdbcTemplate.update(
                "DELETE FROM channel_evaluation_schedules WHERE provider_channel_id = :providerChannelId",
                Map.of("providerChannelId", providerChannelId)
        );
    }

    private MapSqlParameterSource params(ChannelEvaluationSchedulePO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("providerChannelId", po.getProviderChannelId())
                .addValue("cronExpression", po.getCronExpression())
                .addValue("zoneId", po.getZoneId())
                .addValue("models", po.getModels())
                .addValue("enabled", po.isEnabled())
                .addValue("lastTriggeredAt", timestamp(po.getLastTriggeredAt()))
                .addValue("nextTriggerAt", timestamp(po.getNextTriggerAt()))
                .addValue("createdAt", timestamp(po.getCreatedAt()))
                .addValue("updatedAt", timestamp(po.getUpdatedAt()));
    }
}
