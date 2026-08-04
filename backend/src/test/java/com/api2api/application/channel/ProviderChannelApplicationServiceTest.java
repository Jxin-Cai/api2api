package com.api2api.application.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProviderChannelApplicationServiceTest {

    @Test
    void test_restores_all_rate_limits_when_operator_is_admin() {
        // Arrange
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        ProviderChannelRepository channelRepository = mock(ProviderChannelRepository.class);
        ProviderModelFetchPort modelFetchPort = mock(ProviderModelFetchPort.class);
        UserAccount operator = mock(UserAccount.class);
        UserAccountId operatorId = UserAccountId.of(1L);
        Instant restoredAt = Instant.parse("2026-08-04T00:00:00Z");
        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));
        when(channelRepository.restoreAllModelRateLimits(restoredAt)).thenReturn(3);
        ProviderChannelApplicationService service = new ProviderChannelApplicationService(
                userRepository,
                channelRepository,
                modelFetchPort,
                Clock.fixed(restoredAt, ZoneOffset.UTC)
        );

        // Act
        int restoredCount = service.resetAllRateLimits(operatorId);

        // Assert
        assertThat(restoredCount).isEqualTo(3);
        verify(operator).assertCanAccess(AccessScope.ADMIN_BACKOFFICE);
        verify(channelRepository).restoreAllModelRateLimits(restoredAt);
    }
}
