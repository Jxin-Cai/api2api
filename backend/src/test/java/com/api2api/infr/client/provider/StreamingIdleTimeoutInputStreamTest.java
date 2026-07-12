package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StreamingIdleTimeoutInputStreamTest {

    @Test
    void test_throwsSocketTimeoutException_when_firstBodyByteNeverArrives() {
        // Arrange
        BlockingInputStream upstream = new BlockingInputStream();
        StreamingIdleTimeoutInputStream stream = new StreamingIdleTimeoutInputStream(
                upstream,
                Duration.ofMillis(25),
                Duration.ofSeconds(1)
        );

        // Act / Assert
        assertThatThrownBy(stream::read)
                .isInstanceOf(SocketTimeoutException.class)
                .hasMessageContaining("idle timeout");
    }

    private static final class BlockingInputStream extends InputStream {
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            try {
                if (!closed.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Test stream was not closed by its timeout");
                }
                return -1;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Test stream read was interrupted", exception);
            }
        }

        @Override
        public void close() {
            closed.countDown();
        }
    }
}
