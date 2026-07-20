package com.api2api.application.credential;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.BusinessException;
import com.api2api.application.credential.command.DeleteModelGroupCommand;
import com.api2api.domain.credential.model.ModelGroup;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModelGroupApplicationServiceTest {

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
