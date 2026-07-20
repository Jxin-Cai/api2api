package com.api2api.infr.repository.credential.mapper;

import static com.api2api.infr.repository.common.JdbcTimestampSupport.instant;
import static com.api2api.infr.repository.common.JdbcTimestampSupport.timestamp;

import com.api2api.infr.repository.credential.po.ApiCredentialPO;
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
public class JdbcApiCredentialMapper implements ApiCredentialMapper {

    private static final String COLUMNS = "c.id, c.owner_user_id, c.name, c.key_hash, c.key_preview, c.encrypted_key_material, c.key_material_nonce, c.key_material_version, c.model_group_id, g.model_whitelist, c.token_limit, c.status, c.last_used_at, c.created_at, c.updated_at, c.deleted";

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<ApiCredentialPO> rowMapper = (rs, rowNum) -> ApiCredentialPO.builder()
            .id(rs.getLong("id"))
            .ownerUserId(rs.getLong("owner_user_id"))
            .name(rs.getString("name"))
            .keyHash(rs.getString("key_hash"))
            .keyPreview(rs.getString("key_preview"))
            .encryptedKeyMaterial(rs.getString("encrypted_key_material"))
            .keyMaterialNonce(rs.getString("key_material_nonce"))
            .keyMaterialVersion(rs.getInt("key_material_version"))
            .modelGroupId(rs.getLong("model_group_id"))
            .modelWhitelist(rs.getString("model_whitelist"))
            .tokenLimit(rs.getLong("token_limit"))
            .status(rs.getString("status"))
            .lastUsedTime(instant(rs, "last_used_at"))
            .createdTime(instant(rs, "created_at"))
            .updatedTime(instant(rs, "updated_at"))
            .deleted(rs.getBoolean("deleted"))
            .build();

    @Override
    public int insert(ApiCredentialPO apiCredential) {
        return jdbcTemplate.update("""
                INSERT INTO api_credentials (id, owner_user_id, name, key_hash, key_preview, encrypted_key_material, key_material_nonce, key_material_version, model_group_id, token_limit, status, last_used_at, created_at, updated_at, deleted)
                VALUES (:id, :ownerUserId, :name, :keyHash, :keyPreview, :encryptedKeyMaterial, :keyMaterialNonce, :keyMaterialVersion, :modelGroupId, :tokenLimit, :status, :lastUsedTime, :createdTime, :updatedTime, :deleted)
                """, params(apiCredential));
    }

    @Override
    public int update(ApiCredentialPO apiCredential) {
        return jdbcTemplate.update("""
                UPDATE api_credentials
                SET name = :name,
                    model_group_id = :modelGroupId,
                    encrypted_key_material = :encryptedKeyMaterial,
                    key_material_nonce = :keyMaterialNonce,
                    key_material_version = :keyMaterialVersion,
                    token_limit = :tokenLimit,
                    status = :status,
                    last_used_at = :lastUsedTime,
                    updated_at = :updatedTime,
                    deleted = :deleted
                WHERE id = :id
                """, params(apiCredential));
    }

    @Override
    public ApiCredentialPO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM api_credentials c JOIN model_groups g ON g.id = c.model_group_id AND g.deleted = FALSE WHERE c.id = :id AND c.deleted = FALSE",
                Map.of("id", id), rowMapper));
    }

    @Override
    public ApiCredentialPO selectByKeyHash(String keyHash) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM api_credentials c JOIN model_groups g ON g.id = c.model_group_id AND g.deleted = FALSE WHERE c.key_hash = :keyHash AND c.deleted = FALSE",
                Map.of("keyHash", keyHash), rowMapper));
    }

    @Override
    public List<ApiCredentialPO> selectByOwnerUserId(Long ownerUserId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM api_credentials c JOIN model_groups g ON g.id = c.model_group_id AND g.deleted = FALSE WHERE c.owner_user_id = :ownerUserId AND c.deleted = FALSE ORDER BY c.created_at DESC, c.id DESC",
                Map.of("ownerUserId", ownerUserId), rowMapper);
    }

    @Override
    public int softDeleteById(Long id, Instant updatedAt) {
        return jdbcTemplate.update("""
                UPDATE api_credentials
                SET deleted = TRUE,
                    status = 'DISABLED',
                    updated_at = :updatedTime
                WHERE id = :id
                  AND deleted = FALSE
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("updatedTime", timestamp(updatedAt)));
    }

    private MapSqlParameterSource params(ApiCredentialPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("ownerUserId", po.getOwnerUserId())
                .addValue("name", po.getName())
                .addValue("keyHash", po.getKeyHash())
                .addValue("keyPreview", po.getKeyPreview())
                .addValue("encryptedKeyMaterial", po.getEncryptedKeyMaterial())
                .addValue("keyMaterialNonce", po.getKeyMaterialNonce())
                .addValue("keyMaterialVersion", po.getKeyMaterialVersion())
                .addValue("modelGroupId", po.getModelGroupId())
                .addValue("tokenLimit", po.getTokenLimit())
                .addValue("status", po.getStatus())
                .addValue("lastUsedTime", timestamp(po.getLastUsedTime()))
                .addValue("createdTime", timestamp(po.getCreatedTime()))
                .addValue("updatedTime", timestamp(po.getUpdatedTime()))
                .addValue("deleted", po.isDeleted());
    }

}
