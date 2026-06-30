package com.api2api.ohs.http.admin.converter;

import com.api2api.application.user.command.ChangeUserDisplayNameCommand;
import com.api2api.application.user.command.ChangeUserRoleCommand;
import com.api2api.application.user.command.ChangeUserStatusCommand;
import com.api2api.application.user.command.CreateUserCommand;
import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.Username;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.admin.dto.AdminChangeUserDisplayNameRequest;
import com.api2api.ohs.http.admin.dto.AdminChangeUserRoleRequest;
import com.api2api.ohs.http.admin.dto.AdminCreateUserRequest;
import com.api2api.ohs.http.admin.dto.UserAccountListResponse;
import com.api2api.ohs.http.admin.dto.UserAccountResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts admin user HTTP models to application commands and responses.
 */
@Mapper(config = MapStructConfig.class)
public interface AdminUserHttpConverter {

    @Mapping(target = "operatorUserId", source = "operatorUserId")
    @Mapping(target = "userAccountId", source = "newUserAccountId")
    @Mapping(target = "username", source = "request.username")
    @Mapping(target = "displayName", source = "request.displayName")
    @Mapping(target = "role", source = "request.role")
    @Mapping(target = "password", source = "request.password")
    CreateUserCommand toCreateUserCommand(
            AdminCreateUserRequest request,
            UserAccountId operatorUserId,
            UserAccountId newUserAccountId
    );

    @Mapping(target = "operatorUserId", source = "operatorUserId")
    @Mapping(target = "targetUserId", source = "targetUserId")
    @Mapping(target = "displayName", source = "request.displayName")
    ChangeUserDisplayNameCommand toChangeDisplayNameCommand(
            AdminChangeUserDisplayNameRequest request,
            UserAccountId operatorUserId,
            UserAccountId targetUserId
    );

    @Mapping(target = "operatorUserId", source = "operatorUserId")
    @Mapping(target = "targetUserId", source = "targetUserId")
    @Mapping(target = "newRole", source = "request.newRole")
    ChangeUserRoleCommand toChangeRoleCommand(
            AdminChangeUserRoleRequest request,
            UserAccountId operatorUserId,
            UserAccountId targetUserId
    );

    ChangeUserStatusCommand toChangeUserStatusCommand(UserAccountId operatorUserId, UserAccountId targetUserId);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "username", source = "username.value")
    @Mapping(target = "displayName", source = "displayName.value")
    UserAccountResponse toUserAccountResponse(UserAccount userAccount);

    default UserAccountListResponse toListResponse(List<UserAccount> userAccounts) {
        return UserAccountListResponse.builder()
                .users(userAccounts.stream().map(this::toUserAccountResponse).toList())
                .build();
    }

    default Username toUsername(String username) {
        return Username.of(username);
    }

    default DisplayName toDisplayName(String displayName) {
        return DisplayName.of(displayName);
    }
}
