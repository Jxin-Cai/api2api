package com.api2api.ohs.http.gateway;

import com.api2api.domain.gateway.model.GatewayInvocationId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.usage.model.UsageRecordId;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Generates identifiers required before a gateway request enters the application layer.
 */
@Component
public class GatewayIdentifierHelper {

    private static final int MAX_GATEWAY_REQUEST_ID_LENGTH = 64;
    private static final AtomicLong SEQUENCE = new AtomicLong(new SecureRandom().nextInt(1_000_000) + 1L);

    public GatewayInvocationId nextInvocationId() {
        return GatewayInvocationId.of(nextPositiveLong());
    }

    public GatewayRequestId requestId(String xRequestId) {
        String candidate = xRequestId == null || xRequestId.isBlank()
                ? "gw-" + UUID.randomUUID()
                : xRequestId.trim();
        if (candidate.length() > MAX_GATEWAY_REQUEST_ID_LENGTH) {
            candidate = candidate.substring(0, MAX_GATEWAY_REQUEST_ID_LENGTH);
        }
        return GatewayRequestId.of(candidate);
    }

    public UsageRecordId nextUsageRecordId() {
        return UsageRecordId.of(nextPositiveLong());
    }

    private long nextPositiveLong() {
        long epochMilli = Instant.now().toEpochMilli();
        long sequence = SEQUENCE.updateAndGet(current -> current == Long.MAX_VALUE ? 1L : current + 1L);
        long value = Objects.hash(epochMilli, sequence) & Long.MAX_VALUE;
        return value == 0L ? sequence : value;
    }
}
