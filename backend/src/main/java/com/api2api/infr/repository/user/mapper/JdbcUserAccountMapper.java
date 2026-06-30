package com.api2api.infr.repository.user.mapper;

import com.api2api.infr.repository.user.po.UserAccountPO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
public class JdbcUserAccountMapper implements UserAccountMapper {

    private static final String COLUMNS = "id, username, display_name, role, status, password_hash, created_at, updated_at, deleted";

    @NonNull
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<UserAccountPO> rowMapper = (rs, rowNum) -> UserAccountPO.builder()
            .id(rs.getLong("id"))
            .username(rs.getString("username"))
            .displayName(rs.getString("display_name"))
            .role(rs.getString("role"))
            .status(rs.getString("status"))
            .passwordHash(rs.getString("password_hash"))
            .createdTime(instant(rs, "created_at"))
            .updatedTime(instant(rs, "updated_at"))
            .deleted(rs.getBoolean("deleted"))
            .build();

    @Override
    public int insert(UserAccountPO userAccount) {
        return jdbcTemplate.update("""
                INSERT INTO user_accounts (id, username, display_name, role, status, password_hash, created_at, updated_at, deleted)
                VALUES (:id, :username, :displayName, :role, :status, :passwordHash, :createdTime, :updatedTime, :deleted)
                """, params(userAccount));
    }

    @Override
    public int update(UserAccountPO userAccount) {
        return jdbcTemplate.update("""
                UPDATE user_accounts
                SET username = :username,
                    display_name = :displayName,
                    role = :role,
                    status = :status,
                    password_hash = :passwordHash,
                    updated_at = :updatedTime,
                    deleted = :deleted
                WHERE id = :id
                """, params(userAccount));
    }

    @Override
    public UserAccountPO selectById(Long id) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM user_accounts WHERE id = :id AND deleted = FALSE",
                Map.of("id", id), rowMapper));
    }

    @Override
    public UserAccountPO selectByUsername(String username) {
        return DataAccessUtils.singleResult(jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM user_accounts WHERE username = :username AND deleted = FALSE",
                Map.of("username", username), rowMapper));
    }

    private MapSqlParameterSource params(UserAccountPO po) {
        return new MapSqlParameterSource()
                .addValue("id", po.getId())
                .addValue("username", po.getUsername())
                .addValue("displayName", po.getDisplayName())
                .addValue("role", po.getRole())
                .addValue("status", po.getStatus())
                .addValue("passwordHash", po.getPasswordHash())
                .addValue("createdTime", Timestamp.from(po.getCreatedTime()))
                .addValue("updatedTime", Timestamp.from(po.getUpdatedTime()))
                .addValue("deleted", po.isDeleted());
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
