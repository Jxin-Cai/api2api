package com.api2api.domain.protocol.model;

/**
 * 协议转换定义的启停状态。
 */
public enum ConversionStatus {
    ENABLED,
    DISABLED,
    NOT_IMPLEMENTED;

    public boolean canBeEnabled() {
        return this != NOT_IMPLEMENTED;
    }
}
