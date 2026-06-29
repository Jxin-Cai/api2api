package com.api2api.ohs.http.credential.dto;

/**
 * Request context for listing API credentials owned by the current user.
 * Current user identity is resolved from HTTP header, not from request body.
 */
public final class ListMyApiCredentialsRequest {
    // Marker class, current user resolved via CurrentUserContextResolver
}
