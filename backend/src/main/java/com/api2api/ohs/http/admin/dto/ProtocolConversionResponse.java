package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Protocol conversion definition detail response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConversionResponse {

    private Long id;
    private String sourceProtocol;
    private String targetProtocol;
    private String kind;
    private String status;
    private String implementationStatus;
    private ProtocolConversionCapabilityResponse capability;
    private ProtocolConversionMappingResponse requestMapping;
    private ProtocolConversionMappingResponse responseMapping;
    private Long createdAt;
    private Long updatedAt;
}
