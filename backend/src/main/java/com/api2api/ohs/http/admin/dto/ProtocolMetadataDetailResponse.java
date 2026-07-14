package com.api2api.ohs.http.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMetadataDetailResponse {
    private Long id;
    private String protocolType;
    private String displayName;
    private String apiSpecVersion;
    private String description;
    private String defaultEndpointPath;
    private List<ProtocolFieldSectionResponse> sections;
    private int fieldCount;
    private int inputFieldCount;
    private int outputFieldCount;
    private Long createdAt;
    private Long updatedAt;
}
