package com.api2api.infr.repository.credential.po;

import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelGroupPO {
    private Long id;
    private Long ownerUserId;
    private String name;
    private String modelWhitelist;
    private String modelDailyLimits;
    private Set<Long> allowedChannelIds;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
}
