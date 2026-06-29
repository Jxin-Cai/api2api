package com.api2api.ohs.http.admin;

import com.api2api.application.user.UserAccountApplicationService;
import com.api2api.application.user.command.ChangeUserDisplayNameCommand;
import com.api2api.application.user.command.ChangeUserRoleCommand;
import com.api2api.application.user.command.ChangeUserStatusCommand;
import com.api2api.application.user.command.CreateUserCommand;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.IdentifierFactory;
import com.api2api.ohs.http.admin.converter.AdminUserHttpConverter;
import com.api2api.ohs.http.admin.dto.AdminChangeUserDisplayNameRequest;
import com.api2api.ohs.http.admin.dto.AdminChangeUserRoleRequest;
import com.api2api.ohs.http.admin.dto.AdminCreateUserRequest;
import com.api2api.ohs.http.admin.dto.UserAccountResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for user account management.
 */
@RestController
@RequestMapping("/api/admin/users")
@Validated
@RequiredArgsConstructor
public class AdminUserController {

    @NonNull
    private final UserAccountApplicationService userAccountApplicationService;

    @NonNull
    private final AdminUserHttpConverter adminUserHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @NonNull
    private final IdentifierFactory identifierFactory;

    @PostMapping
    public ApiResponse<UserAccountResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest createRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UserAccountId newUserAccountId = identifierFactory.newUserAccountId();
        CreateUserCommand command = adminUserHttpConverter.toCreateUserCommand(
                createRequest, operatorUserId, newUserAccountId);
        UserAccount userAccount = userAccountApplicationService.createUser(command);
        return ApiResponse.success(adminUserHttpConverter.toUserAccountResponse(userAccount));
    }

    @PatchMapping("/{user-id}/display-name")
    public ApiResponse<UserAccountResponse> changeDisplayName(
            @PathVariable("user-id") Long userId,
            @Valid @RequestBody AdminChangeUserDisplayNameRequest changeRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UserAccountId targetUserId = UserAccountId.of(userId);
        ChangeUserDisplayNameCommand command = adminUserHttpConverter.toChangeDisplayNameCommand(
                changeRequest, operatorUserId, targetUserId);
        UserAccount userAccount = userAccountApplicationService.changeDisplayName(command);
        return ApiResponse.success(adminUserHttpConverter.toUserAccountResponse(userAccount));
    }

    @PatchMapping("/{user-id}/role")
    public ApiResponse<UserAccountResponse> changeRole(
            @PathVariable("user-id") Long userId,
            @Valid @RequestBody AdminChangeUserRoleRequest changeRequest,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UserAccountId targetUserId = UserAccountId.of(userId);
        ChangeUserRoleCommand command = adminUserHttpConverter.toChangeRoleCommand(
                changeRequest, operatorUserId, targetUserId);
        UserAccount userAccount = userAccountApplicationService.changeRole(command);
        return ApiResponse.success(adminUserHttpConverter.toUserAccountResponse(userAccount));
    }

    @PatchMapping("/{user-id}/disable")
    public ApiResponse<UserAccountResponse> disableUser(
            @PathVariable("user-id") Long userId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UserAccountId targetUserId = UserAccountId.of(userId);
        ChangeUserStatusCommand command = adminUserHttpConverter.toChangeUserStatusCommand(
                operatorUserId, targetUserId);
        UserAccount userAccount = userAccountApplicationService.disableUser(command);
        return ApiResponse.success(adminUserHttpConverter.toUserAccountResponse(userAccount));
    }

    @PatchMapping("/{user-id}/enable")
    public ApiResponse<UserAccountResponse> enableUser(
            @PathVariable("user-id") Long userId,
            HttpServletRequest request
    ) {
        UserAccountId operatorUserId = currentUserContextResolver.resolveOperatorUserId(request);
        UserAccountId targetUserId = UserAccountId.of(userId);
        ChangeUserStatusCommand command = adminUserHttpConverter.toChangeUserStatusCommand(
                operatorUserId, targetUserId);
        UserAccount userAccount = userAccountApplicationService.enableUser(command);
        return ApiResponse.success(adminUserHttpConverter.toUserAccountResponse(userAccount));
    }
}
