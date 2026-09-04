package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPart;

class MultipartFormRequestReaderTest {

    private final MultipartFormRequestReader reader = new MultipartFormRequestReader();

    @Test
    void test_readsTextPartsAsFields_when_partHasNoFilename() {
        // Arrange
        MockHttpServletRequest request = multipartRequest();
        request.addPart(new MockPart("model", "gpt-image-1".getBytes(StandardCharsets.UTF_8)));
        request.addPart(new MockPart("prompt", "a cat".getBytes(StandardCharsets.UTF_8)));

        // Act
        MultipartFormPayload payload = reader.read(request);

        // Assert
        assertThat(payload.fields())
                .containsExactly(new TextField("model", "gpt-image-1"), new TextField("prompt", "a cat"));
        assertThat(payload.files()).isEmpty();
    }

    @Test
    void test_readsFileParts_when_partHasFilename() {
        // Arrange
        MockHttpServletRequest request = multipartRequest();
        MockPart image = new MockPart("image", "cat.png", new byte[] {1, 2, 3});
        image.getHeaders().setContentType(org.springframework.http.MediaType.IMAGE_PNG);
        request.addPart(image);

        // Act
        MultipartFormPayload payload = reader.read(request);

        // Assert
        assertThat(payload.files()).hasSize(1);
        assertThat(payload.files().get(0).name()).isEqualTo("image");
        assertThat(payload.files().get(0).filename()).isEqualTo("cat.png");
        assertThat(payload.files().get(0).contentType()).isEqualTo("image/png");
        assertThat(payload.files().get(0).content()).containsExactly(1, 2, 3);
    }

    @Test
    void test_ignoresQueryParameters_when_readingFormFields() {
        // Arrange
        MockHttpServletRequest request = multipartRequest();
        request.setQueryString("beta=true");
        request.addParameter("beta", "true");
        request.addPart(new MockPart("model", "gpt-image-1".getBytes(StandardCharsets.UTF_8)));

        // Act
        MultipartFormPayload payload = reader.read(request);

        // Assert
        assertThat(payload.hasPart("beta")).isFalse();
    }

    @Test
    void test_rejectsRequest_when_contentTypeIsNotMultipart() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/images/edits");
        request.setContentType("application/json");

        // Act & Assert
        assertThatThrownBy(() -> reader.read(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multipart/form-data");
    }

    private static MockHttpServletRequest multipartRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/images/edits");
        request.setContentType("multipart/form-data; boundary=xyz");
        return request;
    }
}
