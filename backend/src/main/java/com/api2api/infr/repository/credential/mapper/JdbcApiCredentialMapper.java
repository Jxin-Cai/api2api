package com.api2api.infr.repository.credential.mapper;

import com.api2api.infr.repository.credential.po.ApiCredentialPO;
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

@Repository
@RequiredArgsConstructor
public class JdbcApiCredentialMapper implements ApiCredentialMapper {

    private static final String COLUMNS = "id, owner_user_id, name, key_hash, key_preview, encrypted_key_material, key_material_nonce, key_material_version, model_whitelist, token_limit, status, last_used_at, created_at, updated_at, deleted";

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
                INSERT INTO api_credentials (id, owner_user_id, name, key_hash, key_preview, encrypted_key_material, key_material_nonce, key_material_version, model_whitelist, token_limit, status, last_used_at, created_at, updated_at, deleted)
                VALUES (:id, :ownerUserId, :name, :keyHash, :keyPreview, :encryptedKeyMaterial, :keyMaterialNonce, :keyMaterialVersion, :modelWhitelist, :tokenLimit, :status, :lastUsedTime, :createdTime, :updatedTime, :deleted)
                """, params(apiCredential));
    }

    @Override
    public int update(ApiCredentialPO apiCredential) {
        return jdbcTemplate.update("""
                UPDATE api_credentials
                SET name = :name,
                    model_whitelist = :modelWhitelist,
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
                "SELECT " + COLUMNS + " FROM api_credentials WHERE id = :id AND deleted = FALSE",
                Map.of("id", id), rowMapper));
    }

    @Override
    public ApiCredentialPO selectByKeyHash(String keyHash) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM api_credentials WHERE key_hash = :keyHash AND deleted = FALSE",
                Map.of("keyHash", keyHash), rowMapper));
    }

    @Override
    public List<ApiCredentialPO> selectByOwnerUserId(Long ownerUserId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM api_credentials WHERE owner_user_id = :ownerUserId AND deleted = FALSE ORDER BY created_at DESC, id DESC",
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
                .addValue("modelWhitelist", po.getModelWhitelist())
                .addValue("tokenLimit", po.getTokenLimit())
                .addValue("status", po.getStatus())
                .addValue("lastUsedTime", timestamp(po.getLastUsedTime()))
                .addValue("createdTime", timestamp(po.getCreatedTime()))
                .addValue("updatedTime", timestamp(po.getUpdatedTime()))
                .addValue("deleted", po.isDeleted());
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
