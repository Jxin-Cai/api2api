package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.GatewayInvocationApplicationService;
import com.api2api.application.gateway.GatewayStreamingInvocation;
import com.api2api.application.gateway.ProviderStreamingResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
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

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(responseBody);
    }
}
