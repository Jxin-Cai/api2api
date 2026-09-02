package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAIImagesUsageExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAIImagesUsageExtractor extractor = new OpenAIImagesUsageExtractor();

    @Test
    void test_extractsInputAndOutputTokens_when_gptImageResponseCarriesUsage() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"created":1758290000,"data":[{"b64_json":"aGk="}],
                 "usage":{"input_tokens":50,"output_tokens":1056,"total_tokens":1106,
                  "input_tokens_details":{"text_tokens":50,"image_tokens":0}}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage).extracting(
                UnifiedTokenUsage::inputTokens,
                UnifiedTokenUsage::outputTokens,
                UnifiedTokenUsage::cacheCreationInputTokens,
                UnifiedTokenUsage::cacheReadInputTokens
        ).containsExactly(50L, 1056L, 0L, 0L);
    }

    @Test
    void test_returnsUnknownUsage_when_dalleResponseOmitsUsage() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"created":1758290000,"data":[{"url":"https://example.invalid/img.png"}]}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.usageKnown()).isFalse();
    }
}
