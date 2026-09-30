package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonGatewayPayloadModelMappingAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonGatewayPayloadModelMappingAdapter adapter = new JsonGatewayPayloadModelMappingAdapter(objectMapper);

    @Test
    void test_preservesParseFailureCause_when_bodyIsMalformedJson() {
        // Arrange
        String body = "{broken";

        // Act / Assert
        assertThatThrownBy(() -> adapter.rewriteModel(ProtocolType.OPENAI_RESPONSES, body, ModelName.of("model")))
                .isInstanceOf(ProtocolConversionException.class)
                .hasMessage("MODEL_MAPPING_FAILED")
                .hasCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    void test_preservesValidationError_when_bodyIsNotJsonObject() {
        // Arrange
        String body = "[]";

        // Act / Assert
        assertThatThrownBy(() -> adapter.rewriteModel(ProtocolType.OPENAI_RESPONSES, body, ModelName.of("model")))
                .isInstanceOf(ProtocolConversionException.class)
                .hasMessage("MODEL_MAPPING_REQUIRES_JSON_OBJECT");
    }

    @Test
    void test_propagatesUnexpectedFailure_when_jsonParserHasProgrammingError() throws Exception {
        // Arrange
        var failure = new IllegalStateException("synthetic parser failure");
        var failingMapper = spy(new ObjectMapper());
        doThrow(failure).when(failingMapper).readTree(anyString());
        var failingAdapter = new JsonGatewayPayloadModelMappingAdapter(failingMapper);

        // Act / Assert
        assertThatThrownBy(() -> failingAdapter.rewriteModel(ProtocolType.OPENAI_RESPONSES, "{}", ModelName.of("model")))
                .isSameAs(failure);
    }

    @Test
    void test_doesNotWriteModel_when_targetIsBedrockInvokeModel() {
        String body = "{\"messages\":[{\"role\":\"user\",\"content\":[{\"text\":\"hello\"}]}]}";

        String mapped = adapter.rewriteModel(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                body,
                ModelName.of("anthropic.claude-opus-4-8")
        );

        assertThat(mapped).isEqualTo(body);
    }

    @Test
    void test_rewritesModel_when_jsonProtocolCarriesModelInBody() throws Exception {
        String mapped = adapter.rewriteModel(
                ProtocolType.OPENAI_RESPONSES,
                "{\"model\":\"alias\",\"input\":\"hello\"}",
                ModelName.of("gpt-5.5")
        );

        assertThat(objectMapper.readTree(mapped).path("model").asText()).isEqualTo("gpt-5.5");
    }
}
