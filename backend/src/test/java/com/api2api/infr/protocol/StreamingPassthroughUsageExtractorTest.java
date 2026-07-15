package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
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
    void test_throwsEofException_when_claudeStreamHasNoTerminalEvent() {
        // Arrange
        String upstream = "event: content_block_delta\ndata: {\"type\":\"content_block_delta\"}\n\n";
        ByteArrayOutputStream downstream = new ByteArrayOutputStream();

        // Act / Assert
        assertThatThrownBy(() -> extractor.transferAndExtract(
                new ByteArrayInputStream(upstream.getBytes(StandardCharsets.UTF_8)),
                downstream,
                ProtocolType.CLAUDE_MESSAGES
        )).isInstanceOf(EOFException.class)
                .hasMessageContaining("terminal event");
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
