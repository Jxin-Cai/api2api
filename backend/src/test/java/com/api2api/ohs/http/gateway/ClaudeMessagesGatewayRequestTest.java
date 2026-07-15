package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.infr.protocol.contract.ProtocolContractRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ClaudeMessagesGatewayRequestTest {

    private final ProtocolContractRegistry contractRegistry =
            new ProtocolContractRegistry(new ObjectMapper());

    @Test
    void shouldDetectAdvancedToolAndReasoningRequirementsFromConversationHistory() throws Exception {
        String body = """
                {
                  "model": "claude-opus-4-8",
                  "max_tokens": 4096,
                  "messages": [{
                    "role": "assistant",
                    "content": [
                      {"type":"thinking","thinking":"summary","signature":"opaque"},
                      {"type":"mcp_tool_use","id":"mcp_1","name":"lookup","server_name":"docs","input":{}}
                    ]
                  }]
                }
                """;

        ClaudeMessagesGatewayRequest request = ClaudeMessagesGatewayRequest.fromContract(
                contractRegistry.parseRequest(ProtocolType.CLAUDE_MESSAGES, body)
        );

        assertThat(request.toolCallingRequired()).isTrue();
        assertThat(request.reasoningRequired()).isTrue();
    }

    @Test
    void shouldTreatOutputEffortAsReasoningCapabilityRequirement() throws Exception {
        String body = """
                {
                  "model": "claude-opus-4-8",
                  "max_tokens": 4096,
                  "output_config": {"effort":"xhigh"},
                  "messages": [{"role":"user","content":"work"}]
                }
                """;

        ClaudeMessagesGatewayRequest request = ClaudeMessagesGatewayRequest.fromContract(
                contractRegistry.parseRequest(ProtocolType.CLAUDE_MESSAGES, body)
        );

        assertThat(request.reasoningRequired()).isTrue();
    }
}
