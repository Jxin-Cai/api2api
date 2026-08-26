package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class ClientDisconnectDetectorTest {

    @Test
    void test_detectsDisconnect_when_asyncRequestIsNotUsable() {
        AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(
                "ServletOutputStream failed to flush: java.io.IOException: Broken pipe",
                new IOException("Broken pipe")
        );

        boolean disconnected = ClientDisconnectDetector.isClientDisconnect(exception);

        assertThat(disconnected).isTrue();
    }

    @Test
    void test_detectsDisconnect_when_tomcatClientAborts() {
        ClientAbortException exception = new ClientAbortException(new IOException("Broken pipe"));

        boolean disconnected = ClientDisconnectDetector.isClientDisconnect(exception);

        assertThat(disconnected).isTrue();
    }

    @Test
    void test_detectsDisconnect_when_brokenPipeIsWrapped() {
        UncheckedIOException exception = new UncheckedIOException(new IOException("Broken pipe"));

        boolean disconnected = ClientDisconnectDetector.isClientDisconnect(exception);

        assertThat(disconnected).isTrue();
    }

    @Test
    void test_doesNotDetectDisconnect_when_upstreamStreamEndsEarly() {
        EOFException exception = new EOFException("Bedrock InvokeModel stream ended before message_stop");

        boolean disconnected = ClientDisconnectDetector.isClientDisconnect(exception);

        assertThat(disconnected).isFalse();
    }
}
