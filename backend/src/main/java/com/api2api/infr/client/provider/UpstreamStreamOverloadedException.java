package com.api2api.infr.client.provider;

import com.api2api.application.gateway.UpstreamGatewayException;
import com.api2api.application.gateway.UpstreamResponseMetadata;
import com.api2api.domain.routing.model.RouteFailureType;
import org.springframework.http.HttpStatus;

/** Explicit capacity rejection detected before any response content is handed to the client. */
final class UpstreamStreamOverloadedException extends UpstreamGatewayException {

    UpstreamStreamOverloadedException(long elapsedMillis, UpstreamResponseMetadata metadata) {
        super(RouteFailureType.CHANNEL_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value(), true,
                elapsedMillis, "Upstream rejected stream before output: servers overloaded", metadata);
    }
}
