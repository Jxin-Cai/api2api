package com.api2api.application.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Port for passthrough streaming with usage extraction (no protocol conversion).
 */
public interface StreamingPassthroughPort {

    UnifiedTokenUsage transferAndExtract(
            InputStream input,
            OutputStream output,
            ProtocolType upstreamProtocol
    ) throws IOException;
}
