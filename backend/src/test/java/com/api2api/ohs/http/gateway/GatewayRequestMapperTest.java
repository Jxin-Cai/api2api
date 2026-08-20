package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.api2api.application.gateway.InboundRequestContext;
import com.api2api.application.gateway.ProtocolOperation;
import com.api2api.application.gateway.command.InvokeGatewayCommand;
import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.credential.model.ApiKeyHash;
import com.api2api.domain.gateway.model.GatewayInvocationId;
import com.api2api.domain.gateway.model.GatewayRequestId;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import com.api2api.domain.usage.model.UsageRecordId;
import org.junit.jupiter.api.Test;

class GatewayRequestMapperTest {

    @Test
    void test_disablesStreaming_when_operationDoesNotSupportIt() {
        // Arrange
        GatewayApiKeyHashHelper keyHashHelper = mock(GatewayApiKeyHashHelper.class);
        GatewayIdentifierHelper identifierHelper = mock(GatewayIdentifierHelper.class);
        when(keyHashHelper.hashGatewayApiKey("Bearer key", null))
                .thenReturn(ApiKeyHash.of("a".repeat(64)));
        when(identifierHelper.nextInvocationId()).thenReturn(GatewayInvocationId.of(1L));
        when(identifierHelper.requestId("req-1")).thenReturn(GatewayRequestId.of("req-1"));
        when(identifierHelper.nextUsageRecordId()).thenReturn(UsageRecordId.of(1L));
        GatewayRequestMapper mapper = new GatewayRequestMapper(keyHashHelper, identifierHelper);
        GatewayProtocolRequest protocolRequest = ContractBackedGatewayRequest.fromContract(
                new ParsedGatewayRequest(
                        "{\"model\":\"claude-opus-4-8\",\"stream\":true,\"messages\":[]}",
                        "claude-opus-4-8",
                        true,
                        false,
                        false
                )
        );

        // Act
        InvokeGatewayCommand command = mapper.toCommand(
                protocolRequest,
                "Bearer key",
                null,
                "req-1",
                ProtocolType.CLAUDE_MESSAGES,
                InboundRequestContext.of(java.util.Map.of(), "beta=true", ProtocolOperation.COUNT_TOKENS)
        );

        // Assert
        assertThat(command.isStreaming()).isFalse();
        assertThat(command.getInbound().operation()).isEqualTo(ProtocolOperation.COUNT_TOKENS);
    }
}
