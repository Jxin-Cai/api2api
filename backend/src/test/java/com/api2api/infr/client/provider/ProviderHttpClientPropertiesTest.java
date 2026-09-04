package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.gateway.ProtocolOperation;
import com.api2api.domain.channel.model.ProtocolType;
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

    @Test
    void test_returnsImagesEditsPath_when_operationIsImageEdits() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        String path = properties.upstreamPathFor(ProtocolType.OPENAI_IMAGES, ProtocolOperation.IMAGE_EDITS);

        // Assert
        assertThat(path).isEqualTo("/v1/images/edits");
    }

    @Test
    void test_returnsImagesVariationsPath_when_operationIsImageVariations() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        String path = properties.upstreamPathFor(ProtocolType.OPENAI_IMAGES, ProtocolOperation.IMAGE_VARIATIONS);

        // Assert
        assertThat(path).isEqualTo("/v1/images/variations");
    }

    @Test
    void test_returnsCountTokensPath_when_operationIsCountTokens() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act
        String path = properties.upstreamPathFor(ProtocolType.CLAUDE_MESSAGES, ProtocolOperation.COUNT_TOKENS);

        // Assert
        assertThat(path).isEqualTo("/v1/messages/count_tokens");
    }

    @Test
    void test_rejectsOperation_when_protocolDoesNotDefineIt() {
        // Arrange
        ProviderHttpClientProperties properties = new ProviderHttpClientProperties();

        // Act & Assert
        assertThatThrownBy(() -> properties.upstreamPathFor(ProtocolType.OPENAI_CHAT_COMPLETIONS, ProtocolOperation.IMAGE_EDITS))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
