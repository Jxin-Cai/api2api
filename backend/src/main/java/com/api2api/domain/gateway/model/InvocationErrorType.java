package com.api2api.domain.gateway.model;

/**
 * Domain error categories that can terminate a gateway invocation.
 */
public enum InvocationErrorType {
    AUTHENTICATION_FAILED,
    MODEL_NOT_ALLOWED,
    QUOTA_EXHAUSTED,
    NO_AVAILABLE_CHANNEL,
    CONVERSION_FAILED,
    UPSTREAM_FAILED
}
