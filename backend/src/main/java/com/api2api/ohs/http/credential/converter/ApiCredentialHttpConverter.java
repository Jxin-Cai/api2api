package com.api2api.ohs.http.credential.converter;

import com.api2api.application.credential.command.ChangeApiCredentialStatusCommand;
import com.api2api.application.credential.command.ChangeTokenLimitCommand;
import com.api2api.application.credential.command.CreateApiCredentialCommand;
import com.api2api.application.credential.command.RevealApiCredentialSecretCommand;
import com.api2api.application.credential.command.RenameApiCredentialCommand;
import com.api2api.application.credential.command.ReplaceModelWhitelistCommand;
import com.api2api.application.credential.dto.ApiCredentialUsageView;
import com.api2api.application.credential.dto.RevealedApiCredentialSecret;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.model.TokenLimit;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.ohs.http.credential.ApiKeyMaterialHelper.ApiKeyMaterial;
import com.api2api.ohs.http.credential.dto.ApiCredentialListResponse;
import com.api2api.ohs.http.credential.dto.ApiCredentialResponse;
import com.api2api.ohs.http.credential.dto.ChangeTokenLimitRequest;
import com.api2api.ohs.http.credential.dto.CreateApiCredentialRequest;
import com.api2api.ohs.http.credential.dto.CreateApiCredentialResponse;
import com.api2api.ohs.http.credential.dto.RenameApiCredentialRequest;
import com.api2api.ohs.http.credential.dto.ReplaceModelWhitelistRequest;
import com.api2api.ohs.http.credential.dto.RevealApiCredentialSecretResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts API credential HTTP models to application commands and responses.
 */
@Mapper(config = MapStructConfig.class)
public abstract class ApiCredentialHttpConverter {

    public CreateApiCredentialCommand toCreateCommand(
            CreateApiCredentialRequest request,
            UserAccountId ownerUserId,
            ApiCredentialId apiCredentialId,
            ApiKeyMaterial keyMaterial,
            EncryptedApiKeyMaterial encryptedKeyMaterial
    ) {
        return CreateApiCredentialCommand.builder()
                .ownerUserId(ownerUserId)
                .apiCredentialId(apiCredentialId)
                .name(ApiCredentialName.of(request.getName()))
                .keyHash(keyMaterial.getKeyHash())
                .keyPreview(keyMaterial.getKeyPreview())
                .encryptedKeyMaterial(encryptedKeyMaterial)
                .modelWhitelist(toModelWhitelist(request.getModelWhitelist()))
                .tokenLimit(TokenLimit.of(request.getTokenLimit()))
                .build();
    }

    public RenameApiCredentialCommand toRenameCommand(
            RenameApiCredentialRequest request,
            UserAccountId ownerUserId,
            ApiCredentialId apiCredentialId
    ) {
        return RenameApiCredentialCommand.builder()
                .ownerUserId(ownerUserId)
                .apiCredentialId(apiCredentialId)
                .name(ApiCredentialName.of(request.getName()))
                .build();
    }

    public ReplaceModelWhitelistCommand toReplaceWhitelistCommand(
            ReplaceModelWhitelistRequest request,
            UserAccountId ownerUserId,
            ApiCredentialId apiCredentialId
    ) {
        return ReplaceModelWhitelistCommand.builder()
                .ownerUserId(ownerUserId)
                .apiCredentialId(apiCredentialId)
                .modelWhitelist(toModelWhitelist(request.getModelWhitelist()))
                .build();
    }

    public ChangeTokenLimitCommand toChangeTokenLimitCommand(
            ChangeTokenLimitRequest request,
            UserAccountId ownerUserId,
            ApiCredentialId apiCredentialId
    ) {
        return ChangeTokenLimitCommand.builder()
                .ownerUserId(ownerUserId)
                .apiCredentialId(apiCredentialId)
                .tokenLimit(TokenLimit.of(request.getTokenLimit()))
                .build();
    }

    public ChangeApiCredentialStatusCommand toChangeStatusCommand(
            UserAccountId ownerUserId,
            ApiCredentialId apiCredentialId
    ) {
        return ChangeApiCredentialStatusCommand.builder()
                .ownerUserId(ownerUserId)
                .apiCredentialId(apiCredentialId)
                .build();
    }

    public ApiCredentialListResponse toListResponse(List<ApiCredential> credentials) {
        return ApiCredentialListResponse.builder()
                .credentials(credentials.stream().map(this::toResponse).toList())
                .build();
    }

    public ApiCredentialListResponse toUsageListResponse(List<ApiCredentialUsageView> credentialViews) {
        return ApiCredentialListResponse.builder()
                .credentials(credentialViews.stream().map(this::toResponse).toList())
                .build();
    }

    public RevealApiCredentialSecretCommand toRevealCommand(UserAccountId ownerUserId, ApiCredentialId apiCredentialId) {
        return RevealApiCredentialSecretCommand.builder()
                .ownerUserId(ownerUserId)
                .apiCredentialId(apiCredentialId)
                .build();
    }

    public RevealApiCredentialSecretResponse toRevealResponse(RevealedApiCredentialSecret secret) {
        return RevealApiCredentialSecretResponse.builder()
                .apiCredentialId(secret.getApiCredentialId().value())
                .keyPreview(secret.getKeyPreview().getValue())
                .plaintextApiKey(secret.getPlaintextApiKey())
                .build();
    }

    public CreateApiCredentialResponse toCreateResponse(ApiCredential credential, String plainApiKey) {
        return CreateApiCredentialResponse.builder()
                .credential(toResponse(credential))
                .plaintextApiKey(plainApiKey)
                .build();
    }

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "ownerUserId", source = "ownerUserId.value")
    @Mapping(target = "name", source = "name.value")
    @Mapping(target = "keyPreview", source = "keyPreview.value")
    @Mapping(target = "modelWhitelist", expression = "java(toModelWhitelistResponse(credential.getModelWhitelist()))")
    @Mapping(target = "tokenLimit", source = "tokenLimit.value")
    @Mapping(target = "consumedTokens", constant = "0L")
    @Mapping(target = "remainingTokens", ignore = true)
    public abstract ApiCredentialResponse toResponse(ApiCredential credential);

    @Mapping(target = "id", source = "credential.id.value")
    @Mapping(target = "ownerUserId", source = "credential.ownerUserId.value")
    @Mapping(target = "name", source = "credential.name.value")
    @Mapping(target = "keyPreview", source = "credential.keyPreview.value")
    @Mapping(target = "modelWhitelist", expression = "java(toModelWhitelistResponse(view.getCredential().getModelWhitelist()))")
    @Mapping(target = "tokenLimit", source = "credential.tokenLimit.value")
    @Mapping(target = "status", source = "credential.status")
    @Mapping(target = "lastUsedAt", source = "credential.lastUsedAt")
    @Mapping(target = "createdAt", source = "credential.createdAt")
    @Mapping(target = "updatedAt", source = "credential.updatedAt")
    @Mapping(target = "consumedTokens", source = "consumedTokens")
    @Mapping(target = "remainingTokens", source = "remainingTokens")
    public abstract ApiCredentialResponse toResponse(ApiCredentialUsageView view);

    protected ModelWhitelist toModelWhitelist(List<String> modelNames) {
        Set<ModelName> models = new LinkedHashSet<>();
        for (String modelName : modelNames) {
            models.add(ModelName.of(modelName));
        }
        return ModelWhitelist.of(models);
    }

    protected List<String> toModelWhitelistResponse(ModelWhitelist whitelist) {
        return whitelist.getModels().stream()
                .map(ModelName::getValue)
                .toList();
    }
}
