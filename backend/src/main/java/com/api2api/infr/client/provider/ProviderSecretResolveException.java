package com.api2api.infr.client.provider;

/**
 * Infrastructure exception raised when a provider key reference cannot be resolved.
 */
public class ProviderSecretResolveException extends RuntimeException {

    public ProviderSecretResolveException(String message) {
        super(message);
    }
}
