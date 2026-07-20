package com.api2api.domain.channel.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderChannelTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-20T00:00:00Z");
    private static final Instant REENABLED_AT = Instant.parse("2026-07-21T00:00:00Z");

    @Test
    void test_restoresAllRateLimitedModels_when_ChannelIsReenabled() {
        // Arrange
        ProviderChannel channel = channelWithModels(List.of(
                model(1L, "model-a", ChannelModelStatus.RATE_LIMITED),
                model(2L, "model-b", ChannelModelStatus.RATE_LIMITED)
        ));

        // Act
        channel.enable(REENABLED_AT);

        // Assert
        assertThat(channel.supportedModels())
                .allSatisfy(model -> {
                    assertThat(model.status()).isEqualTo(ChannelModelStatus.ENABLED);
                    assertThat(model.rateLimitedAt()).isNull();
                    assertThat(model.rateLimitResetAt()).isNull();
                });
    }

    @Test
    void test_keepsManuallyDisabledModelsDisabled_when_ChannelIsReenabled() {
        // Arrange
        ProviderChannel channel = channelWithModels(List.of(
                model(1L, "model-limited", ChannelModelStatus.RATE_LIMITED),
                model(2L, "model-disabled", ChannelModelStatus.DISABLED)
        ));

        // Act
        channel.enable(REENABLED_AT);

        // Assert
        assertThat(channel.supportedModels().get(1).status()).isEqualTo(ChannelModelStatus.DISABLED);
    }

    private static ProviderChannel channelWithModels(List<ChannelModelSupport> models) {
        return ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("provider"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("key-reference"),
                ProviderModelsPath.DEFAULT,
                0,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                models,
                ProviderChannelStatus.ENABLED,
                CREATED_AT,
                CREATED_AT
        );
    }

    private static ChannelModelSupport model(long id, String name, ChannelModelStatus status) {
        Instant rateLimitedAt = status == ChannelModelStatus.RATE_LIMITED ? CREATED_AT : null;
        Instant rateLimitResetAt = status == ChannelModelStatus.RATE_LIMITED ? CREATED_AT.plusSeconds(3600) : null;
        return ChannelModelSupport.rehydrate(
                ChannelModelSupportId.of(id),
                ModelName.of(name),
                ModelName.of(name),
                ProtocolType.OPENAI_RESPONSES,
                RoutePriority.of(1),
                false,
                status,
                rateLimitedAt,
                rateLimitResetAt,
                ModelSupportSource.MANUAL,
                CREATED_AT,
                CREATED_AT
        );
    }
}
