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
public class ChannelModelSupportPO {
    private Long id;
    private Long providerChannelId;
    private String requestedModel;
    private String upstreamModel;
    private String upstreamProtocol;
    private int priority;
    private boolean preferred;
    private String source;
    private String status;
    private Instant createdTime;
    private Instant updatedTime;
}
