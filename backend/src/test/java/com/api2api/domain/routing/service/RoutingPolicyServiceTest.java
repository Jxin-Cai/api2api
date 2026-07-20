package com.api2api.domain.routing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ChannelModelSupportId;
import com.api2api.domain.channel.model.ChannelModelStatus;
import com.api2api.domain.channel.model.ChannelProtocolMapping;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ModelSupportSource;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.channel.model.ProviderChannelStatus;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.channel.model.ProviderModelsPath;
import com.api2api.domain.channel.model.RoutePriority;
import com.api2api.domain.protocol.model.ContentMappingType;
import com.api2api.domain.protocol.model.ConversionCapability;
import com.api2api.domain.protocol.model.ConversionImplementationStatus;
import com.api2api.domain.protocol.model.ConversionRequirement;
import com.api2api.domain.protocol.model.FieldMapping;
import com.api2api.domain.protocol.model.MappingDirection;
import com.api2api.domain.protocol.model.MappingDocument;
import com.api2api.domain.protocol.model.MappingLossiness;
import com.api2api.domain.protocol.model.ProtocolConversionDefinition;
import com.api2api.domain.protocol.model.ProtocolConversionDefinitionId;
import com.api2api.domain.routing.model.RoutePlan;
import com.api2api.domain.routing.model.RouteAttempt;
import com.api2api.domain.routing.model.RouteFailure;
import com.api2api.domain.routing.model.RouteFailureType;
import com.api2api.domain.routing.model.FailoverAction;
import com.api2api.domain.routing.model.RoutingRequest;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoutingPolicyServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
    private final RoutingPolicyService service = new DefaultRoutingPolicyService();

    @Test
    void usesConfiguredUpstreamProtocolForRequestProtocol() {
        ProviderChannel channel = ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("OpenAI channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("sk-test"),
                ProviderModelsPath.DEFAULT,
                10,
                Set.of(ChannelProtocolMapping.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES)),
                List.of(
                        model(1L, "claude-sonnet", "gpt-4.1", ProtocolType.OPENAI_RESPONSES, 1, true),
                        model(2L, "claude-sonnet", "gpt-4.1-chat", ProtocolType.OPENAI_CHAT_COMPLETIONS, 1, true)
                ),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );

        RoutePlan plan = service.buildRoutePlan(
                RoutingRequest.of(
                        ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("claude-sonnet"),
                        ConversionRequirement.of(false, false, false)
                ),
                List.of(channel),
                List.of(
                        definition(1L, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES),
                        definition(2L, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_CHAT_COMPLETIONS)
                ),
                NOW
        );

        assertThat(plan.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.clientProtocol()).isEqualTo(ProtocolType.CLAUDE_MESSAGES);
            assertThat(candidate.upstreamProtocol()).isEqualTo(ProtocolType.OPENAI_RESPONSES);
            assertThat(candidate.upstreamModel().value()).isEqualTo("gpt-4.1");
        });
    }

    @Test
    void keepsModelProtocolAsFallbackWhenConfiguredProtocolCannotServeModel() {
        ProviderChannel channel = ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("multi-protocol channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("sk-test"),
                ProviderModelsPath.DEFAULT,
                10,
                Set.of(
                        ChannelProtocolMapping.of(ProtocolType.CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                        ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)
                ),
                List.of(
                        model(1L, "gpt-5.5", "gpt-5.5", ProtocolType.CLAUDE_MESSAGES, 1, true),
                        model(2L, "gpt-5.5", "gpt-5.5", ProtocolType.OPENAI_RESPONSES, 1, true)
                ),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );

        RoutePlan plan = service.buildRoutePlan(
                RoutingRequest.of(
                        ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("gpt-5.5"),
                        ConversionRequirement.of(false, false, false)
                ),
                List.of(channel),
                List.of(
                        definition(1L, ProtocolType.CLAUDE_MESSAGES, ProtocolType.CLAUDE_MESSAGES),
                        definition(2L, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES)
                ),
                NOW
        );

        assertThat(plan.candidates()).extracting(candidate -> candidate.upstreamProtocol())
                .containsExactly(ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES);

        RouteFailure failure = RouteFailure.of(
                ProviderChannelId.of(1L),
                RouteFailureType.UPSTREAM_ERROR,
                "model_not_found",
                true,
                NOW
        );
        RouteAttempt failedAttempt = RouteAttempt.start(plan.firstCandidate(), 1, NOW).markFailed(failure, NOW);

        var decision = service.decideNext(plan, List.of(failedAttempt), failure);

        assertThat(decision.action()).isEqualTo(FailoverAction.RETRY_NEXT);
        assertThat(decision.nextCandidate().upstreamProtocol()).isEqualTo(ProtocolType.OPENAI_RESPONSES);
        assertThat(decision.nextCandidate().providerChannelId()).isEqualTo(ProviderChannelId.of(1L));
    }

    @Test
    void derivesUpstreamProtocolFromModelSupportWhenRequestMappingMissing() {
        ProviderChannel channel = ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("OpenAI responses channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("sk-test"),
                ProviderModelsPath.DEFAULT,
                10,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                List.of(model(1L, "gpt5.5", "gpt-5.5", ProtocolType.OPENAI_RESPONSES, 1, true)),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );

        RoutePlan plan = service.buildRoutePlan(
                RoutingRequest.of(
                        ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("gpt5.5"),
                        ConversionRequirement.of(false, false, false)
                ),
                List.of(channel),
                List.of(definition(1L, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES)),
                NOW
        );

        assertThat(plan.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.clientProtocol()).isEqualTo(ProtocolType.CLAUDE_MESSAGES);
            assertThat(candidate.upstreamProtocol()).isEqualTo(ProtocolType.OPENAI_RESPONSES);
            assertThat(candidate.upstreamModel().value()).isEqualTo("gpt-5.5");
        });
    }

    @Test
    void doesNotDeriveCandidateWhenConversionCapabilityDoesNotSatisfyRequirement() {
        ProviderChannel channel = ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("OpenAI responses channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("sk-test"),
                ProviderModelsPath.DEFAULT,
                10,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                List.of(model(1L, "gpt5.5", "gpt-5.5", ProtocolType.OPENAI_RESPONSES, 1, true)),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );

        RoutePlan plan = service.buildRoutePlan(
                RoutingRequest.of(
                        ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("gpt5.5"),
                        ConversionRequirement.of(true, false, false)
                ),
                List.of(channel),
                List.of(definition(1L, ProtocolType.CLAUDE_MESSAGES, ProtocolType.OPENAI_RESPONSES)),
                NOW
        );

        assertThat(plan.candidates()).isEmpty();
    }

    @Test
    void doesNotDeriveCandidateWithoutConversionDefinition() {
        ProviderChannel channel = ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("OpenAI responses channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("sk-test"),
                ProviderModelsPath.DEFAULT,
                10,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                List.of(model(1L, "gpt5.5", "gpt-5.5", ProtocolType.OPENAI_RESPONSES, 1, true)),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );

        RoutePlan plan = service.buildRoutePlan(
                RoutingRequest.of(
                        ProtocolType.CLAUDE_MESSAGES,
                        ModelName.of("gpt5.5"),
                        ConversionRequirement.of(false, false, false)
                ),
                List.of(channel),
                List.of(),
                NOW
        );

        assertThat(plan.candidates()).isEmpty();
    }

    @Test
    void test_keepsOtherModelRoutable_when_ChannelModelIsRateLimited() {
        // Arrange
        ChannelModelSupport limitedModel = ChannelModelSupport.rehydrate(
                ChannelModelSupportId.of(1L),
                ModelName.of("gpt-limited"),
                ModelName.of("gpt-limited"),
                ProtocolType.OPENAI_RESPONSES,
                RoutePriority.of(1),
                false,
                ChannelModelStatus.RATE_LIMITED,
                NOW,
                NOW.plusSeconds(3600),
                ModelSupportSource.MANUAL,
                NOW,
                NOW
        );
        ProviderChannel channel = ProviderChannel.rehydrate(
                ProviderChannelId.of(1L),
                ProviderChannelName.of("multi-model channel"),
                ProviderHost.of("https://api.example.com"),
                ProviderKeyRef.of("sk-test"),
                ProviderModelsPath.DEFAULT,
                10,
                Set.of(ChannelProtocolMapping.of(ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                List.of(
                        limitedModel,
                        model(2L, "gpt-available", "gpt-available", ProtocolType.OPENAI_RESPONSES, 1, false)
                ),
                ProviderChannelStatus.ENABLED,
                NOW,
                NOW
        );

        // Act
        RoutePlan plan = service.buildRoutePlan(
                RoutingRequest.of(
                        ProtocolType.OPENAI_RESPONSES,
                        ModelName.of("gpt-available"),
                        ConversionRequirement.of(false, false, false)
                ),
                List.of(channel),
                List.of(definition(1L, ProtocolType.OPENAI_RESPONSES, ProtocolType.OPENAI_RESPONSES)),
                NOW
        );

        // Assert
        assertThat(plan.candidates()).singleElement().satisfies(candidate ->
                assertThat(candidate.requestedModel()).isEqualTo(ModelName.of("gpt-available"))
        );
    }

    private ChannelModelSupport model(
            long id,
            String requestedModel,
            String upstreamModel,
            ProtocolType upstreamProtocol,
            int priority,
            boolean preferred
    ) {
        return ChannelModelSupport.create(
                ChannelModelSupportId.of(id),
                ModelName.of(requestedModel),
                ModelName.of(upstreamModel),
                upstreamProtocol,
                RoutePriority.of(priority),
                preferred,
                ModelSupportSource.MANUAL,
                NOW
        );
    }

    private ProtocolConversionDefinition definition(long id, ProtocolType sourceProtocol, ProtocolType targetProtocol) {
        return ProtocolConversionDefinition.create(
                ProtocolConversionDefinitionId.of(id),
                sourceProtocol,
                targetProtocol,
                ConversionCapability.of(false, false, false, true, true, Set.of(ContentMappingType.TEXT)),
                mapping(MappingDirection.REQUEST),
                mapping(MappingDirection.RESPONSE),
                ConversionImplementationStatus.IMPLEMENTED,
                NOW
        );
    }

    private MappingDocument mapping(MappingDirection direction) {
        return MappingDocument.of(
                direction,
                direction.name() + " mapping",
                "passthrough",
                List.of(FieldMapping.of("payload", "payload", "passthrough", MappingLossiness.NONE))
        );
    }
}
