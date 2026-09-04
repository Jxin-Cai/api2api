package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.FilePart;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonMultipartFormPayloadCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonMultipartFormPayloadCodec codec = new JsonMultipartFormPayloadCodec(objectMapper);

    @Test
    void test_exposesTextFieldsAsTopLevelMembers_when_encodingForm() throws Exception {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(
                List.of(new TextField("model", "gpt-image-1"), new TextField("prompt", "a cat")),
                List.of()
        );

        // Act
        JsonNode envelope = objectMapper.readTree(codec.encode(payload));

        // Assert
        assertThat(envelope.path("model").asText()).isEqualTo("gpt-image-1");
        assertThat(envelope.path("prompt").asText()).isEqualTo("a cat");
    }

    @Test
    void test_writesStreamAsJsonBoolean_when_formFieldIsTextualTrue() throws Exception {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(
                List.of(new TextField("model", "gpt-image-1"), new TextField("stream", "true")),
                List.of()
        );

        // Act
        JsonNode envelope = objectMapper.readTree(codec.encode(payload));

        // Assert
        assertThat(envelope.path("stream").isBoolean()).isTrue();
        assertThat(envelope.path("stream").asBoolean()).isTrue();
    }

    @Test
    void test_groupsRepeatedFieldsIntoArray_when_formRepeatsName() throws Exception {
        // Arrange
        MultipartFormPayload payload = new MultipartFormPayload(
                List.of(new TextField("tag", "a"), new TextField("tag", "b")),
                List.of()
        );

        // Act
        JsonNode envelope = objectMapper.readTree(codec.encode(payload));

        // Assert
        assertThat(envelope.path("tag").isArray()).isTrue();
        assertThat(envelope.path("tag")).extracting(JsonNode::asText).containsExactly("a", "b");
    }

    @Test
    void test_roundTripsFieldsAndFiles_when_decodingOwnEnvelope() {
        // Arrange
        MultipartFormPayload original = new MultipartFormPayload(
                List.of(new TextField("model", "gpt-image-1"),
                        new TextField("stream", "false"),
                        new TextField("image[]", "ignored-text"),
                        new TextField("n", "2")),
                List.of(new FilePart("image[]", "a.png", "image/png", new byte[] {1, 2, 3}),
                        new FilePart("mask", "m.png", null, new byte[] {9}))
        );

        // Act
        MultipartFormPayload decoded = codec.decode(codec.encode(original));

        // Assert
        assertThat(decoded.fields()).containsExactlyElementsOf(original.fields());
        assertThat(decoded.files()).hasSize(2);
        assertThat(decoded.files().get(0).name()).isEqualTo("image[]");
        assertThat(decoded.files().get(0).filename()).isEqualTo("a.png");
        assertThat(decoded.files().get(0).contentType()).isEqualTo("image/png");
        assertThat(decoded.files().get(0).content()).containsExactly(1, 2, 3);
        assertThat(decoded.files().get(1).contentType()).isNull();
    }

    @Test
    void test_isReadableByImagesProtocolContract_when_envelopeCarriesModelAndStream() {
        // Arrange
        ProtocolContractRegistry registry = new ProtocolContractRegistry(objectMapper);
        String envelope = codec.encode(new MultipartFormPayload(
                List.of(new TextField("model", "gpt-image-1"),
                        new TextField("prompt", "a hat"),
                        new TextField("stream", "true")),
                List.of(new FilePart("image", "cat.png", "image/png", new byte[] {1}))
        ));

        // Act
        ParsedGatewayRequest parsed = registry.parseGatewayRequest(ProtocolType.OPENAI_IMAGES, envelope);

        // Assert
        assertThat(parsed.model()).isEqualTo("gpt-image-1");
        assertThat(parsed.streaming()).isTrue();
    }

    @Test
    void test_keepsRewrittenModel_when_modelMappingAdapterEditsEnvelope() {
        // Arrange
        JsonGatewayPayloadModelMappingAdapter mapping = new JsonGatewayPayloadModelMappingAdapter(objectMapper);
        String envelope = codec.encode(new MultipartFormPayload(
                List.of(new TextField("model", "alias"), new TextField("prompt", "a hat")),
                List.of(new FilePart("image", "cat.png", "image/png", new byte[] {1}))
        ));

        // Act
        MultipartFormPayload decoded = codec.decode(
                mapping.rewriteModel(ProtocolType.OPENAI_IMAGES, envelope, ModelName.of("gpt-image-1")));

        // Assert
        assertThat(decoded.fields()).contains(new TextField("model", "gpt-image-1"));
        assertThat(decoded.files()).hasSize(1);
    }

    @Test
    void test_rejectsEnvelope_when_bodyIsNotJsonObject() {
        // Arrange
        String envelope = "[1,2]";

        // Act & Assert
        assertThatThrownBy(() -> codec.decode(envelope))
                .isInstanceOf(IllegalStateException.class);
    }
}
