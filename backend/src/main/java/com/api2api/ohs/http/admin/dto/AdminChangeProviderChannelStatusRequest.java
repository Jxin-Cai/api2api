package com.api2api.ohs.http.admin.dto;

/**
 * Request context for changing provider channel status (enable/disable).
 * No request body needed as providerChannelId comes from path variable
 * and operatorUserId is resolved from current user context.
 */
public class AdminChangeProviderChannelStatusRequest {
    // Empty marker class - fields resolved from path variable and CurrentUserContextResolver
}
