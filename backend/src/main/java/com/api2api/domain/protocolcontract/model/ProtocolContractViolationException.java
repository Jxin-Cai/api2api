package com.api2api.domain.protocolcontract.model;

public final class ProtocolContractViolationException extends IllegalArgumentException {

    public ProtocolContractViolationException(String message) {
        super(message);
    }

    public ProtocolContractViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
