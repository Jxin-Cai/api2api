package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Protocol conversion mapping response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConversionMappingResponse {

    private String direction;
    private String title;
    private String summary;
    private List<ProtocolConversionFieldMappingResponse> fieldMappings;
}
