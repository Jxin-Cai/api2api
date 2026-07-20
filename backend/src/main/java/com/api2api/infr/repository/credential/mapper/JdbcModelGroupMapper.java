package com.api2api.infr.repository.credential.mapper;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;

import com.api2api.infr.repository.credential.po.ModelGroupPO;
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
public class JdbcModelGroupMapper implements ModelGroupMapper {

    private static final String COLUMNS = "id, owner_user_id, name, model_whitelist, created_at, updated_at, deleted";

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ModelGroupPO> rowMapper = (rs, rowNum) -> ModelGroupPO.builder()
            .id(rs.getLong("id"))
            .ownerUserId(rs.getLong("owner_user_id"))
            .name(rs.getString("name"))
            .modelWhitelist(rs.getString("model_whitelist"))
            .createdAt(instant(rs, "created_at"))
            .updatedAt(instant(rs, "updated_at"))
            .deleted(rs.getBoolean("deleted"))
            .build();

    @Override
    public int insert(ModelGroupPO group) {
        return jdbcTemplate.update("""
                INSERT INTO model_groups (id, owner_user_id, name, model_whitelist, created_at, updated_at, deleted)
                VALUES (:id, :ownerUserId, :name, :modelWhitelist, :createdAt, :updatedAt, :deleted)
                """, params(group));
    }

    @Override
    public int update(ModelGroupPO group) {
        return jdbcTemplate.update("""
                UPDATE model_groups
                SET name = :name, model_whitelist = :modelWhitelist, updated_at = :updatedAt
                WHERE id = :id AND deleted = FALSE
                """, params(group));
    }

    @Override
    public ModelGroupPO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM model_groups WHERE id = :id AND deleted = FALSE",
                Map.of("id", id), rowMapper));
    }

    @Override
    public List<ModelGroupPO> selectByOwnerUserId(Long ownerUserId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM model_groups WHERE owner_user_id = :ownerUserId AND deleted = FALSE ORDER BY created_at DESC, id DESC",
                Map.of("ownerUserId", ownerUserId), rowMapper);
    }

    @Override
    public boolean existsByOwnerAndNameExcludingId(Long ownerUserId, String name, Long excludedId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS(
                    SELECT 1 FROM model_groups
                    WHERE owner_user_id = :ownerUserId AND name = :name AND id <> :excludedId AND deleted = FALSE
                )
                """, Map.of("ownerUserId", ownerUserId, "name", name, "excludedId", excludedId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean existsCredentialBinding(Long id) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM api_credentials WHERE model_group_id = :id AND deleted = FALSE)
                """, Map.of("id", id), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int softDeleteById(Long id, Instant updatedAt) {
        return jdbcTemplate.update("""
                UPDATE model_groups SET deleted = TRUE, updated_at = :updatedAt
                WHERE id = :id AND deleted = FALSE
                """, new MapSqlParameterSource().addValue("id", id).addValue("updatedAt", timestamp(updatedAt)));
    }

    private MapSqlParameterSource params(ModelGroupPO group) {
        return new MapSqlParameterSource()
                .addValue("id", group.getId())
                .addValue("ownerUserId", group.getOwnerUserId())
                .addValue("name", group.getName())
                .addValue("modelWhitelist", group.getModelWhitelist())
                .addValue("createdAt", timestamp(group.getCreatedAt()))
                .addValue("updatedAt", timestamp(group.getUpdatedAt()))
                .addValue("deleted", group.isDeleted());
    }
}
