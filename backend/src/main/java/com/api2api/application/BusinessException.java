package com.api2api.application;

import java.util.Objects;

/**
 * Business exception raised by application use case orchestration.
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code) {
        super(requireCode(code));
        this.code = requireCode(code);
    }

    public BusinessException(String code, Throwable cause) {
        super(requireCode(code), cause);
        this.code = requireCode(code);
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }

    private static String requireCode(String code) {
        String requiredCode = Objects.requireNonNull(code, "Business exception code must not be null").trim();
        if (requiredCode.isEmpty()) {
            throw new IllegalArgumentException("Business exception code must not be blank");
        }
        return requiredCode;
    }
}
