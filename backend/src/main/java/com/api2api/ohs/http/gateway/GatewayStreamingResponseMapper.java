package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingConversionContext;
import com.api2api.application.gateway.GatewayStreamingConversionPort;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.api2api.infr.protocol.StreamingPassthroughUsageExtractor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private static final Set<String> FILTERED_RESPONSE_HEADERS = Set.of(
            "connection",
            "content-length",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "set-cookie",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    @NonNull
    private final GatewayInvocationApplicationService gatewayInvocationApplicationService;

    @NonNull
    private final GatewayStreamingConversionPort streamingConversionPort;

    @NonNull
    private final StreamingPassthroughUsageExtractor streamingPassthroughUsageExtractor;

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
                    usage = streamingPassthroughUsageExtractor.transferAndExtract(
                            providerResponse.body(),
                            outputStream,
                            providerResponse.protocol()
                    );
                }
                outputStream.flush();
            } catch (IOException exception) {
                if (streamingInvocation.requiresProtocolConversion()) {
                    writeProtocolConversionError(
                            outputStream,
                            streamingInvocation.invocation().requestProtocol(),
                            exception
                    );
                }
                gatewayInvocationApplicationService.completeStreamingFailure(
                        streamingInvocation,
                        new UncheckedIOException(exception)
                );
                throw exception;
            } catch (RuntimeException exception) {
                gatewayInvocationApplicationService.completeStreamingFailure(streamingInvocation, exception);
                throw exception;
            }
            gatewayInvocationApplicationService.completeStreamingSuccess(streamingInvocation, usage);
        };

        return responseBody;
    }

    private void writeProtocolConversionError(
            OutputStream outputStream,
            ProtocolType clientProtocol,
            IOException conversionFailure
    ) {
        String event = switch (clientProtocol) {
            case CLAUDE_MESSAGES -> """
                    event: error
                    data: {"type":"error","error":{"type":"api_error","message":"Upstream stream ended before a terminal event"}}

                    """;
            case OPENAI_RESPONSES -> """
                    event: error
                    data: {"type":"error","message":"Upstream stream ended before a terminal event"}

                    """;
            default -> "";
        };
        if (event.isEmpty()) {
            return;
        }
        try {
            outputStream.write(event.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException errorWriteFailure) {
            conversionFailure.addSuppressed(errorWriteFailure);
        }
    }

    private void applyHeaders(GatewayStreamingInvocation streamingInvocation, HttpServletResponse response) {
        ProviderStreamingResponse providerResponse = streamingInvocation.providerResponse();
        response.setStatus(providerResponse.statusCode());
        if (!streamingInvocation.requiresProtocolConversion() && providerResponse.headers() != null) {
            providerResponse.headers().forEach((name, values) -> {
                if (shouldForwardHeader(name) && values != null) {
                    values.stream()
                            .filter(value -> value != null && !value.isBlank())
                            .forEach(value -> response.addHeader(name, value));
                }
            });
        }
        MediaType contentType = streamingInvocation.requiresProtocolConversion()
                ? MediaType.TEXT_EVENT_STREAM
                : contentTypeOf(providerResponse.headers());
        response.setContentType(contentType.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue());
        response.setHeader(HttpHeaders.CONNECTION, "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private MediaType contentTypeOf(Map<String, List<String>> upstreamHeaders) {
        if (upstreamHeaders != null) {
            for (Map.Entry<String, List<String>> entry : upstreamHeaders.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)
                        && entry.getValue() != null
                        && !entry.getValue().isEmpty()
                        && entry.getValue().get(0) != null
                        && !entry.getValue().get(0).isBlank()) {
                    return MediaType.parseMediaType(entry.getValue().get(0));
                }
            }
        }
        return MediaType.TEXT_EVENT_STREAM;
    }

    private boolean shouldForwardHeader(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return !FILTERED_RESPONSE_HEADERS.contains(normalized)
                && !normalized.equals(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.ROOT));
    }
}
