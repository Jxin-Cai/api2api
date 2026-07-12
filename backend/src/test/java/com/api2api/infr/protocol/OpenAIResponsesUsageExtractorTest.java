package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenAIResponsesUsageExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAIResponsesUsageExtractor extractor = new OpenAIResponsesUsageExtractor();

    @Test
    void test_extractsCacheCreationTokens_when_responsesUsesNestedCreationField() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"input_tokens":120,"output_tokens":20,"input_tokens_details":{
                  "cached_tokens":30,"cache_creation_input_tokens":40
                }}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage).extracting(
                UnifiedTokenUsage::inputTokens,
                UnifiedTokenUsage::outputTokens,
                UnifiedTokenUsage::cacheCreationInputTokens,
                UnifiedTokenUsage::cacheReadInputTokens,
                UnifiedTokenUsage::totalTokens
        ).containsExactly(50L, 20L, 40L, 30L, 140L);
    }

    @Test
    void test_extractsCacheCreationTokens_when_responsesUsesTopLevelCreationField() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"input_tokens":80,"output_tokens":10,"cache_creation_input_tokens":25,
                  "input_tokens_details":{"cached_tokens":15}}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.cacheCreationInputTokens()).isEqualTo(25L);
    }
}
