package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.application.gateway.StreamingPassthroughPort;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Builds raw SSE responses for protocol-compatible streaming gateway calls.
 */
@Component
@RequiredArgsConstructor
public class GatewayStreamingResponseMapper {

    @NonNull
    private final GatewayInvocationApplicationService gatewayInvocationApplicationService;

    @NonNull
    private final GatewayStreamingConversionPort streamingConversionPort;

    @NonNull
    private final StreamingPassthroughPort streamingPassthroughPort;

    public StreamingResponseBody toResponseBody(
            GatewayStreamingInvocation streamingInvocation,
            HttpServletResponse response
    ) {
        applyHeaders(streamingInvocation, response);
        StreamingResponseBody responseBody = outputStream -> {
            UnifiedTokenUsage usage = UnifiedTokenUsage.unknown();
            try (ProviderStreamingResponse providerResponse = streamingInvocation.providerResponse()) {
                if (streamingInvocation.requiresProtocolConversion()) {
                    GatewayStreamingConversionContext conversionContext = GatewayStreamingConversionContext.of(
                            providerResponse.protocol(),
                            streamingInvocation.invocation().requestProtocol(),
                            streamingInvocation.candidate().requestedModel(),
                            streamingInvocation.candidate().providerChannelId(),
                            streamingInvocation.candidate().upstreamModel()
                    );
                    usage = streamingConversionPort.transform(
                            conversionContext,
                            providerResponse.body(),
                            outputStream
                    );
                } else {
                    usage = streamingPassthroughPort.transferAndExtract(
                            providerResponse.body(),
                            outputStream,
                            providerResponse.protocol()
                    );
                }
                outputStream.flush();
            } catch (IOException exception) {
                if (ClientDisconnectDetector.isClientDisconnect(exception)) {
                    gatewayInvocationApplicationService.completeStreamingClientDisconnect(
                            streamingInvocation,
                            usage
                    );
                    return;
                }
                boolean errorEventWritten = writeStreamingError(
                        outputStream,
                        streamingInvocation.invocation().requestProtocol(),
                        exception
                );
                gatewayInvocationApplicationService.completeStreamingFailure(
                        streamingInvocation,
                        new UncheckedIOException(exception)
                );
                if (!errorEventWritten) {
                    throw exception;
                }
                return;
            } catch (RuntimeException exception) {
                if (ClientDisconnectDetector.isClientDisconnect(exception)) {
                    gatewayInvocationApplicationService.completeStreamingClientDisconnect(
                            streamingInvocation,
                            usage
                    );
                    return;
                }
                gatewayInvocationApplicationService.completeStreamingFailure(streamingInvocation, exception);
                throw exception;
            }
            gatewayInvocationApplicationService.completeStreamingSuccess(streamingInvocation, usage);
        };

        return responseBody;
    }

    /**
     * Signals an upstream transport failure after part of the stream was already relayed, while the
     * client connection is still writable. Client disconnects never reach this path.
     */
    private boolean writeStreamingError(
            OutputStream outputStream,
            ProtocolType clientProtocol,
            IOException streamingFailure
    ) {
        String event = switch (clientProtocol) {
            case CLAUDE_MESSAGES -> """
                    event: error
                    data: {"type":"error","error":{"type":"api_error","message":"Upstream stream failed before completion"}}

                    """;
            case OPENAI_RESPONSES -> """
                    event: error
                    data: {"type":"error","code":"upstream_stream_error","message":"Upstream stream failed before completion","param":null}

                    """;
            case OPENAI_CHAT_COMPLETIONS -> """
                    data: {"error":{"type":"upstream_stream_error","message":"Upstream stream failed before completion"}}

                    data: [DONE]

                    """;
            default -> "";
        };
        if (event.isEmpty()) {
            return false;
        }
        try {
            outputStream.write(event.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            return true;
        } catch (IOException errorWriteFailure) {
            streamingFailure.addSuppressed(errorWriteFailure);
            return false;
        }
    }

    private void applyHeaders(GatewayStreamingInvocation streamingInvocation, HttpServletResponse response) {
        ProviderStreamingResponse providerResponse = streamingInvocation.providerResponse();
        response.setStatus(providerResponse.statusCode());
        if (!streamingInvocation.requiresProtocolConversion() && providerResponse.headers() != null) {
            providerResponse.headers().forEach((name, values) -> {
                if (GatewayResponseHeaderFilter.shouldForward(name) && values != null) {
                    values.stream()
                            .filter(value -> value != null && !value.isBlank())
                            .forEach(value -> response.addHeader(name, value));
                }
            });
        }
        MediaType contentType = streamingInvocation.requiresProtocolConversion()
                ? MediaType.TEXT_EVENT_STREAM
                : GatewayResponseHeaderFilter.extractContentType(providerResponse.headers(), MediaType.TEXT_EVENT_STREAM);
        response.setContentType(contentType.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue());
        response.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
    }

}
