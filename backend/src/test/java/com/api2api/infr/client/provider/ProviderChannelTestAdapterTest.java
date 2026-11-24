package com.api2api.infr.client.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.api2api.application.BusinessException;
import com.api2api.application.gateway.ProviderGatewayResponse;
import com.api2api.domain.channel.model.ChannelModelSupport;
import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.channel.model.ProviderChannel;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.protocol.model.ProtocolConversionResult;
import com.api2api.domain.protocol.model.ProtocolPayload;
import com.api2api.domain.protocol.repository.ProtocolConversionDefinitionRepository;
import com.api2api.domain.protocol.service.ProtocolConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class ProviderChannelTestAdapterTest {

    private final ObjectMapper json = new ObjectMapper();
    private final BearerTokenProviderCallStrategy transport = mock(BearerTokenProviderCallStrategy.class);
    private final ProtocolConversionService conversion = mock(ProtocolConversionService.class);
    private final ProviderChannel channel = mock(ProviderChannel.class);
    private final ChannelModelSupport model = mock(ChannelModelSupport.class);
    private final ProviderChannelTestAdapter adapter = new ProviderChannelTestAdapter(json,
            mock(ProtocolConversionDefinitionRepository.class), conversion, transport);

    @ParameterizedTest
    @CsvSource({
            "OPENAI_CHAT_COMPLETIONS, /messages/0/content, '{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}'",
            "CLAUDE_MESSAGES, /messages/0/content, '{\"content\":[{\"type\":\"text\",\"text\":\"hi\"}]}'",
            "OPENAI_RESPONSES, /input, '{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"hi\"}]}]}'",
            "OPENAI_IMAGES, /prompt, '{\"data\":[{\"url\":\"https://example.test/image\"}]}'"
    })
    void test_sends_hello_to_saved_model_when_native_protocol_is_selected(
            ProtocolType protocol, String promptPath, String response
    ) throws Exception {
        givenUpstream(protocol, 200, response);

        var result = adapter.sendHello(channel, protocol, model);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(transport).sendTest(eq(channel), eq(model), body.capture());
        var request = json.readTree(body.getValue());
        assertThat(request.path("model").textValue()).isEqualTo("upstream-model");
        assertThat(request.at(promptPath).textValue()).isEqualTo("hello");
        assertThat(result.reply()).isEqualTo(protocol == ProtocolType.OPENAI_IMAGES ? "上游已返回 1 张图片" : "hi");
    }

    @ParameterizedTest
    @ValueSource(strings = {"<html>OK</html>", "{}", "{\"error\":{\"message\":\"denied\"}}", "{\"choices\":[]}"})
    void test_rejects_false_success_when_upstream_returns_no_completion(String response) {
        givenUpstream(ProtocolType.OPENAI_CHAT_COMPLETIONS, 200, response);

        assertThatThrownBy(() -> adapter.sendHello(channel, ProtocolType.OPENAI_CHAT_COMPLETIONS, model))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 429, 502})
    void test_reports_status_without_raw_body_when_provider_rejects_request(int status) {
        givenUpstream(ProtocolType.OPENAI_CHAT_COMPLETIONS, status, "private upstream diagnostic");

        assertThatThrownBy(() -> adapter.sendHello(channel, ProtocolType.OPENAI_CHAT_COMPLETIONS, model))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining("private upstream diagnostic");
    }

    private void givenUpstream(ProtocolType protocol, int status, String response) {
        when(channel.id()).thenReturn(ProviderChannelId.of(7L));
        when(channel.keyRef()).thenReturn(ProviderKeyRef.of("test-credential"));
        when(model.upstreamProtocol()).thenReturn(protocol);
        when(model.upstreamModel()).thenReturn(ModelName.of("upstream-model"));
        when(conversion.convertRequest(any(ProtocolPayload.class), eq(protocol), any(), any()))
                .thenAnswer(call -> ProtocolConversionResult.passthrough(call.getArgument(0)));
        when(transport.sendTest(eq(channel), eq(model), anyString()))
                .thenReturn(ProviderGatewayResponse.of(protocol, status, Map.of(), response, false));
    }
}
