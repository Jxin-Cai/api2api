package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GptModelVersionTest {

    @ParameterizedTest
    @CsvSource({
            "gpt-5.6, 5, 6",
            "gpt-5.6-luna, 5, 6",
            "gpt-5-codex, 5, 0",
            "gpt-6, 6, 0",
            "gpt-6-astra, 6, 0",
            "GPT-6.1-Preview, 6, 1",
            "gpt-4o, 4, 0",
            "gpt-4.1-mini, 4, 1"
    })
    void test_parsesMajorAndMinor_when_modelIsVersionedGpt(String model, int major, int minor) {
        // Arrange / Act
        GptModelVersion version = GptModelVersion.parse(model).orElseThrow();

        // Assert
        assertThat(version).isEqualTo(new GptModelVersion(major, minor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"o3-mini", "gpt-oss-120b", "claude-opus-4", "gpt-", "gpt5.6"})
    void test_returnsEmpty_when_modelIsNotVersionedGpt(String model) {
        // Arrange / Act
        boolean parsed = GptModelVersion.parse(model).isPresent();

        // Assert
        assertThat(parsed).isFalse();
    }

    @Test
    void test_returnsEmpty_when_modelIsNull() {
        // Arrange / Act
        boolean parsed = GptModelVersion.parse(null).isPresent();

        // Assert
        assertThat(parsed).isFalse();
    }

    @Test
    void test_treatsNewMajorAsAtLeast_when_gateTargetsOlderMinorRelease() {
        // Arrange
        String model = "gpt-6-astra";

        // Act
        boolean atLeast = GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5_6);

        // Assert
        assertThat(atLeast).isTrue();
    }

    @Test
    void test_isNotAtLeast_when_minorReleasePredatesGate() {
        // Arrange
        String model = "gpt-5.5";

        // Act
        boolean atLeast = GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5_6);

        // Assert
        assertThat(atLeast).isFalse();
    }

    @Test
    void test_isNotAtLeast_when_modelIsUnversioned() {
        // Arrange
        String model = "o3-mini";

        // Act
        boolean atLeast = GptModelVersion.isAtLeast(model, GptModelVersion.GPT_5);

        // Assert
        assertThat(atLeast).isFalse();
    }
}
