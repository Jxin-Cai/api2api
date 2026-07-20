package com.api2api.infr.repository.credential.converter;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.model.ApiCredentialStatus;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ApiKeyPreview;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.model.TokenLimit;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.infr.repository.credential.po.ApiCredentialPO;
import com.api2api.infr.repository.credential.ModelWhitelistTextCodec;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converts API credential aggregate to persistence object.
 */
@Mapper(config = MapStructConfig.class)
public interface ApiCredentialPersistenceConverter {

    @Mapping(target = "id", expression = "java(apiCredential.getId().value())")
    @Mapping(target = "ownerUserId", expression = "java(apiCredential.getOwnerUserId().getValue())")
    @Mapping(target = "name", expression = "java(apiCredential.getName().getValue())")
    @Mapping(target = "keyHash", expression = "java(apiCredential.getKeyHash().getValue())")
    @Mapping(target = "keyPreview", expression = "java(apiCredential.getKeyPreview().getValue())")
    @Mapping(target = "encryptedKeyMaterial", expression = "java(apiCredential.getEncryptedKeyMaterial().getCiphertext())")
    @Mapping(target = "keyMaterialNonce", expression = "java(apiCredential.getEncryptedKeyMaterial().getNonce())")
    @Mapping(target = "keyMaterialVersion", expression = "java(apiCredential.getEncryptedKeyMaterial().getVersion())")
    @Mapping(target = "modelGroupId", expression = "java(apiCredential.getModelGroupId().value())")
    @Mapping(target = "modelWhitelist", expression = "java(toModelWhitelistText(apiCredential.getModelWhitelist()))")
    @Mapping(target = "tokenLimit", expression = "java(apiCredential.getTokenLimit().getValue())")
    @Mapping(target = "status", expression = "java(apiCredential.getStatus().name())")
    @Mapping(target = "lastUsedTime", source = "lastUsedAt")
    @Mapping(target = "createdTime", source = "createdAt")
    @Mapping(target = "updatedTime", source = "updatedAt")
    @Mapping(target = "deleted", constant = "false")
    ApiCredentialPO toPO(ApiCredential apiCredential);

    default ApiCredential toDomain(ApiCredentialPO po) {
        return ApiCredential.rehydrate(
                ApiCredentialId.of(po.getId()),
                UserAccountId.of(po.getOwnerUserId()),
                ApiCredentialName.of(po.getName()),
                ApiKeyHash.of(po.getKeyHash()),
                ApiKeyPreview.of(po.getKeyPreview()),
                EncryptedApiKeyMaterial.of(
                        po.getEncryptedKeyMaterial(),
                        po.getKeyMaterialNonce(),
                        po.getKeyMaterialVersion() <= 0 ? 1 : po.getKeyMaterialVersion()
                ),
                ModelGroupId.of(po.getModelGroupId()),
                toModelWhitelist(po.getModelWhitelist()),
                TokenLimit.of(po.getTokenLimit()),
                ApiCredentialStatus.valueOf(po.getStatus()),
                po.getLastUsedTime(),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }

    default String toModelWhitelistText(ModelWhitelist whitelist) {
        return ModelWhitelistTextCodec.encode(whitelist);
    }

    default ModelWhitelist toModelWhitelist(String text) {
        return ModelWhitelistTextCodec.decode(text);
    }
}
