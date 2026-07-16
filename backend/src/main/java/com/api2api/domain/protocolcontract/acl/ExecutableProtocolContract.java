package com.api2api.domain.protocolcontract.acl;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocolcontract.model.ParsedGatewayRequest;

/** Framework-free port for executing protocol request and response contracts. */
public interface ExecutableProtocolContract {

    ParsedGatewayRequest parseRequest(ProtocolType protocolType, String rawBody);

    /**
     * Extracts only the request facts needed by the latency-sensitive gateway path.
     * Detailed leaf validation remains available through {@link #parseRequest}.
     */
    ParsedGatewayRequest parseGatewayRequest(ProtocolType protocolType, String rawBody);

    String buildResponse(ProtocolType protocolType, String rawBody);
}
