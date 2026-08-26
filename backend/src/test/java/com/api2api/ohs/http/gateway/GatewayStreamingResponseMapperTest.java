package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.gateway.model.GatewayInvocation;
import com.api2api.domain.routing.model.RouteCandidate;
import com.api2api.domain.usage.model.UsageRecordId;
import com.api2api.infr.protocol.StreamingPassthroughUsageExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class GatewayStreamingResponseMapperTest {

    @Test
    void test_writesClaudeErrorEvent_when_convertedStreamEndsBeforeTerminalEvent() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                200,
                Map.of(),
                new ByteArrayInputStream(new byte[0])
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(true);
        when(candidate.requestedModel()).thenReturn(ModelName.of("claude-opus-4.6"));
        when(candidate.providerChannelId()).thenReturn(ProviderChannelId.of(1L));
        when(candidate.upstreamModel()).thenReturn(ModelName.of("anthropic.claude-opus-4-6-v1:0"));
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
            throw new EOFException("Bedrock InvokeModel stream ended before message_stop");
        });
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        mapper.toResponseBody(
                streamingInvocation,
                new MockHttpServletResponse()
        ).writeTo(downstream);

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .contains("event: content_block_delta")
                .contains("event: error")
                .contains("Upstream stream failed before completion")
                .doesNotContain("event: message_stop");
        verify(applicationService).completeStreamingFailure(any(), any());
    }

    @Test
    void test_completesSuccessfully_when_passthroughStreamEndsWithoutTerminalEvent() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        String incompleteEvent = "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n";
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.CLAUDE_MESSAGES,
                200,
                Map.of(),
                new ByteArrayInputStream(incompleteEvent.getBytes(StandardCharsets.UTF_8))
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(false);
        GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                invocation,
                UsageRecordId.of(1L),
                candidate,
                providerResponse
        );
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        mapper.toResponseBody(streamingInvocation, new MockHttpServletResponse()).writeTo(downstream);

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8)).isEqualTo(incompleteEvent);
    }

    @Test
    void test_reportsSuccess_when_passthroughStreamEndsWithoutTerminalEvent() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        String incompleteEvent = "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n";
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.CLAUDE_MESSAGES,
                200,
                Map.of(),
                new ByteArrayInputStream(incompleteEvent.getBytes(StandardCharsets.UTF_8))
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(false);
        GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                invocation,
                UsageRecordId.of(1L),
                candidate,
                providerResponse
        );
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );

        // Act
        mapper.toResponseBody(streamingInvocation, new MockHttpServletResponse())
                .writeTo(new ByteArrayOutputStream());

        // Assert
        verify(applicationService).completeStreamingSuccess(any(), any());
    }

    @Test
    void test_completesClientDisconnect_when_passthroughFlushHitsBrokenPipe() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        String incompleteEvent = "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n";
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.CLAUDE_MESSAGES,
                200,
                Map.of(),
                new ByteArrayInputStream(incompleteEvent.getBytes(StandardCharsets.UTF_8))
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(false);
        GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                invocation,
                UsageRecordId.of(1L),
                candidate,
                providerResponse
        );
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );
        DisconnectingOutputStream downstream = new DisconnectingOutputStream();

        // Act
        mapper.toResponseBody(streamingInvocation, new MockHttpServletResponse()).writeTo(downstream);

        // Assert
        verify(applicationService).completeStreamingClientDisconnect(any(), any());
        verify(applicationService, never()).completeStreamingFailure(any(), any());
        verify(applicationService, never()).completeStreamingSuccess(any(), any());
    }

    @Test
    void test_doesNotWriteErrorEvent_when_convertedStreamDisconnects() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                200,
                Map.of(),
                new ByteArrayInputStream(new byte[0])
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(true);
        when(candidate.requestedModel()).thenReturn(ModelName.of("claude-opus-4.6"));
        when(candidate.providerChannelId()).thenReturn(ProviderChannelId.of(1L));
        when(candidate.upstreamModel()).thenReturn(ModelName.of("anthropic.claude-opus-4-6-v1:0"));
        GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                invocation,
                UsageRecordId.of(1L),
                candidate,
                providerResponse
        );
        when(conversionPort.transform(any(), any(), any())).thenAnswer(call -> {
            call.<OutputStream>getArgument(2).write(
                    "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n"
                            .getBytes(StandardCharsets.UTF_8)
            );
            throw new AsyncRequestNotUsableException(
                    "ServletOutputStream failed to flush: java.io.IOException: Broken pipe",
                    new IOException("Broken pipe")
            );
        });
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        mapper.toResponseBody(streamingInvocation, new MockHttpServletResponse()).writeTo(downstream);

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8))
                .contains("event: content_block_delta")
                .doesNotContain("event: error");
        verify(applicationService).completeStreamingClientDisconnect(any(), any());
        verify(applicationService, never()).completeStreamingFailure(any(), any());
    }

    @Test
    void test_doesNotRethrow_when_convertedStreamDisconnects() throws Exception {
        // Arrange
        GatewayInvocationApplicationService applicationService = mock(GatewayInvocationApplicationService.class);
        GatewayStreamingConversionPort conversionPort = mock(GatewayStreamingConversionPort.class);
        GatewayInvocation invocation = mock(GatewayInvocation.class);
        RouteCandidate candidate = mock(RouteCandidate.class);
        ProviderStreamingResponse providerResponse = ProviderStreamingResponse.of(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                200,
                Map.of(),
                new ByteArrayInputStream(new byte[0])
        );
        when(invocation.requestProtocol()).thenReturn(ProtocolType.CLAUDE_MESSAGES);
        when(candidate.requiresProtocolConversion()).thenReturn(true);
        when(candidate.requestedModel()).thenReturn(ModelName.of("claude-opus-4.6"));
        when(candidate.providerChannelId()).thenReturn(ProviderChannelId.of(1L));
        when(candidate.upstreamModel()).thenReturn(ModelName.of("anthropic.claude-opus-4-6-v1:0"));
        GatewayStreamingInvocation streamingInvocation = GatewayStreamingInvocation.opened(
                invocation,
                UsageRecordId.of(1L),
                candidate,
                providerResponse
        );
        when(conversionPort.transform(any(), any(), any())).thenThrow(new AsyncRequestNotUsableException(
                "ServletOutputStream failed to flush: java.io.IOException: Broken pipe",
                new IOException("Broken pipe")
        ));
        GatewayStreamingResponseMapper mapper = new GatewayStreamingResponseMapper(
                applicationService,
                conversionPort,
                new StreamingPassthroughUsageExtractor(new ObjectMapper())
        );

        // Act & Assert
        assertThatCode(() -> mapper.toResponseBody(streamingInvocation, new MockHttpServletResponse())
                .writeTo(new ByteArrayOutputStream()))
                .doesNotThrowAnyException();
    }

    private static final class DisconnectingOutputStream extends OutputStream {

        @Override
        public void write(int value) {
            // Bytes may be buffered before the client aborts on flush.
        }

        @Override
        public void flush() throws IOException {
            throw new AsyncRequestNotUsableException(
                    "ServletOutputStream failed to flush: java.io.IOException: Broken pipe",
                    new IOException("Broken pipe")
            );
        }
    }
}
