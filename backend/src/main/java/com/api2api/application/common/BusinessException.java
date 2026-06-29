package com.api2api.application.common;

import java.util.Objects;

/**
 * Business exception raised by application use cases.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode) {
        super(Objects.requireNonNull(errorCode, "Error code must not be null"));
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message) {
        super(Objects.requireNonNull(message, "Error message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "Error code must not be null");
    }

    public String getErrorCode() {
        return errorCode;
    }
}
