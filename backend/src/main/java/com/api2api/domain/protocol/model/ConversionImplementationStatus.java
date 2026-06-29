package com.api2api.domain.protocol.model;

/**
 * 协议转换实现完成度，用于后台展示和路由准入判断。
 */
public enum ConversionImplementationStatus {
    IMPLEMENTED,
    PARTIAL,
    NOT_IMPLEMENTED;

    public boolean isRoutable() {
        return this == IMPLEMENTED;
    }
}
