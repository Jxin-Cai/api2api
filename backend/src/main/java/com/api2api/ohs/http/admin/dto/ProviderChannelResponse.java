package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Provider channel response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderChannelResponse {

    private Long id;
    private String name;
    private String host;
    private String keyRef;
    private String keyMasked;
    private Boolean hasKey;
    private Integer routePriority;
    private Set<String> supportedProtocols;
    private List<ChannelModelSupportResponse> supportedModels;
    private String status;
    private Long createdAt;
    private Long updatedAt;
}
