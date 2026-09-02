package com.api2api.domain.channel.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProtocolTypeTest {

    @Test
    void test_parsesOpenAIImages_when_externalValueIsEndpointPath() {
        // Arrange
        String external = "/v1/images/generations";

        // Act
        var parsed = ProtocolType.parseExternal(external);

        // Assert
        assertThat(parsed).contains(ProtocolType.OPENAI_IMAGES);
    }

    @Test
    void test_parsesOpenAIImages_when_externalValueUsesImagesAliases() {
        // Arrange
        var aliases = java.util.List.of("images", "images/generations", "image-generations", "OPENAI_IMAGES");

        // Act & Assert
        for (String alias : aliases) {
            assertThat(ProtocolType.parseExternal(alias))
                    .as("alias %s", alias)
                    .contains(ProtocolType.OPENAI_IMAGES);
        }
    }

    @Test
    void test_reportsClientFacing_when_protocolIsOpenAIImages() {
        // Arrange
        ProtocolType protocol = ProtocolType.OPENAI_IMAGES;

        // Act
        boolean clientFacing = protocol.isClientFacing();

        // Assert
        assertThat(clientFacing).isTrue();
    }

    @Test
    void test_returnsImagesGenerationsPath_when_defaultEndpointPathIsRequested() {
        // Arrange
        ProtocolType protocol = ProtocolType.OPENAI_IMAGES;

        // Act
        String path = protocol.defaultEndpointPath();

        // Assert
        assertThat(path).isEqualTo("/v1/images/generations");
    }
}
