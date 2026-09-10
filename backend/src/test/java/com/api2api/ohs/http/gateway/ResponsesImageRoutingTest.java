package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.api2api.application.credential.ApiCredentialApplicationService;
import com.api2api.application.credential.ModelDailyLimitWindow;
import com.api2api.application.gateway.*;
import com.api2api.domain.channel.model.*;
import com.api2api.domain.channel.repository.ProviderChannelRepository;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.credential.model.ApiCredentialName;
import com.api2api.domain.credential.model.ApiKeyPreview;
import com.api2api.domain.credential.model.EncryptedApiKeyMaterial;
import com.api2api.domain.credential.model.ModelGroupId;
import com.api2api.domain.credential.model.ModelWhitelist;
import com.api2api.domain.credential.model.TokenLimit;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.credential.repository.ModelGroupRepository;
import com.api2api.domain.gateway.service.DefaultGatewayInvocationService;
import com.api2api.domain.protocol.model.*;
import com.api2api.domain.protocol.repository.ProtocolConversionDefinitionRepository;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.routing.service.DefaultRoutingPolicyService;
import com.api2api.domain.usage.model.UsageRecord;
import com.api2api.domain.usage.repository.UsageRecordRepository;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.infr.protocol.DefaultProtocolConversionServiceAdapter;
import com.api2api.infr.protocol.JsonMultipartFormPayloadCodec;
import com.api2api.infr.protocol.OpenAIImagesUsageExtractor;
import com.api2api.infr.protocol.StreamingPassthroughUsageExtractor;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Real routing, conversion, authorization, reservation and settlement; only persistence/transport are stubbed. */
class ResponsesImageRoutingTest {
    private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");
    private final ObjectMapper json = new ObjectMapper();
    private final ProviderGatewayCallPort upstream = mock(ProviderGatewayCallPort.class);
    private final UsageRecordRepository usage = mock(UsageRecordRepository.class);
    private final ProviderChannelRepository channels = mock(ProviderChannelRepository.class);
    private final ApiCredentialRepository credentials = mock(ApiCredentialRepository.class);
    private ResponsesImageBridge bridge;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        GatewayApiKeyHashHelper hash = new GatewayApiKeyHashHelper();
        GatewayIdentifierHelper ids = new GatewayIdentifierHelper();
        when(credentials.findByKeyHash(any())).thenReturn(Optional.of(credential(true, TokenLimit.unlimited())));
        when(usage.sumActualTokensByApiCredential(any())).thenReturn(BigDecimal.ZERO);
        when(channels.findEnabledForRouting()).thenReturn(List.of(channel(1, 20), channel(2, 10)));
        ProtocolConversionDefinitionRepository definitions = mock(ProtocolConversionDefinitionRepository.class);
        when(definitions.findAll()).thenReturn(List.of(definition()));
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        GatewayInvocationApplicationService gateway = new GatewayInvocationApplicationService(credentials,
                mock(ModelGroupRepository.class), new ModelDailyLimitWindow(clock, "UTC"), usage, channels, definitions,
                new DefaultRoutingPolicyService(),
                new DefaultProtocolConversionServiceAdapter(List.of(), List.of(new OpenAIImagesUsageExtractor()), json),
                new DefaultGatewayInvocationService(), upstream, mock(GatewayPayloadModelMappingPort.class),
                mock(GatewayStreamingConversionPort.class), transactions, clock);
        var codec = new JsonMultipartFormPayloadCodec(json);
        bridge = new ResponsesImageBridge(json, new ResponsesImagePayloadMapper(json, codec), gateway,
                new GatewayRequestMapper(hash, ids), ids,
                new GatewayInvocationResponseMapper(json, new GatewayProtocolErrorBodyBuilder(json)),
                new ProtocolContractRegistry(json), mock(ApiCredentialApplicationService.class),
                new StreamingPassthroughUsageExtractor(json), "gpt-image-2.5");
        when(upstream.forward(any(), anyString(), anyBoolean(), any())).thenReturn(ProviderGatewayResponse.of(
                ProtocolType.OPENAI_IMAGES, 200, Map.of(),
                "{\"data\":[{\"b64_json\":\"aW1hZ2U=\"}],\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}", false));
    }

    @Test
    void test_uses_native_images_route_when_only_images_conversion_is_registered() throws Exception {
        // Arrange / Act
        invoke(false);
        // Assert
        ArgumentCaptor<RouteCandidate> route = ArgumentCaptor.forClass(RouteCandidate.class);
        verify(upstream).forward(route.capture(), anyString(), eq(false), any());
        assertThat(route.getValue().clientProtocol()).isEqualTo(ProtocolType.OPENAI_IMAGES);
        assertThat(route.getValue().upstreamProtocol()).isEqualTo(ProtocolType.OPENAI_IMAGES);
    }

    @Test
    void test_fails_over_to_second_image_channel_when_first_returns_503() throws Exception {
        // Arrange
        when(upstream.forward(any(), anyString(), anyBoolean(), any()))
                .thenReturn(ProviderGatewayResponse.of(ProtocolType.OPENAI_IMAGES, 503, Map.of(), "{\"error\":{\"message\":\"Busy\"}}", false))
                .thenReturn(ProviderGatewayResponse.of(ProtocolType.OPENAI_IMAGES, 200, Map.of(), "{\"data\":[{\"b64_json\":\"aW1hZ2U=\"}]}", false));
        // Act
        invoke(false);
        // Assert
        ArgumentCaptor<RouteCandidate> route = ArgumentCaptor.forClass(RouteCandidate.class);
        verify(upstream, times(2)).forward(route.capture(), anyString(), eq(false), any());
        assertThat(route.getAllValues()).extracting(candidate -> candidate.providerChannelId().value()).containsExactly(1L, 2L);
    }

    @Test
    void test_settles_original_image_usage_when_generation_completes() throws Exception {
        // Arrange / Act
        invoke(false);
        // Assert
        ArgumentCaptor<UsageRecord> record = ArgumentCaptor.forClass(UsageRecord.class);
        verify(usage).update(record.capture());
        assertThat(record.getValue().tokenUsage().totalTokens()).isEqualTo(30);
    }

    @Test
    void test_denies_image_model_before_transport_when_key_only_allows_main_model() {
        // Arrange
        when(credentials.findByKeyHash(any())).thenReturn(Optional.of(credential(false, TokenLimit.unlimited())));
        // Act / Assert
        assertThatThrownBy(() -> invoke(false)).isInstanceOf(IllegalStateException.class).hasMessageContaining("MODEL_NOT_ALLOWED");
        verifyNoInteractions(upstream);
    }

    @Test
    void test_denies_exhausted_quota_before_transport_when_bridged_image_starts() {
        // Arrange
        when(credentials.findByKeyHash(any())).thenReturn(Optional.of(credential(true, TokenLimit.of(100))));
        when(usage.sumActualTokensByApiCredential(any())).thenReturn(BigDecimal.valueOf(100));
        // Act / Assert
        assertThatThrownBy(() -> invoke(false)).isInstanceOf(IllegalStateException.class).hasMessageContaining("TOKEN_QUOTA_EXHAUSTED");
        verifyNoInteractions(upstream);
    }

    @Test
    void test_cancels_reservation_when_image_stream_is_truncated() throws Exception {
        // Arrange
        when(upstream.openStream(any(), anyString(), any())).thenReturn(ProviderStreamingResponse.of(
                ProtocolType.OPENAI_IMAGES, 200, Map.of(), new ByteArrayInputStream("data: {}\n\n".getBytes(StandardCharsets.UTF_8))));
        // Act
        ((StreamingResponseBody) invoke(true)).writeTo(new ByteArrayOutputStream());
        // Assert
        verify(usage).cancelReservation(any());
        verify(usage, never()).update(any());
    }

    @Test
    void test_records_stream_usage_when_images_send_completion_data_without_event_header() throws Exception {
        // Arrange
        when(upstream.openStream(any(), anyString(), any())).thenReturn(ProviderStreamingResponse.of(
                ProtocolType.OPENAI_IMAGES, 200, Map.of(), new ByteArrayInputStream(("data: {\"type\":\"image_generation.completed\","
                + "\"b64_json\":\"aW1hZ2U=\",\"usage\":{\"input_tokens\":10,\"output_tokens\":20}}\n\n").getBytes(StandardCharsets.UTF_8))));
        // Act
        ((StreamingResponseBody) invoke(true)).writeTo(new ByteArrayOutputStream());
        // Assert
        ArgumentCaptor<UsageRecord> record = ArgumentCaptor.forClass(UsageRecord.class);
        verify(usage).update(record.capture());
        assertThat(record.getValue().tokenUsage().totalTokens()).isEqualTo(30);
    }

    private Object invoke(boolean stream) throws Exception {
        String request = "{\"model\":\"gpt-5.6-sol\",\"input\":\"Draw a blue cat\",\"stream\":" + stream
                + ",\"tools\":[{\"type\":\"image_generation\"}],\"tool_choice\":{\"type\":\"image_generation\"}}";
        Object result = bridge.tryHandle(request, "Bearer test-key", null, null,
                InboundRequestContext.empty(), new MockHttpServletResponse()).orElseThrow();
        if (result instanceof ResponseEntity<?> entity) assertThat(entity.getStatusCode().value()).isEqualTo(200);
        return result;
    }

    private ApiCredential credential(boolean allowImages, TokenLimit limit) {
        Set<com.api2api.domain.credential.model.ModelName> models = allowImages
                ? Set.of(com.api2api.domain.credential.model.ModelName.of("gpt-5.6-sol"), com.api2api.domain.credential.model.ModelName.of("gpt-image-2.5"))
                : Set.of(com.api2api.domain.credential.model.ModelName.of("gpt-5.6-sol"));
        return ApiCredential.create(ApiCredentialId.of(1L), UserAccountId.of(1L), ApiCredentialName.of("test"),
                new GatewayApiKeyHashHelper().hashBearerToken("Bearer test-key"), ApiKeyPreview.of("test-key"),
                EncryptedApiKeyMaterial.unavailable(), ModelGroupId.of(1L), ModelWhitelist.of(models), limit, NOW);
    }

    private ProviderChannel channel(long id, int priority) {
        return ProviderChannel.rehydrate(ProviderChannelId.of(id), ProviderChannelName.of("Images " + id),
                ProviderHost.of("https://images.example.com"), ProviderKeyRef.of("TEST_PROVIDER_KEY"), ProviderModelsPath.DEFAULT,
                priority, Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_IMAGES, ProtocolType.OPENAI_IMAGES)),
                List.of(ChannelModelSupport.create(ChannelModelSupportId.of(id), ModelName.of("gpt-image-2.5"), ModelName.of("gpt-image-2.5"),
                        ProtocolType.OPENAI_IMAGES, RoutePriority.of(1), false, ModelSupportSource.MANUAL, NOW)),
                ProviderChannelStatus.ENABLED, NOW, NOW);
    }

    private ProtocolConversionDefinition definition() {
        return ProtocolConversionDefinition.create(ProtocolConversionDefinitionId.of(1L), ProtocolType.OPENAI_IMAGES, ProtocolType.OPENAI_IMAGES,
                ConversionCapability.of(true, false, false, true, false, Set.of(ContentMappingType.TEXT)),
                mapping(MappingDirection.REQUEST), mapping(MappingDirection.RESPONSE), ConversionImplementationStatus.IMPLEMENTED, NOW);
    }

    private MappingDocument mapping(MappingDirection direction) {
        return MappingDocument.of(direction, "Passthrough", "Passthrough", List.of(FieldMapping.of("payload", "payload", "passthrough", MappingLossiness.NONE)));
    }
}
