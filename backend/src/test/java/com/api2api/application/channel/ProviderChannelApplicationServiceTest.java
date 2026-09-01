package com.api2api.application.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.user.model.AccessScope;
import com.api2api.domain.user.model.UserAccount;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.domain.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProviderChannelApplicationServiceTest {

    @Test
    void test_lists_all_configured_channel_models_when_user_queries_provider_model_options() {
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        ProviderChannelRepository channelRepository = mock(ProviderChannelRepository.class);
        UserAccount user = mock(UserAccount.class);
        UserAccountId userId = UserAccountId.of(2L);
        ProviderChannel channel = mock(ProviderChannel.class);
        ChannelModelSupport firstModel = mock(ChannelModelSupport.class);
        ChannelModelSupport secondModel = mock(ChannelModelSupport.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(channel.id()).thenReturn(ProviderChannelId.of(9L));
        when(channel.supportedModels()).thenReturn(List.of(firstModel, secondModel));
        when(firstModel.requestedModel()).thenReturn(ModelName.of("claude-sonnet"));
        when(firstModel.upstreamProtocol()).thenReturn(ProtocolType.OPENAI_CHAT_COMPLETIONS);
        when(secondModel.requestedModel()).thenReturn(ModelName.of("gpt-4.1"));
        when(secondModel.upstreamProtocol()).thenReturn(ProtocolType.OPENAI_RESPONSES);
        when(channelRepository.findAll()).thenReturn(List.of(channel));
        ProviderChannelApplicationService service = new ProviderChannelApplicationService(
                userRepository,
                channelRepository,
                mock(ProviderModelFetchPort.class),
                Clock.fixed(Instant.parse("2026-08-31T00:00:00Z"), ZoneOffset.UTC)
        );

        var options = service.listProviderModelOptions(userId);

        assertThat(options).extracting(option -> option.model().value()).containsExactly("claude-sonnet", "gpt-4.1");
        verify(user).assertCanAccess(AccessScope.USER_PORTAL);
        verify(channelRepository).findAll();
    }

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
