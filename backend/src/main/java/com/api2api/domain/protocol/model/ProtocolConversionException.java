package com.api2api.domain.protocol.model;

/**
 * 协议转换领域异常。
 */
public class ProtocolConversionException extends RuntimeException {

    public ProtocolConversionException(String message) {
        super(message);
    }

    public ProtocolConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
