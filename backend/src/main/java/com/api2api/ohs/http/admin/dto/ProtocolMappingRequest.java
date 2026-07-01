package com.api2api.ohs.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMappingRequest {

    @NotBlank(message = "Request protocol must not be blank")
    private String requestProtocol;

    @NotBlank(message = "Upstream protocol must not be blank")
    private String upstreamProtocol;
}
