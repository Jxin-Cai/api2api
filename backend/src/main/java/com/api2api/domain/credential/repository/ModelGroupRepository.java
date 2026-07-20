package com.api2api.domain.credential.repository;

import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.user.model.UserAccountId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ModelGroupRepository {
    void save(ModelGroup modelGroup);
    Optional<ModelGroup> findById(ModelGroupId id);
    List<ModelGroup> findByOwnerUserId(UserAccountId ownerUserId);
    boolean existsByOwnerAndNameExcludingId(UserAccountId ownerUserId, ModelGroupName name, ModelGroupId excludedId);
    boolean existsCredentialBinding(ModelGroupId id);
    void softDeleteById(ModelGroupId id, Instant deletedAt);
}
