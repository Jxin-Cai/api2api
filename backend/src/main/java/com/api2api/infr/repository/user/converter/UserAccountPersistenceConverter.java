package com.api2api.infr.repository.user.converter;

import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.PasswordHash;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.UserAccountStatus;
import com.api2api.domain.user.model.UserRole;
import com.api2api.domain.user.model.Username;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.infr.repository.user.po.UserAccountPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts between UserAccount aggregate and persistence object.
 */
@Mapper(config = MapStructConfig.class)
public interface UserAccountPersistenceConverter {

    @Mapping(target = "id", expression = "java(userAccount.getId().getValue())")
    @Mapping(target = "username", expression = "java(userAccount.getUsername().getValue())")
    @Mapping(target = "displayName", expression = "java(userAccount.getDisplayName().getValue())")
    @Mapping(target = "role", expression = "java(userAccount.getRole().name())")
    @Mapping(target = "status", expression = "java(userAccount.getStatus().name())")
    @Mapping(target = "passwordHash", expression = "java(userAccount.getPasswordHash() == null ? null : userAccount.getPasswordHash().getValue())")
    @Mapping(target = "createdTime", source = "createdAt")
    @Mapping(target = "updatedTime", source = "updatedAt")
    @Mapping(target = "deleted", constant = "false")
    UserAccountPO toPO(UserAccount userAccount);

    default UserAccount toDomain(UserAccountPO po) {
        return UserAccount.rehydrate(
                UserAccountId.of(po.getId()),
                Username.of(po.getUsername()),
                DisplayName.of(po.getDisplayName()),
                UserRole.valueOf(po.getRole()),
                UserAccountStatus.valueOf(po.getStatus()),
                PasswordHash.ofNullable(po.getPasswordHash()),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }
}
