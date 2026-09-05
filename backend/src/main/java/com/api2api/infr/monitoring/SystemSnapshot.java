package com.api2api.infr.monitoring;

import java.time.Instant;
import java.util.List;

/** All percentages use numerator and denominator from the same resource scope. */
public record SystemSnapshot(
        Instant sampledAt, Scope scope, String operatingSystem, Integer cpuCores,
        Double cpuPercent, List<Double> loadAverage, Double loadPercent,
        Resource memory, Resource disk, String diskScope, RuntimeMetrics runtime,
        DatabasePool databasePool, Health health, List<String> notices
) {
    public enum Scope { HOST_KERNEL, JVM_VISIBLE }
    public enum Health { HEALTHY, WARNING, CRITICAL, UNKNOWN }
    public record Resource(Long usedBytes, Long totalBytes, Double percent) { }
    public record RuntimeMetrics(long uptimeMillis, Resource heap, int threads, Double processCpuPercent) { }
    public record DatabasePool(int active, int idle, int total, int maximum, int waiting) { }
}
