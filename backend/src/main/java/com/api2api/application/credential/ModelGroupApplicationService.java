package com.api2api.application.credential;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.ProviderModelAvailabilityService;
import com.api2api.application.credential.command.CreateModelGroupCommand;
import com.api2api.application.credential.command.DeleteModelGroupCommand;
import com.api2api.application.credential.command.UpdateModelGroupCommand;
import com.api2api.application.credential.dto.ModelGroupView;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelDailyLimits;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModelGroupApplicationService {

    @NonNull private final UserAccountRepository userAccountRepository;
    @NonNull private final ModelGroupRepository modelGroupRepository;
    @NonNull private final ModelGroupDailyUsageService dailyUsageService;
    @NonNull private final ProviderModelAvailabilityService modelAvailabilityService;
    @NonNull private final Clock clock;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ModelGroup> listMyGroups(UserAccountId ownerUserId) {
        assertUserPortal(ownerUserId);
        return modelGroupRepository.findAll();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ModelGroupView> listMyGroupViews(UserAccountId ownerUserId) {
        List<ModelGroup> groups = listMyGroups(ownerUserId);
        Map<ModelGroupId, Map<ModelName, BigDecimal>> usageByGroup = dailyUsageService.loadTodayUsageByGroup(ownerUserId);
        Set<ModelName> availableModels = modelAvailabilityService.availableModels();
        return groups.stream()
                .map(group -> ModelGroupView.of(group, usageByGroup.getOrDefault(group.getId(), Map.of()), availableModels))
                .toList();
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ModelGroupView> listAdminGroupViews(UserAccountId operator) {
        assertAdmin(operator);
        return listMyGroupViews(operator);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ModelGroupView viewGroup(ModelGroup group) {
        Map<ModelGroupId, Map<ModelName, BigDecimal>> usageByGroup =
                dailyUsageService.loadTodayUsageByGroup(group.getOwnerUserId());
        return ModelGroupView.of(group, usageByGroup.getOrDefault(group.getId(), Map.of()),
                modelAvailabilityService.availableModels());
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelGroup createGroup(CreateModelGroupCommand command) {
        assertAdmin(command.getOwnerUserId());
        assertNameAvailable(command.getName(), command.getModelGroupId());
        ModelGroup group = ModelGroup.create(command.getModelGroupId(), command.getOwnerUserId(),
                command.getName(), command.getModelWhitelist(), ModelDailyLimits.empty(), now());
        modelGroupRepository.save(group);
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelGroup updateGroup(UpdateModelGroupCommand command) {
        assertAdmin(command.getOwnerUserId());
        ModelGroup group = loadGroup(command.getModelGroupId());
        assertNameAvailable(command.getName(), command.getModelGroupId());
        group.update(command.getName(), command.getModelWhitelist(), ModelDailyLimits.empty(), now());
        modelGroupRepository.save(group);
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(DeleteModelGroupCommand command) {
        assertAdmin(command.getOwnerUserId());
        ModelGroup group = loadGroup(command.getModelGroupId());
        if (modelGroupRepository.existsCredentialBinding(group.getId())) {
            throw new BusinessException("MODEL_GROUP_IN_USE");
        }
        modelGroupRepository.softDeleteById(group.getId(), now());
    }

    private ModelGroup loadGroup(ModelGroupId id) {
        ModelGroup group = modelGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("MODEL_GROUP_NOT_FOUND"));
        return group;
    }

    private void assertAdmin(UserAccountId operator) {
        userAccountRepository.findById(operator).orElseThrow(() -> new BusinessException("USER_NOT_FOUND"))
                .assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
    }

    private void assertUserPortal(UserAccountId ownerUserId) {
        UserAccount user = userAccountRepository.findById(ownerUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND"));
        user.assertCanAccess(AccessScope.USER_PORTAL);
    }

    private void assertNameAvailable(ModelGroupName name,
                                     ModelGroupId excludedId) {
        if (modelGroupRepository.findAll().stream().anyMatch(group -> !group.getId().equals(excludedId) && group.getName().equals(name))) {
            throw new BusinessException("MODEL_GROUP_NAME_EXISTS");
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
