package com.api2api.ohs.http.gateway;

import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import java.util.Objects;

/** OpenAI Chat wrapper backed by values read through executable contract FieldRefs. */
final class OpenAIChatCompletionsGatewayRequest implements GatewayProtocolRequest {

    private final ParsedGatewayRequest parsedRequest;

    private OpenAIChatCompletionsGatewayRequest(ParsedGatewayRequest parsedRequest) {
        this.parsedRequest = Objects.requireNonNull(parsedRequest, "parsedRequest must not be null");
    }

    static OpenAIChatCompletionsGatewayRequest fromContract(ParsedGatewayRequest parsedRequest) {
        return new OpenAIChatCompletionsGatewayRequest(parsedRequest);
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
