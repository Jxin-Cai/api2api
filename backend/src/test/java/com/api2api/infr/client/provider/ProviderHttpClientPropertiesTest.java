package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProviderHttpClientPropertiesTest {

    @Test
    void test_returnsTenMinuteUpstreamReadTimeout_when_notConfigured() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        Duration timeout = properties.getUpstreamReadTimeout();

        // Assert
        assertThat(timeout).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void test_returnsTwoMinuteStreamingFirstByteTimeout_when_notConfigured() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        Duration timeout = properties.getStreamingFirstByteTimeout();

        // Assert
        assertThat(timeout).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void test_returnsTenMinuteStreamingIdleTimeout_when_notConfigured() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        Duration timeout = properties.getStreamingIdleTimeout();

        // Assert
        assertThat(timeout).isEqualTo(Duration.ofMinutes(10));
    }
}
