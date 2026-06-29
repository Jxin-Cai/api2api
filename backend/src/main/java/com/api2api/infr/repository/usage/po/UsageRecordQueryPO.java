package com.api2api.infr.repository.usage.po;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordQueryPO {
    private Long userId;
    private Long apiCredentialId;
    private Long providerChannelId;
    private String model;
    private String protocol;
    private Instant startTime;
    private Instant endTime;
    private int pageNumber;
    private int pageSize;
}
