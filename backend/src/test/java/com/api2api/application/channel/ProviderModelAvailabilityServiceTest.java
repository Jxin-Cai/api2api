package com.api2api.application.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.routing.model.RoutingRequest;
import com.api2api.domain.routing.service.DefaultRoutingPolicyService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProviderModelAvailabilityServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");
    private static final ModelName MODEL = ModelName.of("public-model");
    private static final ProtocolType PROTOCOL = ProtocolType.OPENAI_RESPONSES;
    private final ProviderChannelRepository repository = mock(ProviderChannelRepository.class);
    private final ProviderModelAvailabilityService service = new ProviderModelAvailabilityService(
            repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void test_performs_only_reads_when_availability_is_requested_repeatedly() {
        // Arrange
        ProviderChannel channel = channel(1);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        service.availableModels();
        service.availableModels();
        service.availableModels();

        // Assert
        verify(repository, times(3)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void test_preserves_model_status_and_timestamps_when_expired_isolation_is_displayed() {
        // Arrange
        ProviderChannel channel = channel(1);
        ChannelModelSupport isolated = support(ChannelModelStatus.RATE_LIMITED, NOW.minusSeconds(1));
        channel.replaceModels(List.of(isolated), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        service.availableModels();
        service.availableModels();

        // Assert
        assertThat(isolated.status()).isEqualTo(ChannelModelStatus.RATE_LIMITED);
        assertThat(isolated.rateLimitResetAt()).isEqualTo(NOW.minusSeconds(1));
        assertThat(isolated.updatedAt()).isEqualTo(NOW);
        assertThat(channel.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void test_keeps_results_idempotent_when_channel_status_changes_are_repeated() {
        // Arrange
        ProviderChannel channel = channel(1);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        channel.disable(NOW);
        Set<ModelName> firstDisabled = service.availableModels();
        channel.disable(NOW.plusSeconds(1));
        Set<ModelName> repeatedDisabled = service.availableModels();
        channel.enable(NOW.plusSeconds(2));
        Set<ModelName> firstEnabled = service.availableModels();
        channel.enable(NOW.plusSeconds(3));
        Set<ModelName> repeatedEnabled = service.availableModels();

        // Assert
        assertThat(List.of(firstDisabled, repeatedDisabled, firstEnabled, repeatedEnabled))
                .containsExactly(Set.of(), Set.of(), Set.of(MODEL), Set.of(MODEL));
    }

    @ParameterizedTest
    @EnumSource(ChannelModelStatus.class)
    void test_matches_routing_enablement_when_model_status_changes(ChannelModelStatus status) {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.replaceModels(List.of(support(status, NOW.plusSeconds(60))), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));
        ProtocolConversionDefinition definition = mock(ProtocolConversionDefinition.class);
        when(definition.matches(PROTOCOL, PROTOCOL)).thenReturn(true);
        when(definition.isEnabledForRouting()).thenReturn(true);
        when(definition.capability()).thenReturn(ConversionCapability.of(
                false, false, false, true, true, Set.of(ContentMappingType.TEXT)));
        RoutingRequest request = RoutingRequest.of(PROTOCOL,
                com.api2api.domain.channel.model.ModelName.of(MODEL.value()), ConversionRequirement.of(false, false, false));

        // Act
        boolean listed = service.availableModels().contains(MODEL);
        boolean routable = new DefaultRoutingPolicyService()
                .buildRoutePlan(request, List.of(channel), List.of(definition), NOW).hasCandidate();

        // Assert
        assertThat(listed).isEqualTo(routable);
    }

    @Test
    void test_returns_requested_alias_once_when_multiple_providers_support_it() {
        // Arrange
        when(repository.findAll()).thenReturn(List.of(channel(1), channel(2)));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).containsExactly(MODEL);
    }

    @Test
    void test_keeps_model_when_another_enabled_provider_supports_it() {
        // Arrange
        ProviderChannel disabled = channel(1);
        disabled.disable(NOW);
        when(repository.findAll()).thenReturn(List.of(disabled, channel(2)));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).containsExactly(MODEL);
    }

    @Test
    void test_removes_model_when_last_supporting_provider_is_disabled() {
        // Arrange
        ProviderChannel channel = channel(1);
        when(repository.findAll()).thenReturn(List.of(channel));
        service.availableModels();

        // Act
        channel.disable(NOW);
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @Test
    void test_restores_model_when_provider_is_reenabled() {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.disable(NOW);
        when(repository.findAll()).thenReturn(List.of(channel));
        service.availableModels();

        // Act
        channel.enable(NOW);
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).containsExactly(MODEL);
    }

    @Test
    void test_preserves_explicit_model_disable_when_provider_is_reenabled() {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.disableModel(ChannelModelSupportId.of(1L), NOW);
        channel.disable(NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        channel.enable(NOW);
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @Test
    void test_returns_empty_when_no_providers_exist() {
        // Arrange
        when(repository.findAll()).thenReturn(List.of());

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @Test
    void test_returns_empty_when_enabled_provider_has_no_models() {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.replaceModels(List.of(), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @Test
    void test_removes_model_when_provider_is_removed_between_queries() {
        // Arrange
        when(repository.findAll()).thenReturn(List.of(channel(1))).thenReturn(List.of());
        service.availableModels();

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @Test
    void test_removes_model_when_last_support_is_removed_between_queries() {
        // Arrange
        ProviderChannel channel = channel(1);
        when(repository.findAll()).thenReturn(List.of(channel));
        service.availableModels();

        // Act
        channel.removeModel(com.api2api.domain.channel.model.ModelName.of(MODEL.value()), PROTOCOL, NOW);
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @Test
    void test_excludes_model_when_its_upstream_protocol_is_no_longer_mapped() {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.replaceProtocolMappings(Set.of(ChannelProtocolMapping.of(
                ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolType.OPENAI_CHAT_COMPLETIONS)), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ChannelModelStatus.class, names = {"DISABLED", "RATE_LIMITED"})
    void test_excludes_model_when_support_is_not_enabled(ChannelModelStatus status) {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.replaceModels(List.of(support(status, NOW.plusSeconds(60))), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void test_restores_model_when_rate_limit_isolation_has_expired(long seconds) {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.replaceModels(List.of(support(ChannelModelStatus.RATE_LIMITED, NOW.plusSeconds(seconds))), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).containsExactly(MODEL);
    }

    @Test
    void test_excludes_model_when_rate_limit_has_no_reset_time() {
        // Arrange
        ProviderChannel channel = channel(1);
        channel.replaceModels(List.of(support(ChannelModelStatus.RATE_LIMITED, null)), NOW);
        when(repository.findAll()).thenReturn(List.of(channel));

        // Act
        Set<ModelName> models = service.availableModels();

        // Assert
        assertThat(models).isEmpty();
    }

    private static ProviderChannel channel(long id) {
        ProviderChannel channel = ProviderChannel.create(ProviderChannelId.of(id),
                ProviderChannelName.of("test-provider"), ProviderHost.of("https://provider.example"),
                ProviderKeyRef.of("test-only-placeholder"), 0, Set.of(PROTOCOL), NOW);
        channel.addOrUpdateModel(support(ChannelModelStatus.ENABLED, null), NOW);
        return channel;
    }

    private static ChannelModelSupport support(ChannelModelStatus status, Instant resetAt) {
        return ChannelModelSupport.rehydrate(ChannelModelSupportId.of(1L),
                com.api2api.domain.channel.model.ModelName.of(MODEL.value()),
                com.api2api.domain.channel.model.ModelName.of("upstream-only-name"), PROTOCOL,
                RoutePriority.of(1), false, status, null, resetAt, ModelSupportSource.MANUAL, NOW, NOW);
    }
}
