package com.api2api.ohs.http.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Preview response for upstream provider model fetches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderModelPreviewResponse {

    private List<ChannelModelSupportResponse> models;
}
