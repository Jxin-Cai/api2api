package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.FilePart;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultipartFormBodyWriterTest {

    @Test
    void test_declaresBoundaryInContentType_when_writingForm() {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(List.of(new TextField("prompt", "hi")), List.of());

        // Act
        UpstreamRequestBody body = MultipartFormBodyWriter.write(payload);

        // Assert
        String boundary = body.contentType().substring("multipart/form-data; boundary=".length());
        assertThat(body.contentType()).startsWith("multipart/form-data; boundary=");
        assertThat(new String(body.content(), StandardCharsets.UTF_8))
                .startsWith("--" + boundary + "\r\n")
                .endsWith("--" + boundary + "--\r\n");
    }

    @Test
    void test_writesTextFieldAsFormDataPart_when_fieldIsPresent() {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(List.of(new TextField("prompt", "añadir")), List.of());

        // Act
        String body = new String(MultipartFormBodyWriter.write(payload).content(), StandardCharsets.UTF_8);

        // Assert
        assertThat(body).contains("Content-Disposition: form-data; name=\"prompt\"\r\n\r\nañadir\r\n");
    }

    @Test
    void test_writesFilePartWithFilenameAndContentType_when_fileIsPresent() {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(
                List.of(),
                List.of(new FilePart("image", "cat.png", "image/png", new byte[] {0x10, 0x20}))
        );

        // Act
        String body = new String(MultipartFormBodyWriter.write(payload).content(), StandardCharsets.ISO_8859_1);

        // Assert
        assertThat(body).contains(
                "Content-Disposition: form-data; name=\"image\"; filename=\"cat.png\"\r\n"
                        + "Content-Type: image/png\r\n\r\n\u0010\u0020\r\n");
    }

    @Test
    void test_fallsBackToOctetStream_when_fileHasNoContentType() {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(
                List.of(),
                List.of(new FilePart("mask", "m.png", null, new byte[] {1}))
        );

        // Act
        String body = new String(MultipartFormBodyWriter.write(payload).content(), StandardCharsets.UTF_8);

        // Assert
        assertThat(body).contains("Content-Type: application/octet-stream\r\n");
    }

    @Test
    void test_escapesQuotes_when_filenameContainsQuote() {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(
                List.of(),
                List.of(new FilePart("image", "a\"b.png", "image/png", new byte[] {1}))
        );

        // Act
        String body = new String(MultipartFormBodyWriter.write(payload).content(), StandardCharsets.UTF_8);

        // Assert
        assertThat(body).contains("filename=\"a%22b.png\"");
    }
}
