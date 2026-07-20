package com.api2api.infr.repository.credential;

import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.repository.credential.mapper.ModelGroupMapper;
import com.api2api.infr.repository.credential.po.ModelGroupPO;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ModelGroupRepositoryImpl implements ModelGroupRepository {

    @NonNull
    private final ModelGroupMapper mapper;

    @Override
    public void save(ModelGroup group) {
        Objects.requireNonNull(group, "Model group must not be null");
        ModelGroupPO po = toPO(group);
        if (mapper.selectById(group.getId().value()) == null) {
            mapper.insert(po);
        } else {
            mapper.update(po);
        }
    }

    @Override
    public Optional<ModelGroup> findById(ModelGroupId id) {
        Objects.requireNonNull(id, "Model group id must not be null");
        return Optional.ofNullable(mapper.selectById(id.value())).map(this::toDomain);
    }

    @Override
    public List<ModelGroup> findByOwnerUserId(UserAccountId ownerUserId) {
        Objects.requireNonNull(ownerUserId, "Owner user id must not be null");
        return mapper.selectByOwnerUserId(ownerUserId.getValue()).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByOwnerAndNameExcludingId(UserAccountId ownerUserId, ModelGroupName name,
                                                   ModelGroupId excludedId) {
        return mapper.existsByOwnerAndNameExcludingId(
                Objects.requireNonNull(ownerUserId, "Owner user id must not be null").getValue(),
                Objects.requireNonNull(name, "Model group name must not be null").getValue(),
                Objects.requireNonNull(excludedId, "Excluded model group id must not be null").value()
        );
    }

    @Override
    public boolean existsCredentialBinding(ModelGroupId id) {
        return mapper.existsCredentialBinding(Objects.requireNonNull(id, "Model group id must not be null").value());
    }

    @Override
    public void softDeleteById(ModelGroupId id, Instant deletedAt) {
        mapper.softDeleteById(Objects.requireNonNull(id, "Model group id must not be null").value(),
                Objects.requireNonNull(deletedAt, "Deleted time must not be null"));
    }

    private ModelGroupPO toPO(ModelGroup group) {
        return ModelGroupPO.builder()
                .id(group.getId().value())
                .ownerUserId(group.getOwnerUserId().getValue())
                .name(group.getName().getValue())
                .modelWhitelist(ModelWhitelistTextCodec.encode(group.getModelWhitelist()))
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .deleted(false)
                .build();
    }

    private ModelGroup toDomain(ModelGroupPO po) {
        return ModelGroup.rehydrate(ModelGroupId.of(po.getId()), UserAccountId.of(po.getOwnerUserId()),
                ModelGroupName.of(po.getName()), ModelWhitelistTextCodec.decode(po.getModelWhitelist()),
                po.getCreatedAt(), po.getUpdatedAt());
    }
}
