package com.api2api.application.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import org.junit.jupiter.api.Test;

class ProtocolOperationTest {

    @Test
    void test_isAvailableOnEveryProtocol_when_operationIsInvoke() {
        // Arrange
        ProtocolOperation operation = ProtocolOperation.INVOKE;

        // Act & Assert
        for (ProtocolType protocol : ProtocolType.values()) {
            assertThat(operation.availableOn(protocol)).as("%s", protocol).isTrue();
        }
    }

    @Test
    void test_isAvailableOnlyOnImagesProtocol_when_operationIsImageEdits() {
        // Arrange
        ProtocolOperation operation = ProtocolOperation.IMAGE_EDITS;

        // Act
        boolean onImages = operation.availableOn(ProtocolType.OPENAI_IMAGES);
        boolean onChat = operation.availableOn(ProtocolType.OPENAI_CHAT_COMPLETIONS);

        // Assert
        assertThat(onImages).isTrue();
        assertThat(onChat).isFalse();
    }

    @Test
    void test_isBillable_when_operationGeneratesImages() {
        // Arrange & Act & Assert
        assertThat(ProtocolOperation.IMAGE_EDITS.billable()).isTrue();
        assertThat(ProtocolOperation.IMAGE_VARIATIONS.billable()).isTrue();
    }

    @Test
    void test_isNotBillable_when_operationIsCountTokens() {
        // Arrange
        ProtocolOperation operation = ProtocolOperation.COUNT_TOKENS;

        // Act & Assert
        assertThat(operation.billable()).isFalse();
    }

    @Test
    void test_doesNotSupportStreaming_when_operationIsImageVariations() {
        // Arrange
        ProtocolOperation operation = ProtocolOperation.IMAGE_VARIATIONS;

        // Act & Assert
        assertThat(operation.supportsStreaming()).isFalse();
    }

    @Test
    void test_acceptsMultipartForm_when_operationIsImageEditsOrVariations() {
        // Arrange & Act & Assert
        assertThat(ProtocolOperation.IMAGE_EDITS.acceptsMultipartForm()).isTrue();
        assertThat(ProtocolOperation.IMAGE_VARIATIONS.acceptsMultipartForm()).isTrue();
        assertThat(ProtocolOperation.INVOKE.acceptsMultipartForm()).isFalse();
    }

    @Test
    void test_requiresNativeProtocol_when_operationIsNotInvoke() {
        // Arrange
        ProtocolOperation operation = ProtocolOperation.IMAGE_EDITS;

        // Act & Assert
        assertThat(operation.requiresNativeProtocol()).isTrue();
    }
}
