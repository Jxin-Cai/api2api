package com.api2api.ohs.http.gateway;

import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import java.util.Objects;

/** Claude request wrapper backed by values read through the executable contract FieldRefs. */
final class ClaudeMessagesGatewayRequest implements GatewayProtocolRequest {

    private final ParsedGatewayRequest parsedRequest;

    private ClaudeMessagesGatewayRequest(ParsedGatewayRequest parsedRequest) {
        this.parsedRequest = Objects.requireNonNull(parsedRequest, "parsedRequest must not be null");
    }

    static ClaudeMessagesGatewayRequest fromContract(ParsedGatewayRequest parsedRequest) {
        return new ClaudeMessagesGatewayRequest(parsedRequest);
    }

    @Override
    public String rawBody() {
        return parsedRequest.rawBody();
    }

    @Override
    public String model() {
        return parsedRequest.model();
    }

    @Override
    public boolean streaming() {
        return parsedRequest.streaming();
    }

    @Override
    public boolean toolCallingRequired() {
        return parsedRequest.toolCallingRequired();
    }

    @Override
    public boolean reasoningRequired() {
        return parsedRequest.reasoningRequired();
    }
}
