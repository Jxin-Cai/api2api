package com.api2api.ohs.http.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for creating a provider channel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateProviderChannelRequest {

    @NotBlank(message = "Channel name must not be blank")
    private String name;

    @NotBlank(message = "Provider host must not be blank")
    private String host;

    @NotBlank(message = "Provider key must not be blank")
    private String keyRef;

    private String modelsPath;

    private Integer routePriority;

    @NotEmpty(message = "Supported protocols must not be empty")
    private Set<String> supportedProtocols;

    @Valid
    private List<ProtocolMappingRequest> protocolMappings;
}
