package com.api2api.infr.repository.protocol.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConversionDefinitionPO {
    private Long id;
    private String sourceProtocol;
    private String targetProtocol;
    private String kind;
    private String status;
    private boolean supportsStreaming;
    private boolean supportsToolCalling;
    private boolean supportsReasoning;
    private boolean supportsUsageMapping;
    private boolean supportsCacheTokenMapping;
    private String requestMappingJson;
    private String responseMappingJson;
    private String implementationStatus;
    private Instant createdTime;
    private Instant updatedTime;
}
