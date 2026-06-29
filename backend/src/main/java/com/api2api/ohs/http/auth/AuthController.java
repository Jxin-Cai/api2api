package com.api2api.ohs.http.auth;

import com.api2api.application.user.UserAccountApplicationService;
import com.api2api.application.user.command.LoginCommand;
import com.api2api.application.user.command.UpdateCurrentUserProfileCommand;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.auth.converter.AuthHttpConverter;
import com.api2api.ohs.http.auth.dto.CurrentUserResponse;
import com.api2api.ohs.http.auth.dto.LoginRequest;
import com.api2api.ohs.http.auth.dto.LoginResponse;
import com.api2api.ohs.http.auth.dto.UpdateCurrentUserProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth controller for login and current user operations.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    @NonNull
    private final UserAccountApplicationService userAccountApplicationService;

    @NonNull
    private final AuthHttpConverter authHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        LoginCommand command = authHttpConverter.toLoginCommand(loginRequest);
        UserAccount userAccount = userAccountApplicationService.login(command);
        currentUserContextResolver.bindCurrentUser(request, userAccount.getId());
        return ApiResponse.success(authHttpConverter.toLoginResponse(userAccount));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        currentUserContextResolver.clearCurrentUser(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/current-user")
    public ApiResponse<CurrentUserResponse> getCurrentUser(HttpServletRequest request) {
        UserAccountId currentUserId = currentUserContextResolver.resolveCurrentUserId(request);
        UserAccount userAccount = userAccountApplicationService.getCurrentUser(currentUserId);
        return ApiResponse.success(authHttpConverter.toCurrentUserResponse(userAccount));
    }

    @PatchMapping("/current-user/profile")
    public ApiResponse<CurrentUserResponse> updateCurrentUserProfile(
            @Valid @RequestBody UpdateCurrentUserProfileRequest updateRequest,
            HttpServletRequest request
    ) {
        UserAccountId currentUserId = currentUserContextResolver.resolveCurrentUserId(request);
        UpdateCurrentUserProfileCommand command = authHttpConverter.toUpdateCurrentUserProfileCommand(
                updateRequest,
                currentUserId
        );
        UserAccount userAccount = userAccountApplicationService.updateCurrentUserProfile(command);
        return ApiResponse.success(authHttpConverter.toCurrentUserResponse(userAccount));
    }
}
