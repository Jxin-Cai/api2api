package com.api2api.application.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Port for converting an upstream streaming protocol response into the client streaming protocol.
 */
public interface GatewayStreamingConversionPort {

    /**
     * Returns whether this port can convert a streaming response between the two protocols.
     *
     * @param upstreamProtocol protocol returned by the provider stream
     * @param clientProtocol protocol expected by the gateway client
     * @return true when a streaming response transformer is available
     */
    boolean supports(ProtocolType upstreamProtocol, ProtocolType clientProtocol);

    /**
     * Converts upstream streaming bytes to client-compatible streaming bytes.
     * Implementations must not close either stream; ownership remains with the caller.
     *
     * @param context protocols and client-facing model for the converted stream
     * @param upstreamBody upstream response body
     * @param clientBody client response body
     * @return token usage extracted from stream metadata, or unknown when unavailable
     * @throws IOException when stream reading or writing fails
     */
    UnifiedTokenUsage transform(
            GatewayStreamingConversionContext context,
            InputStream upstreamBody,
            OutputStream clientBody
    ) throws IOException;
}
