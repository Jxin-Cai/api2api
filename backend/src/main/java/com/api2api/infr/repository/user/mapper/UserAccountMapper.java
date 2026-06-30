package com.api2api.infr.repository.user.mapper;

import com.api2api.infr.repository.user.po.UserAccountPO;
import java.util.List;

/**
 * Mapper contract for user_accounts persistence.
 * SQL mappings are supplied by the concrete infrastructure configuration.
 */
public interface UserAccountMapper {

    int insert(UserAccountPO userAccount);

    int update(UserAccountPO userAccount);

    UserAccountPO selectById(Long id);

    UserAccountPO selectByUsername(String username);

    List<UserAccountPO> selectAll();
}
