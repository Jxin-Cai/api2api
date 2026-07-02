package com.api2api.domain.credential.model;

import com.api2api.domain.user.model.UserAccountId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing one API key owned by a user.
 */
public final class ApiCredential {

    private final ApiCredentialId id;
    private final UserAccountId ownerUserId;
    private ApiCredentialName name;
    private final ApiKeyHash keyHash;
    private final ApiKeyPreview keyPreview;
    private final EncryptedApiKeyMaterial encryptedKeyMaterial;
    private ModelWhitelist modelWhitelist;
    private TokenLimit tokenLimit;
    private ApiCredentialStatus status;
    private Instant lastUsedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private ApiCredential(
            ApiCredentialId id,
            UserAccountId ownerUserId,
            ApiCredentialName name,
            ApiKeyHash keyHash,
            ApiKeyPreview keyPreview,
            EncryptedApiKeyMaterial encryptedKeyMaterial,
            ModelWhitelist modelWhitelist,
            TokenLimit tokenLimit,
            ApiCredentialStatus status,
            Instant lastUsedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "API credential id must not be null");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        this.name = Objects.requireNonNull(name, "API credential name must not be null");
        this.keyHash = Objects.requireNonNull(keyHash, "API key hash must not be null");
        this.keyPreview = Objects.requireNonNull(keyPreview, "API key preview must not be null");
        this.encryptedKeyMaterial = Objects.requireNonNull(encryptedKeyMaterial, "Encrypted API key material must not be null");
        this.modelWhitelist = Objects.requireNonNull(modelWhitelist, "Model whitelist must not be null");
        this.tokenLimit = Objects.requireNonNull(tokenLimit, "Token limit must not be null");
        this.status = Objects.requireNonNull(status, "API credential status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
        if (lastUsedAt != null && lastUsedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Last used time must not be before created time");
        }
        this.lastUsedAt = lastUsedAt;
    }

