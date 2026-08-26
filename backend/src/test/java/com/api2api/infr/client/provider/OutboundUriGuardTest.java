package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class OutboundUriGuardTest {

    @Test
    void test_returnsUri_when_httpUpstreamUsesPublicHost() {
        // Arrange
        URI uri = URI.create("http://8.8.8.8/v1/responses");

        // Act
        URI verifiedUri = OutboundUriGuard.verify(uri, false);

        // Assert
        assertThat(verifiedUri).isEqualTo(uri);
    }

    @Test
    void test_allowsPrivateNetworkHost_when_insecureHostsAreDisabled() {
        // Arrange
        URI uri = URI.create("http://172.21.0.5:4141/v1/models");

        // Act
        URI verifiedUri = OutboundUriGuard.verify(uri, false);

        // Assert
        assertThat(verifiedUri).isEqualTo(uri);
    }

    @Test
    void test_rejectsLocalhost_when_insecureHostsAreDisabled() {
        // Arrange
        URI uri = URI.create("http://localhost/v1/responses");

        // Act & Assert
        assertThatThrownBy(() -> OutboundUriGuard.verify(uri, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upstream host is not allowed");
    }

    @Test
    void test_rejectsMetadataAddress_when_insecureHostsAreDisabled() {
        // Arrange
        URI uri = URI.create("http://169.254.169.254/latest/meta-data");

        // Act & Assert
        assertThatThrownBy(() -> OutboundUriGuard.verify(uri, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upstream host is not allowed");
    }

    @Test
    void test_rejectsUnsupportedScheme_when_upstreamUsesFtp() {
        // Arrange
        URI uri = URI.create("ftp://8.8.8.8/v1/responses");

        // Act & Assert
        assertThatThrownBy(() -> OutboundUriGuard.verify(uri, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upstream URI must use http or https");
    }
}
