package com.api2api.infr.protocol;

import com.api2api.domain.channel.model.ProtocolType;
import com.api2api.domain.protocol.model.UnifiedTokenUsage;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 三方协议 usage 字段到统一 token 口径的提取 SPI。
 */
public interface UnifiedUsageExtractor {

    ProtocolType protocol();

    UnifiedTokenUsage extract(JsonNode payload);
}
