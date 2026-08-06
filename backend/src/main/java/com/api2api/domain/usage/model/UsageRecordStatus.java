package com.api2api.domain.usage.model;

/**
 * Lifecycle status of a usage record.
 *
 * <p>PENDING records represent in-flight token reservations created when a request passes quota
 * checks. They are updated to SUCCESS or FAILED once the invocation completes, or deleted if the
 * request fails without billable usage. Including PENDING records in quota sums prevents concurrent
 * requests from observing stale consumed-token totals (TOCTOU).
 */
public enum UsageRecordStatus {
    /** Pre-committed reservation written atomically with the quota check. */
    PENDING,
    SUCCESS,
    FAILED
}
