package com.api2api.ohs.http.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Provider channel list response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderChannelListResponse {

    private List<ProviderChannelResponse> channels;
}
