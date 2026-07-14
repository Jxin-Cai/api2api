package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMetadataListItemResponse {
    private Long id;
    private String protocolType;
    private String displayName;
    private String apiSpecVersion;
    private String description;
    private String defaultEndpointPath;
    private int fieldCount;
    private int inputFieldCount;
    private int outputFieldCount;
}
