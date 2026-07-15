package com.api2api.ohs.http.gateway;

import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;
import java.util.Objects;

/** OpenAI Responses wrapper backed by values read through executable contract FieldRefs. */
final class OpenAIResponsesGatewayRequest implements GatewayProtocolRequest {

    private final ParsedGatewayRequest parsedRequest;

    private OpenAIResponsesGatewayRequest(ParsedGatewayRequest parsedRequest) {
        this.parsedRequest = Objects.requireNonNull(parsedRequest, "parsedRequest must not be null");
    }

    static OpenAIResponsesGatewayRequest fromContract(ParsedGatewayRequest parsedRequest) {
        return new OpenAIResponsesGatewayRequest(parsedRequest);
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
