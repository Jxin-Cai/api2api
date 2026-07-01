package com.api2api.infr.repository.channel.po;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderChannelPO {
    private Long id;
    private String name;
    private String host;
    private String keyRef;
    private String modelsPath;
    private int routePriority;
    private String supportedProtocols;
    private String status;
    private Instant createdTime;
    private Instant updatedTime;
    private boolean deleted;
    @Builder.Default
    private List<ChannelProtocolMappingPO> protocolMappings = List.of();
    @Builder.Default
    private List<ChannelModelSupportPO> supportedModels = List.of();
}
