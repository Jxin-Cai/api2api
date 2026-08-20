package com.api2api.application.gateway;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Upstream response headers preserved across failover so that rate-limit signals survive
 * all the way back to the client and drive the gateway's own backoff decisions.
 */
public record UpstreamResponseMetadata(Map<String, List<String>> headers) {

    private static final String RETRY_AFTER = "retry-after";
    private static final Duration MAX_RETRY_AFTER = Duration.ofHours(1);

    public UpstreamResponseMetadata {
        headers = copyHeaders(headers);
    }

    public static UpstreamResponseMetadata empty() {
        return new UpstreamResponseMetadata(Map.of());
    }

    public static UpstreamResponseMetadata of(Map<String, List<String>> headers) {
        return new UpstreamResponseMetadata(headers);
    }

    public boolean present() {
        return !headers.isEmpty();
    }

    /**
     * Parses the upstream {@code Retry-After} header, accepting both delta-seconds and HTTP-date forms.
     * Values beyond {@link #MAX_RETRY_AFTER} are clamped so a hostile upstream cannot disable a route.
     */
    public Optional<Duration> retryAfter(Instant now) {
        Objects.requireNonNull(now, "Reference instant must not be null");
        return headers.entrySet().stream()
                .filter(entry -> RETRY_AFTER.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .filter(value -> value != null && !value.isBlank())
                .map(value -> parseRetryAfter(value.trim(), now))
                .flatMap(Optional::stream)
                .findFirst()
                .map(duration -> duration.compareTo(MAX_RETRY_AFTER) > 0 ? MAX_RETRY_AFTER : duration);
    }

    private static Optional<Duration> parseRetryAfter(String value, Instant now) {
        return parseDeltaSeconds(value).or(() -> parseHttpDate(value, now));
    }

    private static Optional<Duration> parseDeltaSeconds(String value) {
        try {
            long seconds = Long.parseLong(value);
            return seconds <= 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(seconds));
        } catch (NumberFormatException notDeltaSeconds) {
            return Optional.empty();
        }
    }

    private static Optional<Duration> parseHttpDate(String value, Instant now) {
        try {
            Instant resetAt = ZonedDateTime
                    .parse(value, DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH))
                    .toInstant();
            Duration remaining = Duration.between(now, resetAt);
            return remaining.isNegative() || remaining.isZero() ? Optional.empty() : Optional.of(remaining);
        } catch (DateTimeParseException notHttpDate) {
            return Optional.empty();
        }
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().trim(),
                        entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left
                ));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof UpstreamResponseMetadata that && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers);
    }
}
