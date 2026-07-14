package com.api2api.infr.repository.protocol.mapper;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;

import com.api2api.infr.repository.protocol.po.ProtocolConversionDefinitionPO;
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
public class JdbcProtocolConversionDefinitionMapper implements ProtocolConversionDefinitionMapper {

    private static final String COLUMNS = "id, source_protocol, target_protocol, kind, status, implementation_status, supports_streaming, supports_tool_calling, supports_reasoning, supports_usage_mapping, supports_cache_token_mapping, request_mapping_json, response_mapping_json, created_at, updated_at";

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ProtocolConversionDefinitionPO> rowMapper = (rs, rowNum) -> ProtocolConversionDefinitionPO.builder()
            .id(rs.getLong("id"))
            .sourceProtocol(rs.getString("source_protocol"))
            .targetProtocol(rs.getString("target_protocol"))
            .kind(rs.getString("kind"))
            .status(rs.getString("status"))
            .implementationStatus(rs.getString("implementation_status"))
            .supportsStreaming(rs.getBoolean("supports_streaming"))
            .supportsToolCalling(rs.getBoolean("supports_tool_calling"))
            .supportsReasoning(rs.getBoolean("supports_reasoning"))
            .supportsUsageMapping(rs.getBoolean("supports_usage_mapping"))
            .supportsCacheTokenMapping(rs.getBoolean("supports_cache_token_mapping"))
            .requestMappingJson(rs.getString("request_mapping_json"))
            .responseMappingJson(rs.getString("response_mapping_json"))
            .createdTime(instant(rs, "created_at"))
            .updatedTime(instant(rs, "updated_at"))
            .build();

    @Override
    public int insert(ProtocolConversionDefinitionPO definition) {
        return jdbcTemplate.update("""
                INSERT INTO protocol_conversion_definitions (id, source_protocol, target_protocol, kind, status, implementation_status, supports_streaming, supports_tool_calling, supports_reasoning, supports_usage_mapping, supports_cache_token_mapping, request_mapping_json, response_mapping_json, created_at, updated_at)
                VALUES (:id, :sourceProtocol, :targetProtocol, :kind, :status, :implementationStatus, :supportsStreaming, :supportsToolCalling, :supportsReasoning, :supportsUsageMapping, :supportsCacheTokenMapping, :requestMappingJson, :responseMappingJson, :createdTime, :updatedTime)
                """, params(definition));
    }

    @Override
    public int update(ProtocolConversionDefinitionPO definition) {
        return jdbcTemplate.update("""
                UPDATE protocol_conversion_definitions
                SET kind = :kind,
                    status = :status,
                    implementation_status = :implementationStatus,
                    supports_streaming = :supportsStreaming,
                    supports_tool_calling = :supportsToolCalling,
                    supports_reasoning = :supportsReasoning,
                    supports_usage_mapping = :supportsUsageMapping,
                    supports_cache_token_mapping = :supportsCacheTokenMapping,
                    request_mapping_json = :requestMappingJson,
                    response_mapping_json = :responseMappingJson,
                    updated_at = :updatedTime
                WHERE id = :id
                """, params(definition));
    }

    @Override
    public ProtocolConversionDefinitionPO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM protocol_conversion_definitions WHERE id = :id",
                Map.of("id", id), rowMapper));
    }

    @Override
    public ProtocolConversionDefinitionPO selectBySourceAndTarget(String sourceProtocol, String targetProtocol) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM protocol_conversion_definitions WHERE source_protocol = :sourceProtocol AND target_protocol = :targetProtocol",
                Map.of("sourceProtocol", sourceProtocol, "targetProtocol", targetProtocol), rowMapper));
    }

    @Override
    public List<ProtocolConversionDefinitionPO> selectAll() {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM protocol_conversion_definitions ORDER BY source_protocol ASC, target_protocol ASC",
                Map.of(), rowMapper);
    }

    private MapSqlParameterSource params(ProtocolConversionDefinitionPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("sourceProtocol", po.getSourceProtocol())
                .addValue("targetProtocol", po.getTargetProtocol())
                .addValue("kind", po.getKind())
                .addValue("status", po.getStatus())
                .addValue("implementationStatus", po.getImplementationStatus())
                .addValue("supportsStreaming", po.isSupportsStreaming())
                .addValue("supportsToolCalling", po.isSupportsToolCalling())
                .addValue("supportsReasoning", po.isSupportsReasoning())
                .addValue("supportsUsageMapping", po.isSupportsUsageMapping())
                .addValue("supportsCacheTokenMapping", po.isSupportsCacheTokenMapping())
                .addValue("requestMappingJson", po.getRequestMappingJson())
                .addValue("responseMappingJson", po.getResponseMappingJson())
                .addValue("createdTime", timestamp(po.getCreatedTime()))
                .addValue("updatedTime", timestamp(po.getUpdatedTime()));
    }

}
