package com.api2api.ohs.http;

/**
 * Raised when an HTTP API requires an authenticated user session.
 */
public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
