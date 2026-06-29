package com.api2api.ohs.http.admin.dto;

/**
 * Request context for changing protocol conversion status (enable/disable).
 * No request body needed as definitionId comes from path variable
 * and operatorUserId is resolved from current user context.
 */
public class AdminChangeProtocolConversionStatusRequest {
    // Empty marker class - fields resolved from path variable and CurrentUserContextResolver
}
