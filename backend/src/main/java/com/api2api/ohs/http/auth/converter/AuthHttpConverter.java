package com.api2api.ohs.http.auth.converter;

import com.api2api.application.user.command.LoginCommand;
import com.api2api.application.user.command.UpdateCurrentUserProfileCommand;
import com.api2api.domain.user.model.DisplayName;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.model.Username;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.auth.dto.CurrentUserResponse;
import com.api2api.ohs.http.auth.dto.LoginRequest;
import com.api2api.ohs.http.auth.dto.LoginResponse;
import com.api2api.ohs.http.auth.dto.UpdateCurrentUserProfileRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts auth HTTP models to application commands and responses.
 */
@Mapper(config = MapStructConfig.class)
public interface AuthHttpConverter {

    LoginCommand toLoginCommand(LoginRequest request);

    @Mapping(target = "currentUserId", source = "currentUserId")
    @Mapping(target = "displayName", source = "request.displayName")
    UpdateCurrentUserProfileCommand toUpdateCurrentUserProfileCommand(
            UpdateCurrentUserProfileRequest request,
            UserAccountId currentUserId
    );

    @Mapping(target = "currentUserId", source = "id.value")
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "username", source = "username.value")
    @Mapping(target = "displayName", source = "displayName.value")
    LoginResponse toLoginResponse(UserAccount userAccount);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "username", source = "username.value")
    @Mapping(target = "displayName", source = "displayName.value")
    CurrentUserResponse toCurrentUserResponse(UserAccount userAccount);

    default Username toUsername(String username) {
        return Username.of(username);
    }

    default DisplayName toDisplayName(String displayName) {
        return DisplayName.of(displayName);
    }
}
