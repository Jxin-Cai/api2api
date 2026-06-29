package com.api2api.infr.repository.channel.mapper;

import com.api2api.infr.repository.channel.po.ChannelModelSupportPO;
import com.api2api.infr.repository.channel.po.ProviderChannelPO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcProviderChannelMapper implements ProviderChannelMapper {

    private static final String CHANNEL_COLUMNS = "id, name, host, key_ref, supported_protocols, status, created_at, updated_at, deleted";
    private static final String MODEL_COLUMNS = "id, provider_channel_id, requested_model, upstream_model, upstream_protocol, priority, source, status, created_at, updated_at";

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ProviderChannelPO> channelRowMapper = (rs, rowNum) -> ProviderChannelPO.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .host(rs.getString("host"))
            .keyRef(rs.getString("key_ref"))
            .supportedProtocols(rs.getString("supported_protocols"))
            .status(rs.getString("status"))
            .createdTime(instant(rs, "created_at"))
            .updatedTime(instant(rs, "updated_at"))
            .deleted(rs.getBoolean("deleted"))
            .build();

    private final RowMapper<ChannelModelSupportPO> modelRowMapper = (rs, rowNum) -> ChannelModelSupportPO.builder()
            .id(rs.getLong("id"))
            .providerChannelId(rs.getLong("provider_channel_id"))
            .requestedModel(rs.getString("requested_model"))
            .upstreamModel(rs.getString("upstream_model"))
            .upstreamProtocol(rs.getString("upstream_protocol"))
            .priority(rs.getInt("priority"))
            .source(rs.getString("source"))
            .status(rs.getString("status"))
            .createdTime(instant(rs, "created_at"))
            .updatedTime(instant(rs, "updated_at"))
            .build();

    @Override
    @Transactional
    public int insert(ProviderChannelPO providerChannel) {
        int affected = jdbcTemplate.update("""
                INSERT INTO provider_channels (id, name, host, key_ref, supported_protocols, status, created_at, updated_at, deleted)
                VALUES (:id, :name, :host, :keyRef, :supportedProtocols, :status, :createdTime, :updatedTime, :deleted)
                """, params(providerChannel));
        replaceModelSupports(providerChannel);
        return affected;
    }

    @Override
    @Transactional
    public int update(ProviderChannelPO providerChannel) {
        int affected = jdbcTemplate.update("""
                UPDATE provider_channels
                SET name = :name,
                    host = :host,
                    key_ref = :keyRef,
                    supported_protocols = :supportedProtocols,
                    status = :status,
                    updated_at = :updatedTime,
                    deleted = :deleted
                WHERE id = :id
                """, params(providerChannel));
        replaceModelSupports(providerChannel);
        return affected;
    }

    @Override
    public ProviderChannelPO selectById(Long id) {
        ProviderChannelPO channel = DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + CHANNEL_COLUMNS + " FROM provider_channels WHERE id = :id AND deleted = FALSE",
                Map.of("id", id), channelRowMapper));
        return withModels(channel, false);
    }

    @Override
    public List<ProviderChannelPO> selectAll() {
        return jdbcTemplate.query(
                        "SELECT " + CHANNEL_COLUMNS + " FROM provider_channels WHERE deleted = FALSE ORDER BY created_at DESC, id DESC",
                        Map.of(), channelRowMapper)
                .stream()
                .map(channel -> withModels(channel, false))
                .toList();
    }

    @Override
    public List<ProviderChannelPO> selectEnabledForRouting() {
        return jdbcTemplate.query(
                        "SELECT " + CHANNEL_COLUMNS + " FROM provider_channels WHERE deleted = FALSE AND status = 'ENABLED' ORDER BY created_at DESC, id DESC",
                        Map.of(), channelRowMapper)
                .stream()
                .map(channel -> withModels(channel, true))
                .toList();
    }

    private ProviderChannelPO withModels(ProviderChannelPO channel, boolean enabledOnly) {
        if (channel == null) {
            return null;
        }
        String sql = "SELECT " + MODEL_COLUMNS + " FROM channel_model_supports WHERE provider_channel_id = :providerChannelId";
        if (enabledOnly) {
            sql += " AND status = 'ENABLED'";
        }
        sql += " ORDER BY priority ASC, created_at ASC, id ASC";
        channel.setSupportedModels(jdbcTemplate.query(sql, Map.of("providerChannelId", channel.getId()), modelRowMapper));
        return channel;
    }

    private void replaceModelSupports(ProviderChannelPO providerChannel) {
        jdbcTemplate.update("DELETE FROM channel_model_supports WHERE provider_channel_id = :providerChannelId",
                Map.of("providerChannelId", providerChannel.getId()));
        for (ChannelModelSupportPO model : providerChannel.getSupportedModels()) {
            jdbcTemplate.update("""
                    INSERT INTO channel_model_supports (id, provider_channel_id, requested_model, upstream_model, upstream_protocol, priority, source, status, created_at, updated_at)
                    VALUES (:id, :providerChannelId, :requestedModel, :upstreamModel, :upstreamProtocol, :priority, :source, :status, :createdTime, :updatedTime)
                    """, modelParams(model));
        }
    }

    private MapSqlParameterSource params(ProviderChannelPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("name", po.getName())
                .addValue("host", po.getHost())
                .addValue("keyRef", po.getKeyRef())
                .addValue("supportedProtocols", po.getSupportedProtocols())
                .addValue("status", po.getStatus())
                .addValue("createdTime", timestamp(po.getCreatedTime()))
                .addValue("updatedTime", timestamp(po.getUpdatedTime()))
                .addValue("deleted", po.isDeleted());
    }

    private MapSqlParameterSource modelParams(ChannelModelSupportPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("providerChannelId", po.getProviderChannelId())
                .addValue("requestedModel", po.getRequestedModel())
                .addValue("upstreamModel", po.getUpstreamModel())
                .addValue("upstreamProtocol", po.getUpstreamProtocol())
                .addValue("priority", po.getPriority())
                .addValue("source", po.getSource())
                .addValue("status", po.getStatus())
                .addValue("createdTime", timestamp(po.getCreatedTime()))
                .addValue("updatedTime", timestamp(po.getUpdatedTime()));
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
