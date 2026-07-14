package com.api2api.domain.protocolmetadata.model;

public class ProtocolMetadataException extends RuntimeException {

    public ProtocolMetadataException(String message) {
        super(message);
    }

    public ProtocolMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
