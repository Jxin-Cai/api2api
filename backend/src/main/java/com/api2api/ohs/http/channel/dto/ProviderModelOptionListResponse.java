package com.api2api.ohs.http.channel.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderModelOptionListResponse {

    private List<ProviderModelOptionResponse> models;
}
