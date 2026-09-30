package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BedrockEventStreamReaderTest {

    @Test
    void test_decodesFrame_when_headersAndPayloadArePresent() throws IOException {
        // Arrange
        byte[] headers = stringHeaders(":event-type", "chunk", ":message-type", "event", ":exception-type", "");
        byte[] payload = "{\"bytes\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        var stream = new ByteArrayInputStream(frame(headers, payload));

        // Act
        var event = BedrockEventStreamReader.readEvent(stream);

        // Assert
        assertThat(event).extracting("eventType", "messageType", "exceptionType", "payload")
                .containsExactly("chunk", "event", "", payload);
    }

    @Test
    void test_retainsExceptionType_when_frameDescribesUpstreamFailure() throws IOException {
        // Arrange
        var stream = new ByteArrayInputStream(frame(
                stringHeaders(":message-type", "exception", ":exception-type", "throttlingException"), new byte[0]));

        // Act
        var event = BedrockEventStreamReader.readEvent(stream);

        // Assert
        assertThat(event.exceptionType()).isEqualTo("throttlingException");
    }

    @Test
    void test_skipsTypedMetadata_when_unrecognizedHeaderPrecedesEventType() throws IOException {
        // Arrange
        var headers = new ByteArrayOutputStream();
        headers.write(new byte[] {1, 'x', 4, 0, 0, 0, 42});
        headers.write(stringHeaders(":event-type", "chunk"));
        var stream = new ByteArrayInputStream(frame(headers.toByteArray(), new byte[0]));

        // Act
        var event = BedrockEventStreamReader.readEvent(stream);

        // Assert
        assertThat(event.eventType()).isEqualTo("chunk");
    }

    @Test
    void test_readsConsecutiveFrames_when_streamContainsMultipleEvents() throws IOException {
        // Arrange
        var bytes = new ByteArrayOutputStream();
        bytes.write(frame(stringHeaders(":event-type", "first"), new byte[0]));
        bytes.write(frame(stringHeaders(":event-type", "second"), new byte[0]));
        var stream = new ByteArrayInputStream(bytes.toByteArray());

        // Act
        var first = BedrockEventStreamReader.readEvent(stream);
        var second = BedrockEventStreamReader.readEvent(stream);

        // Assert
        assertThat(Arrays.asList(first.eventType(), second.eventType())).containsExactly("first", "second");
    }

    @Test
    void test_returnsEndOfStream_when_noFrameRemains() throws IOException {
        // Arrange
        var stream = new ByteArrayInputStream(new byte[0]);

        // Act
        var event = BedrockEventStreamReader.readEvent(stream);

        // Assert
        assertThat(event).isNull();
    }

    @ParameterizedTest
    @CsvSource({"15, 0", "16, -1"})
    void test_rejectsFrame_when_lengthsAreInvalid(int totalLength, int headersLength) {
        // Arrange
        var prelude = ByteBuffer.allocate(12).putInt(totalLength).putInt(headersLength).putInt(0).array();
        var stream = new ByteArrayInputStream(prelude);

        // Act / Assert
        assertThatThrownBy(() -> BedrockEventStreamReader.readEvent(stream))
                .isInstanceOf(IOException.class).hasMessage("Invalid Bedrock event-stream frame length");
    }

    @Test
    void test_rejectsFrame_when_checksumBytesAreTruncated() throws IOException {
        // Arrange
        byte[] complete = frame(stringHeaders(":event-type", "chunk"), new byte[] {1, 2, 3});
        var stream = new ByteArrayInputStream(Arrays.copyOf(complete, complete.length - 1));

        // Act / Assert
        assertThatThrownBy(() -> BedrockEventStreamReader.readEvent(stream)).isInstanceOf(EOFException.class);
    }

    @Test
    void test_rejectsHeader_when_typeIsUnsupported() throws IOException {
        // Arrange
        var stream = new ByteArrayInputStream(frame(new byte[] {1, 'x', 10}, new byte[0]));

        // Act / Assert
        assertThatThrownBy(() -> BedrockEventStreamReader.readEvent(stream))
                .isInstanceOf(IOException.class).hasMessage("Unsupported Bedrock event-stream header type: 10");
    }

    @Test
    void test_propagatesReadFailure_when_upstreamStreamFails() {
        // Arrange
        var failure = new IOException("synthetic read failure");
        var stream = new InputStream() {
            @Override
            public int read() throws IOException { throw failure; }
        };

        // Act / Assert
        assertThatThrownBy(() -> BedrockEventStreamReader.readEvent(stream)).isSameAs(failure);
    }

    private byte[] stringHeaders(String... namesAndValues) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var output = new DataOutputStream(bytes);
        for (int index = 0; index < namesAndValues.length; index += 2) {
            byte[] name = namesAndValues[index].getBytes(StandardCharsets.UTF_8);
            byte[] value = namesAndValues[index + 1].getBytes(StandardCharsets.UTF_8);
            output.writeByte(name.length);
            output.write(name);
            output.writeByte(7);
            output.writeShort(value.length);
            output.write(value);
        }
        return bytes.toByteArray();
    }

    private byte[] frame(byte[] headers, byte[] payload) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var output = new DataOutputStream(bytes);
        output.writeInt(16 + headers.length + payload.length);
        output.writeInt(headers.length);
        var crc = new CRC32();
        crc.update(bytes.toByteArray());
        output.writeInt((int) crc.getValue());
        output.write(headers);
        output.write(payload);
        crc.reset();
        crc.update(bytes.toByteArray());
        output.writeInt((int) crc.getValue());
        return bytes.toByteArray();
    }
}
