package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Protocol conversion capability response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConversionCapabilityResponse {

    private Boolean supportsStreaming;
    private Boolean supportsToolCalling;
    private Boolean supportsReasoning;
    private Boolean supportsUsageMapping;
    private Boolean supportsCacheTokenMapping;
    private Set<String> supportedContentTypes;
}
