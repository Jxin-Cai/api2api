package com.api2api.domain.protocol.model;

/**
 * 协议转换支持的内容映射类型。
 */
public enum ContentMappingType {
    TEXT,
    TOOL_CALL,
    REASONING,
    USAGE,
    CACHE_TOKENS,
    FINISH_REASON,
    STREAM_EVENT
}
