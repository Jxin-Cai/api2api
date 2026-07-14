package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolFieldDefinitionItemResponse {
    private Long id;
    private String fieldName;
    private String fieldPath;
    private String fieldType;
    private boolean required;
    private String usageDirection;
    private String description;
    private String purpose;
    private String usageContext;
    private int sortOrder;
}
