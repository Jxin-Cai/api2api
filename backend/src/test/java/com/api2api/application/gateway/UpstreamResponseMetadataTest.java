package com.api2api.application.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpstreamResponseMetadataTest {

    private static final Instant NOW = Instant.parse("2026-08-19T00:00:00Z");

    @Test
    void test_returnsDelay_when_retryAfterIsDeltaSeconds() {
        // Arrange
        UpstreamResponseMetadata metadata = UpstreamResponseMetadata.of(Map.of("Retry-After", List.of("30")));

        // Act
        var retryAfter = metadata.retryAfter(NOW);

        // Assert
        assertThat(retryAfter).contains(Duration.ofSeconds(30));
    }

    @Test
    void test_returnsRemainingDelay_when_retryAfterIsHttpDate() {
        // Arrange
        UpstreamResponseMetadata metadata = UpstreamResponseMetadata.of(
                Map.of("retry-after", List.of("Wed, 19 Aug 2026 00:01:00 GMT")));

        // Act
        var retryAfter = metadata.retryAfter(NOW);

        // Assert
        assertThat(retryAfter).contains(Duration.ofMinutes(1));
    }

    @Test
    void test_clampsDelay_when_upstreamAdvertisesExcessiveRetryAfter() {
        // Arrange
        UpstreamResponseMetadata metadata = UpstreamResponseMetadata.of(Map.of("retry-after", List.of("86400")));

        // Act
        var retryAfter = metadata.retryAfter(NOW);

        // Assert
        assertThat(retryAfter).contains(Duration.ofHours(1));
    }

    @Test
    void test_returnsEmpty_when_retryAfterIsAbsent() {
        // Arrange
        UpstreamResponseMetadata metadata = UpstreamResponseMetadata.of(Map.of("x-ratelimit-remaining", List.of("0")));

        // Act
        var retryAfter = metadata.retryAfter(NOW);

        // Assert
        assertThat(retryAfter).isEmpty();
    }

    @Test
    void test_returnsEmpty_when_retryAfterIsUnparseable() {
        // Arrange
        UpstreamResponseMetadata metadata = UpstreamResponseMetadata.of(Map.of("retry-after", List.of("soon")));

        // Act
        var retryAfter = metadata.retryAfter(NOW);

        // Assert
        assertThat(retryAfter).isEmpty();
    }
}
