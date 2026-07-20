package com.api2api.application.credential;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.command.CreateModelGroupCommand;
import com.api2api.application.credential.command.DeleteModelGroupCommand;
import com.api2api.application.credential.command.UpdateModelGroupCommand;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelGroupApplicationService {

    @NonNull private final UserAccountRepository userAccountRepository;
    @NonNull private final ModelGroupRepository modelGroupRepository;
    @NonNull private final Clock clock;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ModelGroup> listMyGroups(UserAccountId ownerUserId) {
        assertUserPortal(ownerUserId);
        return modelGroupRepository.findByOwnerUserId(ownerUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelGroup createGroup(CreateModelGroupCommand command) {
        assertUserPortal(command.getOwnerUserId());
        assertNameAvailable(command.getOwnerUserId(), command.getName(), command.getModelGroupId());
        ModelGroup group = ModelGroup.create(command.getModelGroupId(), command.getOwnerUserId(),
                command.getName(), command.getModelWhitelist(), now());
        modelGroupRepository.save(group);
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelGroup updateGroup(UpdateModelGroupCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ModelGroup group = loadOwnedGroup(command.getModelGroupId(), command.getOwnerUserId());
        assertNameAvailable(command.getOwnerUserId(), command.getName(), command.getModelGroupId());
        group.update(command.getName(), command.getModelWhitelist(), now());
        modelGroupRepository.save(group);
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(DeleteModelGroupCommand command) {
        assertUserPortal(command.getOwnerUserId());
        ModelGroup group = loadOwnedGroup(command.getModelGroupId(), command.getOwnerUserId());
        if (modelGroupRepository.existsCredentialBinding(group.getId())) {
            throw new BusinessException("MODEL_GROUP_IN_USE");
        }
        modelGroupRepository.softDeleteById(group.getId(), now());
    }

    private ModelGroup loadOwnedGroup(ModelGroupId id, UserAccountId ownerUserId) {
        ModelGroup group = modelGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("MODEL_GROUP_NOT_FOUND"));
        group.assertOwnedBy(ownerUserId);
        return group;
    }

    private void assertUserPortal(UserAccountId ownerUserId) {
        UserAccount user = userAccountRepository.findById(ownerUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.assertCanAccess(AccessScope.USER_PORTAL);
    }

    private void assertNameAvailable(UserAccountId ownerUserId,
                                     ModelGroupName name,
                                     ModelGroupId excludedId) {
        if (modelGroupRepository.existsByOwnerAndNameExcludingId(ownerUserId, name, excludedId)) {
            throw new BusinessException("MODEL_GROUP_NAME_EXISTS");
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
