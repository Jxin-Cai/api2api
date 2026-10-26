package com.api2api.infr.repository.quota;

import com.api2api.domain.quota.*;
import com.api2api.domain.usage.model.UsageTimeRange;
import com.api2api.infr.repository.common.UsageTokenSqlFragments;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcTokenQuotaRepository implements TokenQuotaRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<TokenQuotaRule> mapper = (rs, row) -> new TokenQuotaRule(
            rs.getLong("id"), QuotaScope.valueOf(rs.getString("scope")), rs.getLong("target_id"),
            rs.getString("model"), QuotaPeriod.valueOf(rs.getString("period")), rs.getBigDecimal("limit_tokens"), rs.getBoolean("enabled"));

    public List<TokenQuotaRule> findAll() {
        return jdbc.query("SELECT * FROM token_quota_rules ORDER BY id DESC", mapper);
    }

    public List<TokenQuotaRule> findApplicable(long groupId, long keyId) {
        return jdbc.query("""
                SELECT * FROM token_quota_rules WHERE enabled = TRUE
                AND ((scope = 'MODEL_GROUP' AND target_id = :groupId) OR (scope = 'API_KEY' AND target_id = :keyId))
                ORDER BY id
                """, Map.of("groupId", groupId, "keyId", keyId), mapper);
    }

    public TokenQuotaRule save(TokenQuotaRule rule) {
        var params = new MapSqlParameterSource().addValue("id", rule.id()).addValue("scope", rule.scope().name())
                .addValue("targetId", rule.targetId()).addValue("model", rule.model()).addValue("period", rule.period().name())
                .addValue("limitTokens", rule.limitTokens()).addValue("enabled", rule.enabled());
        String sql = rule.id() == 0 ? """
                INSERT INTO token_quota_rules(scope, target_id, model, period, limit_tokens, enabled)
                VALUES (:scope, :targetId, :model, :period, :limitTokens, :enabled) RETURNING *
                """ : """
                UPDATE token_quota_rules SET scope=:scope, target_id=:targetId, model=:model,
                period=:period, limit_tokens=:limitTokens, enabled=:enabled, updated_at=now()
                WHERE id=:id RETURNING *
                """;
        List<TokenQuotaRule> saved = jdbc.query(sql, params, mapper);
        return saved.isEmpty() ? null : saved.get(0);
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM token_quota_rules WHERE id=:id", Map.of("id", id)) > 0;
    }

    public List<QuotaTarget> targets() {
        return jdbc.query("""
                SELECT 'MODEL_GROUP' AS scope, g.id, g.name, '' AS owner_name, g.id AS model_group_id
                FROM model_groups g WHERE NOT g.deleted
                UNION ALL
                SELECT 'API_KEY', c.id, c.name, u.username, c.model_group_id
                FROM api_credentials c JOIN user_accounts u ON u.id=c.owner_user_id WHERE NOT c.deleted
                ORDER BY scope, name, id
                """, (rs, row) -> new QuotaTarget(QuotaScope.valueOf(rs.getString("scope")), rs.getString("id"),
                rs.getString("name"), rs.getString("owner_name"), rs.getString("model_group_id")));
    }

    public boolean targetExists(QuotaScope scope, long targetId) {
        String table = scope == QuotaScope.MODEL_GROUP ? "model_groups" : "api_credentials";
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM " + table
                + " WHERE id=:id AND NOT deleted)", Map.of("id", targetId), Boolean.class));
    }

    public void lockCredential(long keyId) {
        jdbc.queryForList("SELECT id FROM api_credentials WHERE id=:id FOR UPDATE", Map.of("id", keyId));
    }

    public void lockGroup(long groupId) {
        jdbc.queryForList("SELECT id FROM model_groups WHERE id=:id FOR UPDATE", Map.of("id", groupId));
    }

    public BigDecimal consumed(TokenQuotaRule rule, UsageTimeRange window) {
        String target = rule.scope() == QuotaScope.MODEL_GROUP ? "model_group_id" : "api_credential_id";
        var params = new MapSqlParameterSource().addValue("targetId", rule.targetId())
                .addValue("start", Timestamp.from(window.startInclusive())).addValue("end", Timestamp.from(window.endExclusive()));
        String modelFilter = "";
        if (!rule.model().isEmpty()) {
            modelFilter = " AND requested_model=:model";
            params.addValue("model", rule.model());
        }
        return jdbc.queryForObject("SELECT COALESCE(SUM(" + UsageTokenSqlFragments.ACTUAL_TOKENS_SQL
                + "), 0) FROM usage_records WHERE " + target + "=:targetId AND NOT deleted"
                + " AND status IN ('SUCCESS','PENDING') AND started_at >= :start AND started_at < :end" + modelFilter,
                params, BigDecimal.class);
    }
}
