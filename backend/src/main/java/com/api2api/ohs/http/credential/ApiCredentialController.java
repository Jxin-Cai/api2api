package com.api2api.ohs.http.credential;

import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.credential.command.ChangeApiCredentialStatusCommand;
import com.api2api.application.credential.command.ChangeTokenLimitCommand;
import com.api2api.application.credential.command.CreateApiCredentialCommand;
import com.api2api.application.credential.command.RenameApiCredentialCommand;
import com.api2api.application.credential.command.ReplaceModelWhitelistCommand;
import com.api2api.application.credential.dto.ApiCredentialUsageView;
import com.api2api.application.credential.dto.RevealedApiCredentialSecret;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.IdentifierFactory;
import com.api2api.ohs.http.credential.ApiKeyMaterialHelper.ApiKeyMaterial;
import com.api2api.application.credential.ApiKeyMaterialProtector;
import com.api2api.ohs.http.credential.converter.ApiCredentialHttpConverter;
import com.api2api.ohs.http.credential.dto.ApiCredentialListResponse;
import com.api2api.ohs.http.credential.dto.ApiCredentialResponse;
import com.api2api.ohs.http.credential.dto.ChangeTokenLimitRequest;
import com.api2api.ohs.http.credential.dto.CreateApiCredentialRequest;
import com.api2api.ohs.http.credential.dto.CreateApiCredentialResponse;
import com.api2api.ohs.http.credential.dto.RenameApiCredentialRequest;
import com.api2api.ohs.http.credential.dto.ReplaceModelWhitelistRequest;
import com.api2api.ohs.http.credential.dto.RevealApiCredentialSecretResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for API credential management (frontend user portal).
 */
@RestController
@RequestMapping("/api/api-credentials")
@Validated
@RequiredArgsConstructor
public class ApiCredentialController {

    @NonNull
    private final ApiCredentialApplicationService apiCredentialApplicationService;

    @NonNull
    private final ApiCredentialHttpConverter apiCredentialHttpConverter;

    @NonNull
    private final CurrentUserContextResolver currentUserContextResolver;

    @NonNull
    private final IdentifierFactory identifierFactory;

    @NonNull
    private final ApiKeyMaterialHelper apiKeyMaterialHelper;

    @NonNull
    private final ApiKeyMaterialProtector apiKeyMaterialProtector;

    @GetMapping
    public ApiResponse<ApiCredentialListResponse> listMyCredentials(HttpServletRequest request) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        List<ApiCredentialUsageView> credentials = apiCredentialApplicationService.listMyCredentialUsageViews(ownerUserId);
        return ApiResponse.success(apiCredentialHttpConverter.toUsageListResponse(credentials));
    }

    @PostMapping
    public ApiResponse<CreateApiCredentialResponse> createCredential(
            @Valid @RequestBody CreateApiCredentialRequest createRequest,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = identifierFactory.newApiCredentialId();
        ApiKeyMaterial keyMaterial = apiKeyMaterialHelper.generateApiKeyMaterial();
        CreateApiCredentialCommand command = apiCredentialHttpConverter.toCreateCommand(
                createRequest,
                ownerUserId,
                apiCredentialId,
                keyMaterial,
                apiKeyMaterialProtector.protect(keyMaterial.getPlaintextKey())
        );
        ApiCredential credential = apiCredentialApplicationService.createCredential(command);
        return ApiResponse.success(apiCredentialHttpConverter.toCreateResponse(
                credential, keyMaterial.getPlaintextKey()));
    }

    @PostMapping("/{api-credential-id}/reveal")
    public ApiResponse<RevealApiCredentialSecretResponse> revealCredentialSecret(
            @PathVariable("api-credential-id") Long credentialId,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = ApiCredentialId.of(credentialId);
        RevealedApiCredentialSecret secret = apiCredentialApplicationService.revealSecret(
                apiCredentialHttpConverter.toRevealCommand(ownerUserId, apiCredentialId)
        );
        return ApiResponse.success(apiCredentialHttpConverter.toRevealResponse(secret));
    }

    @PatchMapping("/{api-credential-id}/name")
    public ApiResponse<ApiCredentialResponse> renameCredential(
            @PathVariable("api-credential-id") Long credentialId,
            @Valid @RequestBody RenameApiCredentialRequest renameRequest,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = ApiCredentialId.of(credentialId);
        RenameApiCredentialCommand command = apiCredentialHttpConverter.toRenameCommand(
                renameRequest, ownerUserId, apiCredentialId);
        ApiCredential credential = apiCredentialApplicationService.renameCredential(command);
        return ApiResponse.success(apiCredentialHttpConverter.toResponse(credential));
    }

    @PutMapping("/{api-credential-id}/model-whitelist")
    public ApiResponse<ApiCredentialResponse> replaceModelWhitelist(
            @PathVariable("api-credential-id") Long credentialId,
            @Valid @RequestBody ReplaceModelWhitelistRequest replaceRequest,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = ApiCredentialId.of(credentialId);
        ReplaceModelWhitelistCommand command = apiCredentialHttpConverter.toReplaceWhitelistCommand(
                replaceRequest, ownerUserId, apiCredentialId);
        ApiCredential credential = apiCredentialApplicationService.replaceModelWhitelist(command);
        return ApiResponse.success(apiCredentialHttpConverter.toResponse(credential));
    }

    @PutMapping("/{api-credential-id}/token-limit")
    public ApiResponse<ApiCredentialResponse> changeTokenLimit(
            @PathVariable("api-credential-id") Long credentialId,
            @Valid @RequestBody ChangeTokenLimitRequest changeRequest,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = ApiCredentialId.of(credentialId);
        ChangeTokenLimitCommand command = apiCredentialHttpConverter.toChangeTokenLimitCommand(
                changeRequest, ownerUserId, apiCredentialId);
        ApiCredential credential = apiCredentialApplicationService.changeTokenLimit(command);
        return ApiResponse.success(apiCredentialHttpConverter.toResponse(credential));
    }

    @PatchMapping("/{api-credential-id}/disable")
    public ApiResponse<ApiCredentialResponse> disableCredential(
            @PathVariable("api-credential-id") Long credentialId,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = ApiCredentialId.of(credentialId);
        ChangeApiCredentialStatusCommand command = apiCredentialHttpConverter.toChangeStatusCommand(
                ownerUserId, apiCredentialId);
        ApiCredential credential = apiCredentialApplicationService.disableCredential(command);
        return ApiResponse.success(apiCredentialHttpConverter.toResponse(credential));
    }

    @PatchMapping("/{api-credential-id}/enable")
    public ApiResponse<ApiCredentialResponse> enableCredential(
            @PathVariable("api-credential-id") Long credentialId,
            HttpServletRequest request
    ) {
        UserAccountId ownerUserId = currentUserContextResolver.resolveCurrentUserId(request);
        ApiCredentialId apiCredentialId = ApiCredentialId.of(credentialId);
        ChangeApiCredentialStatusCommand command = apiCredentialHttpConverter.toChangeStatusCommand(
                ownerUserId, apiCredentialId);
        ApiCredential credential = apiCredentialApplicationService.enableCredential(command);
        return ApiResponse.success(apiCredentialHttpConverter.toResponse(credential));
    }
}
