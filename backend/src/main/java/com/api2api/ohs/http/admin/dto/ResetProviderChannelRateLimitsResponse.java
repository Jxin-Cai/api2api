package com.api2api.ohs.http.admin.dto;

/** Result of resetting all provider channel model rate limits. */
public record ResetProviderChannelRateLimitsResponse(int restoredCount) {
}
