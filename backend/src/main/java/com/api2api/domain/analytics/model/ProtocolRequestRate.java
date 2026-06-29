package com.api2api.domain.analytics.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Objects;

/**
 * Request count and requests-per-minute rate for a protocol in a time window.
 */
public final class ProtocolRequestRate {

    private final ProtocolType protocol;
    private final AnalyticsTimeWindow window;
    private final long requestCount;
    private final double requestsPerMinute;

    private ProtocolRequestRate(
            ProtocolType protocol,
            AnalyticsTimeWindow window,
            long requestCount,
            double requestsPerMinute
    ) {
        this.protocol = Objects.requireNonNull(protocol, "Protocol request rate protocol must not be null");
        this.window = Objects.requireNonNull(window, "Protocol request rate window must not be null");
        if (requestCount < 0) {
            throw new IllegalArgumentException("Protocol request count must be greater than or equal to 0");
        }
        if (!Double.isFinite(requestsPerMinute) || requestsPerMinute < 0) {
            throw new IllegalArgumentException("Protocol requests per minute must be finite and greater than or equal to 0");
        }
        if (this.window.durationMinutesExact() <= 0) {
            throw new IllegalArgumentException("Protocol request rate window minutes must be greater than 0");
        }
        this.requestCount = requestCount;
        this.requestsPerMinute = requestsPerMinute;
    }

    public static ProtocolRequestRate of(
            ProtocolType protocol,
            AnalyticsTimeWindow window,
            long requestCount,
            double requestsPerMinute
    ) {
        return new ProtocolRequestRate(protocol, window, requestCount, requestsPerMinute);
    }

    public static ProtocolRequestRate calculate(ProtocolType protocol, AnalyticsTimeWindow window, long requestCount) {
        AnalyticsTimeWindow nonNullWindow = Objects.requireNonNull(window, "Protocol request rate window must not be null");
        double minutes = nonNullWindow.durationMinutesExact();
        if (minutes <= 0) {
            throw new IllegalArgumentException("Protocol request rate window minutes must be greater than 0");
        }
        return new ProtocolRequestRate(protocol, nonNullWindow, requestCount, requestCount / minutes);
    }

    public static ProtocolRequestRate zero(ProtocolType protocol, AnalyticsTimeWindow window) {
        return new ProtocolRequestRate(protocol, window, 0, 0.0d);
    }

    public ProtocolType protocol() {
        return protocol;
    }

    public AnalyticsTimeWindow window() {
        return window;
    }

    public long requestCount() {
        return requestCount;
    }

    public double requestsPerMinute() {
        return requestsPerMinute;
    }

    public ProtocolType getProtocol() {
        return protocol;
    }

    public AnalyticsTimeWindow getWindow() {
        return window;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public double getRequestsPerMinute() {
        return requestsPerMinute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolRequestRate that)) {
            return false;
        }
        return requestCount == that.requestCount
                && Double.compare(requestsPerMinute, that.requestsPerMinute) == 0
                && protocol == that.protocol
                && Objects.equals(window, that.window);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, window, requestCount, requestsPerMinute);
    }
}
