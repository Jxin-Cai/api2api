package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.infr.protocol.StreamingPassthroughUsageExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class GatewayStreamingResponseMapperTest {

    @Test
    void test_writesClaudeErrorEvent_when_convertedStreamEndsBeforeTerminalEvent() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.AWS_BEDROCK_CONVERSE,
                200,
                Map.of(),
                new ByteArrayInputStream(new byte[0])
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(true);
        when(candidate.requestedModel()).thenReturn(ModelName.of("claude-opus-4.6"));
        GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                invocation,
                UsageRecordId.of(1L),
                candidate,
                providerResponse
        );
        when(conversionPort.transform(any(), any(), any())).thenAnswer(call -> {
            call.<java.io.OutputStream>getArgument(2).write(
                    "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n"
                            .getBytes(StandardCharsets.UTF_8)
            );
            throw new EOFException("Bedrock Converse stream ended before messageStop");
        });
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act / Assert
        assertThatThrownBy(() -> mapper.toResponseBody(
                streamingInvocation,
                new MockHttpServletResponse()
        ).writeTo(downstream)).isInstanceOf(EOFException.class);
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .contains("event: content_block_delta")
                .contains("event: error")
                .contains("Upstream stream ended before a terminal event")
                .doesNotContain("event: message_stop");
        verify(applicationService).completeStreamingFailure(any(), any());
    }
}
