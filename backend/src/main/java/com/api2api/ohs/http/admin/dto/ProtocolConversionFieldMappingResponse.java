package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Protocol conversion field mapping response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConversionFieldMappingResponse {

    private String sourceField;
    private String targetField;
    private String ruleDescription;
    private String lossiness;
    private String category;
    private String mappingType;
    private String sourcePath;
    private String targetPath;
    private String sourceType;
    private String targetType;
    private Boolean required;
    private Boolean supported;
    private String defaultValue;
    private String condition;
    private String notes;
}
