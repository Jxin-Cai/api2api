package com.api2api.domain.quota;

/** Safe management metadata; never includes key material. */
public record QuotaTarget(QuotaScope scope, String id, String name, String ownerName, String modelGroupId) {}
