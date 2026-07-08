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
import org.springframework.http.ResponseEntity;
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

    public ResponseEntity<StreamingResponseBody> toResponseEntity(GatewayStreamingInvocation streamingInvocation) {
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

        ProviderStreamingResponse providerResponse = streamingInvocation.providerResponse();
        HttpHeaders responseHeaders = responseHeaders(providerResponse.headers());
        responseHeaders.setContentType(contentTypeOf(providerResponse.headers()));
        responseHeaders.setCacheControl(CacheControl.noCache());
        responseHeaders.set(HttpHeaders.CONNECTION, "keep-alive");
        responseHeaders.set("X-Accel-Buffering", "no");

        return ResponseEntity.status(providerResponse.statusCode())
                .headers(responseHeaders)
                .body(responseBody);
    }

    private HttpHeaders responseHeaders(Map<String, List<String>> upstreamHeaders) {
        HttpHeaders responseHeaders = new HttpHeaders();
        if (upstreamHeaders == null) {
            return responseHeaders;
        }
        upstreamHeaders.forEach((name, values) -> {
            if (shouldForwardHeader(name) && values != null) {
                responseHeaders.put(name, List.copyOf(values));
            }
        });
        return responseHeaders;
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
