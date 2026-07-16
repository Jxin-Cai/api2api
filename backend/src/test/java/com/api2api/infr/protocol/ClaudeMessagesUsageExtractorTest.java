package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeMessagesUsageExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClaudeMessagesUsageExtractor extractor = new ClaudeMessagesUsageExtractor();

    @Test
    void test_mapsCachedTokensToCacheRead_when_standardCacheReadIsZero() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"input_tokens":0,"output_tokens":0,
                  "cache_creation_input_tokens":0,"cache_read_input_tokens":0,
                  "cached_tokens":233000}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.cacheReadInputTokens()).isEqualTo(233000L);
    }

    @Test
    void test_preservesStandardCacheRead_when_cachedTokensAliasAlsoExists() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"input_tokens":12,"output_tokens":7,
                  "cache_read_input_tokens":5,"cached_tokens":233000}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.cacheReadInputTokens()).isEqualTo(5L);
    }

    @Test
    void test_sumsCacheCreationDetails_when_aggregateCreationFieldIsZero() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"input_tokens":12,"output_tokens":7,
                  "cache_creation_input_tokens":0,
                  "cache_creation":{"ephemeral_5m_input_tokens":3,"ephemeral_1h_input_tokens":4}}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.cacheCreationInputTokens()).isEqualTo(7L);
    }

    @Test
    void test_mapsOpenAICompatibleTokenAliases_when_standardFieldsAreMissing() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"prompt_tokens":21,"completion_tokens":8}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(21L);
    }

    @Test
    void test_mapsCompletionTokensAlias_when_standardOutputFieldIsMissing() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"prompt_tokens":21,"completion_tokens":8}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.outputTokens()).isEqualTo(8L);
    }

    @Test
    void test_sumsIterationTokens_when_serverCompactionWasTriggered() throws Exception {
        // Arrange
        var payload = objectMapper.readTree("""
                {"usage":{"input_tokens":23000,"output_tokens":1000,"iterations":[
                  {"type":"compaction","input_tokens":180000,"output_tokens":3500},
                  {"type":"message","input_tokens":23000,"output_tokens":1000}
                ]}}
                """);

        // Act
        UnifiedTokenUsage usage = extractor.extract(payload);

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(203000L);
        assertThat(usage.outputTokens()).isEqualTo(4500L);
    }
}
