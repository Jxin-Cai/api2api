package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    public StreamingResponseBody toResponseBody(
            GatewayStreamingInvocation streamingInvocation,
            HttpServletResponse response
    ) {
        applyHeaders(streamingInvocation.providerResponse(), response);
        StreamingResponseBody responseBody = outputStream -> {
            try (ProviderStreamingResponse providerResponse = streamingInvocation.providerResponse()) {
                providerResponse.body().transferTo(outputStream);
                outputStream.flush();
            } catch (IOException exception) {
                gatewayInvocationApplicationService.completeStreamingFailure(
                        streamingInvocation,
                        new UncheckedIOException(exception)
                );
                throw exception;
            } catch (RuntimeException exception) {
                gatewayInvocationApplicationService.completeStreamingFailure(streamingInvocation, exception);
                throw exception;
            }
            gatewayInvocationApplicationService.completeStreamingSuccess(streamingInvocation);
        };

        return responseBody;
    }

    private void applyHeaders(ProviderStreamingResponse providerResponse, HttpServletResponse response) {
        response.setStatus(providerResponse.statusCode());
        if (providerResponse.headers() != null) {
            providerResponse.headers().forEach((name, values) -> {
                if (shouldForwardHeader(name) && values != null) {
                    values.stream()
                            .filter(value -> value != null && !value.isBlank())
                            .forEach(value -> response.addHeader(name, value));
                }
            });
        }
        response.setContentType(contentTypeOf(providerResponse.headers()).toString());
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
