package com.api2api.ohs.http.channel.dto;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderModelOptionResponse {

    private String model;
    private int providerCount;
    private Set<ProtocolType> protocols;
}
