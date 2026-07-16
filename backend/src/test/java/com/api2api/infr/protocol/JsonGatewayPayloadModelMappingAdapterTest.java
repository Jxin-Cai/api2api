package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonGatewayPayloadModelMappingAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonGatewayPayloadModelMappingAdapter adapter = new JsonGatewayPayloadModelMappingAdapter(objectMapper);

    @Test
    void shouldNotWriteModelIntoBedrockConverseBody() {
        String body = "{\"messages\":[{\"role\":\"user\",\"content\":[{\"text\":\"hello\"}]}]}";

        String mapped = adapter.rewriteModel(
                ProtocolType.AWS_BEDROCK_CONVERSE,
                body,
                ModelName.of("anthropic.claude-opus-4-8")
        );

        assertThat(mapped).isEqualTo(body);
    }

    @Test
    void test_doesNotWriteModelIntoBody_when_bedrockClaudeMessagesUsesModelUri() {
        String body = "{\"anthropic_version\":\"bedrock-2023-05-31\",\"messages\":[]}";

        String mapped = adapter.rewriteModel(
                ProtocolType.AWS_BEDROCK_CLAUDE_MESSAGES,
                body,
                ModelName.of("anthropic.claude-opus-4-8")
        );

        assertThat(mapped).isEqualTo(body);
    }

    @Test
    void shouldRewriteModelForJsonProtocolsThatCarryItInTheBody() throws Exception {
        String mapped = adapter.rewriteModel(
                ProtocolType.OPENAI_RESPONSES,
                "{\"model\":\"alias\",\"input\":\"hello\"}",
                ModelName.of("gpt-5.5")
        );

        assertThat(objectMapper.readTree(mapped).path("model").asText()).isEqualTo("gpt-5.5");
    }
}
