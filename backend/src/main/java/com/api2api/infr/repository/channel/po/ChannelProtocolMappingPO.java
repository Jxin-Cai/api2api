package com.api2api.infr.repository.channel.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelProtocolMappingPO {
    private Long providerChannelId;
    private String requestProtocol;
    private String upstreamProtocol;
    private Instant createdTime;
    private Instant updatedTime;
}
