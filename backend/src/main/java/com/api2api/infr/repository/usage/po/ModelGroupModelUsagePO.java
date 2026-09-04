package com.api2api.infr.repository.usage.po;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelGroupModelUsagePO {
    private Long modelGroupId;
    private String requestedModel;
    private BigDecimal actualTokens;
}