    public static ApiCredential create(
            ApiCredentialId id,
            UserAccountId ownerUserId,
            ApiCredentialName name,
            ApiKeyHash keyHash,
            ApiKeyPreview keyPreview,
            EncryptedApiKeyMaterial encryptedKeyMaterial,
            ModelWhitelist modelWhitelist,
            TokenLimit tokenLimit,
            Instant now
    ) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new ApiCredential(
                id,
                ownerUserId,
                name,
                keyHash,
                keyPreview,
                encryptedKeyMaterial,
                modelWhitelist,
                tokenLimit,
                ApiCredentialStatus.ACTIVE,
                null,
                now,
                now
        );
    }

    public static ApiCredential rehydrate(
            ApiCredentialId id,
            UserAccountId ownerUserId,
            ApiCredentialName name,
            ApiKeyHash keyHash,
            ApiKeyPreview keyPreview,
            EncryptedApiKeyMaterial encryptedKeyMaterial,
            ModelWhitelist modelWhitelist,
            TokenLimit tokenLimit,
            ApiCredentialStatus status,
            Instant lastUsedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ApiCredential(
                id,
                ownerUserId,
                name,
                keyHash,
                keyPreview,
                encryptedKeyMaterial,
                modelWhitelist,
                tokenLimit,
                status,
                lastUsedAt,
                createdAt,
                updatedAt
        );
    }

    public void rename(ApiCredentialName name, Instant now) {
        Objects.requireNonNull(name, "API credential name must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.name.equals(name)) {
            return;
        }
        this.name = name;
        this.updatedAt = now;
    }

    public void replaceModelWhitelist(ModelWhitelist whitelist, Instant now) {
        Objects.requireNonNull(whitelist, "Model whitelist must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.modelWhitelist.equals(whitelist)) {
            return;
        }
        this.modelWhitelist = whitelist;
        this.updatedAt = now;
    }

    public void changeTokenLimit(TokenLimit tokenLimit, long currentConsumedTokens, Instant now) {
        Objects.requireNonNull(tokenLimit, "Token limit must not be null");
        validateConsumedTokens(currentConsumedTokens);
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.tokenLimit.equals(tokenLimit)) {
            return;
        }
        this.tokenLimit = tokenLimit;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.status == ApiCredentialStatus.DISABLED) {
            return;
        }
        this.status = ApiCredentialStatus.DISABLED;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.status == ApiCredentialStatus.ACTIVE) {
            return;
        }
        this.status = ApiCredentialStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void assertUsable() {
        if (this.status != ApiCredentialStatus.ACTIVE) {
            throw new IllegalStateException("API_CREDENTIAL_DISABLED: API credential is disabled");
        }
    }

    public void assertOwnedBy(UserAccountId userId) {
        Objects.requireNonNull(userId, "User id must not be null");
        if (!this.ownerUserId.equals(userId)) {
            throw new IllegalStateException("ACCESS_DENIED: API credential is owned by another user");
        }
    }

    public boolean allowsModel(ModelName requestedModel) {
        Objects.requireNonNull(requestedModel, "Requested model must not be null");
        return this.modelWhitelist.contains(requestedModel);
    }

    public void assertModelAllowed(ModelName requestedModel) {
        if (!allowsModel(requestedModel)) {
            throw new IllegalStateException("MODEL_NOT_ALLOWED: requested model is not allowed by API credential whitelist");
        }
    }

    public boolean isQuotaExhausted(long currentConsumedTokens) {
        validateConsumedTokens(currentConsumedTokens);
        return this.tokenLimit.isExceededBy(currentConsumedTokens);
    }

    public void assertQuotaAvailable(long currentConsumedTokens) {
        if (isQuotaExhausted(currentConsumedTokens)) {
            throw new IllegalStateException("TOKEN_QUOTA_EXHAUSTED: API credential token quota is exhausted");
        }
    }

    public TokenConsumptionDecision evaluateConsumption(long currentConsumedTokens, TokenUsageDelta delta) {
        validateConsumedTokens(currentConsumedTokens);
        Objects.requireNonNull(delta, "Token usage delta must not be null");
        if (isQuotaExhausted(currentConsumedTokens)) {
            return TokenConsumptionDecision.of(
                    TokenConsumptionDecision.Result.EXHAUSTED_BEFORE_REQUEST,
                    currentConsumedTokens,
                    currentConsumedTokens,
                    tokenLimit
            );
        }

        long consumedAfter = Math.addExact(currentConsumedTokens, delta.totalTokens());
        TokenConsumptionDecision.Result result = tokenLimit.isExceededBy(consumedAfter)
                ? TokenConsumptionDecision.Result.EXHAUSTED_AFTER_CONSUMPTION
                : TokenConsumptionDecision.Result.ACCEPTED;
        return TokenConsumptionDecision.of(result, currentConsumedTokens, consumedAfter, tokenLimit);
    }

    public void markUsed(Instant usedAt) {
        Objects.requireNonNull(usedAt, "Used time must not be null");
        if (usedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Used time must not be before created time");
        }
        if (lastUsedAt != null && usedAt.isBefore(lastUsedAt)) {
            return;
        }
        this.lastUsedAt = usedAt;
        this.updatedAt = usedAt;
    }

    private static void validateConsumedTokens(long currentConsumedTokens) {
        if (currentConsumedTokens < 0) {
            throw new IllegalArgumentException("Current consumed tokens must not be negative");
        }
    }

    public ApiCredentialId getId() {
        return id;
    }

    public UserAccountId getOwnerUserId() {
        return ownerUserId;
    }

    public ApiCredentialName getName() {
        return name;
    }

    public ApiKeyHash getKeyHash() {
        return keyHash;
    }

    public ApiKeyPreview getKeyPreview() {
        return keyPreview;
    }

    public EncryptedApiKeyMaterial getEncryptedKeyMaterial() {
        return encryptedKeyMaterial;
    }

    public ModelWhitelist getModelWhitelist() {
        return modelWhitelist;
    }

    public TokenLimit getTokenLimit() {
        return tokenLimit;
    }

    public ApiCredentialStatus getStatus() {
        return status;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
