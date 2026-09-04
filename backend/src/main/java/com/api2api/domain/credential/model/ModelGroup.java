package com.api2api.domain.credential.model;

import com.api2api.domain.user.model.UserAccountId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Aggregate that shares one model whitelist and per-model daily caps across multiple API credentials. */
public final class ModelGroup {

    private final ModelGroupId id;
    private final UserAccountId ownerUserId;
    private ModelGroupName name;
    private ModelWhitelist modelWhitelist;
    private ModelDailyLimits modelDailyLimits;
    private final Instant createdAt;
    private Instant updatedAt;

    private ModelGroup(ModelGroupId id, UserAccountId ownerUserId, ModelGroupName name,
                       ModelWhitelist modelWhitelist, ModelDailyLimits modelDailyLimits,
                       Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Model group id must not be null");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        this.name = Objects.requireNonNull(name, "Model group name must not be null");
        this.modelWhitelist = Objects.requireNonNull(modelWhitelist, "Model whitelist must not be null");
        this.modelDailyLimits = Objects.requireNonNull(modelDailyLimits, "Model daily limits must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
    }

    public static ModelGroup create(ModelGroupId id, UserAccountId ownerUserId, ModelGroupName name,
                                    ModelWhitelist modelWhitelist, ModelDailyLimits modelDailyLimits, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new ModelGroup(id, ownerUserId, name, modelWhitelist, modelDailyLimits, now, now);
    }

    public static ModelGroup rehydrate(ModelGroupId id, UserAccountId ownerUserId, ModelGroupName name,
                                       ModelWhitelist modelWhitelist, ModelDailyLimits modelDailyLimits,
                                       Instant createdAt, Instant updatedAt) {
        return new ModelGroup(id, ownerUserId, name, modelWhitelist, modelDailyLimits, createdAt, updatedAt);
    }

    public void update(ModelGroupName name, ModelWhitelist modelWhitelist, ModelDailyLimits modelDailyLimits,
                       Instant now) {
        Objects.requireNonNull(name, "Model group name must not be null");
        Objects.requireNonNull(modelWhitelist, "Model whitelist must not be null");
        Objects.requireNonNull(modelDailyLimits, "Model daily limits must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.name.equals(name) && this.modelWhitelist.equals(modelWhitelist)
                && this.modelDailyLimits.equals(modelDailyLimits)) {
            return;
        }
        this.name = name;
        this.modelWhitelist = modelWhitelist;
        this.modelDailyLimits = modelDailyLimits;
        this.updatedAt = now;
    }

    public void assertOwnedBy(UserAccountId userId) {
        if (!ownerUserId.equals(Objects.requireNonNull(userId, "User id must not be null"))) {
            throw new IllegalStateException("ACCESS_DENIED: model group is owned by another user");
        }
    }

    /**
     * Rejects the request when today's group-wide consumption of {@code model} has reached its daily cap.
     * The same code is mapped to a 429 rate-limit error at the gateway boundary.
     */
    public void assertDailyLimitAvailable(ModelName model, BigDecimal consumedTodayTokens) {
        if (modelDailyLimits.isExceeded(model, consumedTodayTokens)) {
            throw new IllegalStateException(
                    "MODEL_DAILY_LIMIT_EXCEEDED: daily token limit of model " + model.getValue()
                            + " has been reached for this model group");
        }
    }

    /** Models whose daily cap is reached given today's per-model consumption of this group. */
    public Set<ModelName> rateLimitedModels(Map<ModelName, BigDecimal> consumedTodayByModel) {
        Objects.requireNonNull(consumedTodayByModel, "Consumed tokens by model must not be null");
        Set<ModelName> limited = new LinkedHashSet<>();
        modelDailyLimits.limits().keySet().forEach(model -> {
            BigDecimal consumed = consumedTodayByModel.getOrDefault(model, BigDecimal.ZERO);
            if (modelDailyLimits.isExceeded(model, consumed)) {
                limited.add(model);
            }
        });
        return Set.copyOf(limited);
    }

    public ModelGroupId getId() { return id; }
    public UserAccountId getOwnerUserId() { return ownerUserId; }
    public ModelGroupName getName() { return name; }
    public ModelWhitelist getModelWhitelist() { return modelWhitelist; }
    public ModelDailyLimits getModelDailyLimits() { return modelDailyLimits; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
