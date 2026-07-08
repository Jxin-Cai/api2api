package com.api2api.ohs.http.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for previewing provider models from upstream without persisting them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminFetchProviderModelPreviewRequest {

    @NotBlank(message = "Provider host must not be blank")
    private String host;

    @NotBlank(message = "Provider key must not be blank")
    private String keyRef;

    private String modelsPath;

    private Set<String> upstreamProtocols;

    private Set<String> supportedProtocols;

    @Valid
    private List<ProtocolMappingRequest> protocolMappings;

    @NotNull(message = "Default priority must not be null")
    @Min(value = 1, message = "Default priority must be greater than or equal to 1")
    private Integer defaultPriority;
}
