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
    void test_returnsFiveMinuteStreamingFirstByteTimeout_when_notConfigured() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        Duration timeout = properties.getStreamingFirstByteTimeout();

        // Assert
        assertThat(timeout).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void test_returnsTenSecondConnectTimeout_when_notConfigured() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        Duration timeout = properties.getConnectTimeout();

        // Assert
        assertThat(timeout).isEqualTo(Duration.ofSeconds(10));
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

    @Test
    void test_returnsImagesGenerationsPath_when_openaiImagesPathNotConfigured() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        String path = properties.defaultPathFor(com.api2api.domain.channel.model.ProtocolType.OPENAI_IMAGES);

        // Assert
        assertThat(path).isEqualTo("/v1/images/generations");
    }
}
