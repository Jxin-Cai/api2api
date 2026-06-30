package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Channel model support response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelModelSupportResponse {

    private Long id;
    private String requestedModel;
    private String upstreamModel;
    private String upstreamProtocol;
    private Integer priority;
    private Boolean preferred;
    private String status;
    private String source;
    private Long createdAt;
    private Long updatedAt;
}
