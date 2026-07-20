package com.api2api.domain.credential.model;

import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import java.util.Objects;

/** Aggregate that shares one model whitelist across multiple API credentials. */
public final class ModelGroup {

    private final ModelGroupId id;
    private final UserAccountId ownerUserId;
    private ModelGroupName name;
    private ModelWhitelist modelWhitelist;
    private final Instant createdAt;
    private Instant updatedAt;

    private ModelGroup(ModelGroupId id, UserAccountId ownerUserId, ModelGroupName name,
                       ModelWhitelist modelWhitelist, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Model group id must not be null");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        this.name = Objects.requireNonNull(name, "Model group name must not be null");
        this.modelWhitelist = Objects.requireNonNull(modelWhitelist, "Model whitelist must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created time must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time must not be null");
    }

    public static ModelGroup create(ModelGroupId id, UserAccountId ownerUserId, ModelGroupName name,
                                    ModelWhitelist modelWhitelist, Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return new ModelGroup(id, ownerUserId, name, modelWhitelist, now, now);
    }

    public static ModelGroup rehydrate(ModelGroupId id, UserAccountId ownerUserId, ModelGroupName name,
                                       ModelWhitelist modelWhitelist, Instant createdAt, Instant updatedAt) {
        return new ModelGroup(id, ownerUserId, name, modelWhitelist, createdAt, updatedAt);
    }

    public void update(ModelGroupName name, ModelWhitelist modelWhitelist, Instant now) {
        Objects.requireNonNull(name, "Model group name must not be null");
        Objects.requireNonNull(modelWhitelist, "Model whitelist must not be null");
        Objects.requireNonNull(now, "Current time must not be null");
        if (this.name.equals(name) && this.modelWhitelist.equals(modelWhitelist)) {
            return;
        }
        this.name = name;
        this.modelWhitelist = modelWhitelist;
        this.updatedAt = now;
    }

    public void assertOwnedBy(UserAccountId userId) {
        if (!ownerUserId.equals(Objects.requireNonNull(userId, "User id must not be null"))) {
            throw new IllegalStateException("ACCESS_DENIED: model group is owned by another user");
        }
    }

    public ModelGroupId getId() { return id; }
    public UserAccountId getOwnerUserId() { return ownerUserId; }
    public ModelGroupName getName() { return name; }
    public ModelWhitelist getModelWhitelist() { return modelWhitelist; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
