package com.api2api.infr.repository.credential.converter;

import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.model.ApiCredentialStatus;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.credential.model.ApiKeyPreview;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.model.TokenLimit;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.lib.mapping.MapStructConfig;
import com.api2api.infr.repository.credential.po.ApiCredentialPO;
import java.util.LinkedHashSet;
import java.util.Set;
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
                toModelWhitelist(po.getModelWhitelist()),
                TokenLimit.of(po.getTokenLimit()),
                ApiCredentialStatus.valueOf(po.getStatus()),
                po.getLastUsedTime(),
                po.getCreatedTime(),
                po.getUpdatedTime()
        );
    }

    default String toModelWhitelistText(ModelWhitelist whitelist) {
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (ModelName model : whitelist.getModels()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(model.getValue())).append('"');
            first = false;
        }
        return builder.append(']').toString();
    }

    default ModelWhitelist toModelWhitelist(String text) {
        if (text == null || text.isBlank() || "[]".equals(text.trim())) {
            return ModelWhitelist.empty();
        }
        String normalized = text.trim();
        Set<ModelName> models = new LinkedHashSet<>();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            String body = normalized.substring(1, normalized.length() - 1).trim();
            if (!body.isEmpty()) {
                for (String item : body.split(",")) {
                    String value = item.trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    value = value.replace("\\\"", "\"").replace("\\\\", "\\");
                    if (!value.isBlank()) {
                        models.add(ModelName.of(value));
                    }
                }
            }
        } else {
            for (String item : normalized.split(",")) {
                if (!item.isBlank()) {
                    models.add(ModelName.of(item));
                }
            }
        }
        return ModelWhitelist.of(models);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
