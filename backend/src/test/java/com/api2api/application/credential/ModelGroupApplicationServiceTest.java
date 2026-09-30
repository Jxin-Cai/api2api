package com.api2api.application.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.BusinessException;
import com.api2api.application.channel.ProviderModelAvailabilityService;
import com.api2api.application.credential.command.DeleteModelGroupCommand;
import com.api2api.domain.credential.model.ModelDailyLimits;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelGroupName;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import com.api2api.ohs.http.credential.converter.ModelGroupHttpConverter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelGroupApplicationServiceTest {

    private static final ModelName MODEL = ModelName.of("configured-model");
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");
    private final UserAccountId owner = UserAccountId.of(1L);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final ModelGroupRepository groups = mock(ModelGroupRepository.class);
    private final ModelGroupDailyUsageService usage = mock(ModelGroupDailyUsageService.class);
    private final ProviderModelAvailabilityService availability = mock(ProviderModelAvailabilityService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ModelGroupApplicationService service = new ModelGroupApplicationService(
            users, groups, usage, availability, clock);

    @Test
    void test_intersects_permissions_when_provider_also_supports_models_outside_group() {
        // Arrange
        ModelGroup group = configuredGroup();
        when(availability.availableModels()).thenReturn(Set.of(MODEL, ModelName.of("not-allowed")));

        // Act
        var view = service.viewGroup(group);

        // Assert
        assertThat(view.effectiveModels()).containsExactly(MODEL);
    }

    @Test
    void test_returns_empty_support_when_group_whitelist_is_empty() {
        // Arrange
        ModelGroup group = configuredGroup();
        group.update(group.getName(), ModelWhitelist.empty(), ModelDailyLimits.empty(), NOW);
        when(availability.availableModels()).thenReturn(Set.of(MODEL));

        // Act
        var view = service.viewGroup(group);

        // Assert
        assertThat(view.effectiveModels()).isEmpty();
    }

    @Test
    void test_updates_effective_group_support_when_provider_availability_changes() {
        // Arrange
        ModelGroup group = configuredGroup();
        when(users.findById(owner)).thenReturn(Optional.of(mock(UserAccount.class)));
        when(groups.findByOwnerUserId(owner)).thenReturn(List.of(group));
        when(availability.availableModels()).thenReturn(Set.of(MODEL)).thenReturn(Set.of()).thenReturn(Set.of(MODEL));

        // Act
        var before = service.listMyGroupViews(owner).get(0).effectiveModels();
        var disabled = service.listMyGroupViews(owner).get(0).effectiveModels();
        var restored = service.listMyGroupViews(owner).get(0).effectiveModels();

        // Assert
        assertThat(List.of(before, disabled, restored)).containsExactly(Set.of(MODEL), Set.of(), Set.of(MODEL));
    }

    @Test
    void test_preserves_saved_configuration_when_provider_support_disappears() {
        // Arrange
        ModelGroup group = configuredGroup();
        when(availability.availableModels()).thenReturn(Set.of());
        ModelGroupHttpConverter converter = new ModelGroupHttpConverter(new ModelDailyLimitWindow(clock, "UTC"));

        // Act
        var response = converter.toResponse(service.viewGroup(group));

        // Assert
        assertThat(response.getModelWhitelist()).containsExactly(MODEL.value());
        assertThat(response.getModelDailyLimits()).containsExactlyEntriesOf(Map.of(MODEL.value(), 100L));
        assertThat(response.getUpdatedAt()).isEqualTo(NOW);
        verify(groups, never()).save(group);
    }

    @Test
    void test_serializes_no_effective_models_when_group_has_no_provider_support() {
        // Arrange
        ModelGroup group = configuredGroup();
        when(availability.availableModels()).thenReturn(Set.of());
        ModelGroupHttpConverter converter = new ModelGroupHttpConverter(new ModelDailyLimitWindow(clock, "UTC"));

        // Act
        var response = converter.toResponse(service.viewGroup(group));

        // Assert
        assertThat(response.getEffectiveModels()).isEmpty();
    }

    @Test
    void test_keeps_daily_limit_status_when_provider_is_unavailable() {
        // Arrange
        ModelGroup group = configuredGroup();
        when(availability.availableModels()).thenReturn(Set.of());
        when(usage.loadTodayUsageByGroup(owner)).thenReturn(Map.of(group.getId(), Map.of(MODEL, BigDecimal.valueOf(100))));

        // Act
        var view = service.viewGroup(group);

        // Assert
        assertThat(view.rateLimitedModels()).containsExactly(MODEL);
    }

    private ModelGroup configuredGroup() {
        return ModelGroup.create(ModelGroupId.of(2L), owner, ModelGroupName.of("test-group"),
                ModelWhitelist.of(Set.of(MODEL)), ModelDailyLimits.of(Map.of(MODEL, 100L)), NOW);
    }

    @Test
    void test_rejects_deletion_when_group_has_bound_credentials() {
        // Arrange
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        ModelGroupRepository groupRepository = mock(ModelGroupRepository.class);
        UserAccount user = mock(UserAccount.class);
        ModelGroup group = mock(ModelGroup.class);
        UserAccountId userId = UserAccountId.of(1L);
        ModelGroupId groupId = ModelGroupId.of(2L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(group.getId()).thenReturn(groupId);
        when(groupRepository.existsCredentialBinding(groupId)).thenReturn(true);
        ModelGroupApplicationService service = new ModelGroupApplicationService(
                userRepository,
                groupRepository,
                mock(ModelGroupDailyUsageService.class),
                mock(ProviderModelAvailabilityService.class),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC)
        );
        DeleteModelGroupCommand command = DeleteModelGroupCommand.builder()
                .ownerUserId(userId)
                .modelGroupId(groupId)
                .build();

        // Act / Assert
        assertThatThrownBy(() -> service.deleteGroup(command))
                .isInstanceOf(BusinessException.class)
                .hasMessage("MODEL_GROUP_IN_USE");
        verify(groupRepository, never()).softDeleteById(groupId, Instant.parse("2026-07-20T00:00:00Z"));
    }
}
