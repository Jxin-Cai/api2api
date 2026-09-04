package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StreamingPassthroughUsageExtractorTest {

    private final StreamingPassthroughUsageExtractor extractor =
            new StreamingPassthroughUsageExtractor(new ObjectMapper());

    @Test
    void test_flushesCompletedEvent_when_upstreamEventArrives() throws IOException {
        // Arrange
        String upstream = "event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n";
        CountingOutputStream downstream = new CountingOutputStream();

        // Act
        extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.CLAUDE_MESSAGES
        );

        // Assert
        assertThat(downstream.flushCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void test_relaysUpstreamBody_when_claudeStreamHasNoTerminalEvent() throws IOException {
        // Arrange
        String upstream = "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.CLAUDE_MESSAGES
        );

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8)).isEqualTo(upstream);
    }

    @Test
    void test_extractsUsage_when_claudeStreamOmitsTrailingBlankLine() throws IOException {
        // Arrange
        String upstream = "event: message_delta\n"
                + "data: {\"usage\":{\"input_tokens\":10,\"output_tokens\":5}}\n\n"
                + "event: message_stop\ndata: {\"type\":\"message_stop\"}\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.CLAUDE_MESSAGES
        );

        // Assert
        assertThat(usage.totalTokens()).isEqualTo(15);
    }

    @Test
    void test_detectsTerminalEvent_when_claudeStreamOmitsEventLines() throws IOException {
        // Arrange
        String upstream = "data: {\"type\":\"message_stop\"}\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.CLAUDE_MESSAGES
        );

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8)).isEqualTo(upstream);
    }

    @Test
    void test_acceptsDoneMarker_when_chatCompletionStreamEnds() throws IOException {
        // Arrange
        String upstream = "data: {\"choices\":[]}\n\ndata: [DONE]\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.OPENAI_CHAT_COMPLETIONS
        );

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8)).contains("data: [DONE]");
    }

    @Test
    void test_extractsUsage_when_imagesStreamEmitsCompletedEvent() throws IOException {
        // Arrange
        String upstream = "event: image_generation.partial_image\n"
                + "data: {\"type\":\"image_generation.partial_image\",\"b64_json\":\"aGk=\",\"partial_image_index\":0}\n\n"
                + "event: image_generation.completed\n"
                + "data: {\"type\":\"image_generation.completed\",\"b64_json\":\"aGk=\","
                + "\"usage\":{\"input_tokens\":42,\"output_tokens\":1000,\"total_tokens\":1042}}\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.OPENAI_IMAGES
        );

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(42);
        assertThat(usage.outputTokens()).isEqualTo(1000);
    }

    @Test
    void test_extractsUsage_when_imageEditStreamEmitsCompletedEvent() throws IOException {
        // Arrange
        String upstream = "event: image_edit.partial_image\n"
                + "data: {\"type\":\"image_edit.partial_image\",\"b64_json\":\"aGk=\",\"partial_image_index\":0}\n\n"
                + "event: image_edit.completed\n"
                + "data: {\"type\":\"image_edit.completed\",\"b64_json\":\"aGk=\","
                + "\"usage\":{\"input_tokens\":7,\"output_tokens\":300,\"total_tokens\":307}}\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        UnifiedTokenUsage usage = extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.OPENAI_IMAGES
        );

        // Assert
        assertThat(usage.inputTokens()).isEqualTo(7);
        assertThat(usage.outputTokens()).isEqualTo(300);
    }

    @Test
    void test_relaysUpstreamBodyUnchanged_when_imagesStreamIsPassedThrough() throws IOException {
        // Arrange
        String upstream = "event: image_generation.completed\n"
                + "data: {\"type\":\"image_generation.completed\",\"b64_json\":\"aGk=\"}\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act
        extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.OPENAI_IMAGES
        );

        // Assert
        assertThat(downstream.toString(StandardCharsets.UTF_8)).isEqualTo(upstream);
    }

    private static final class CountingOutputStream extends ByteArrayOutputStream {

        private int flushCount;

        @Override
        public void flush() throws IOException {
            super.flush();
            flushCount++;
        }

        int flushCount() {
            return flushCount;
        }
    }
}
