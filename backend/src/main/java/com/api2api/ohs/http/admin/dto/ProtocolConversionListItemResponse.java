package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Protocol conversion definition list item response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConversionListItemResponse {

    private Long id;
    private String sourceProtocol;
    private String targetProtocol;
    private String kind;
    private String status;
    private String implementationStatus;
    private Boolean supportsStreaming;
    private Boolean supportsToolCalling;
    private Boolean supportsReasoning;
    private Boolean supportsUsageMapping;
    private Boolean supportsCacheTokenMapping;
}
