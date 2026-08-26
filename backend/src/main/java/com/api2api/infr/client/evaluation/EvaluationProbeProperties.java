package com.api2api.infr.client.evaluation;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the external probe service used to evaluate channels.
 */
@ConfigurationProperties(prefix = "api2api.evaluation-probe")
public class EvaluationProbeProperties {

    private String baseUrl = "https://bazaarlink.ai";
    private String runPath = "/api/probe/run";
    private String historyPath = "/api/probe/history";
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration requestTimeout = Duration.ofSeconds(30);

    /**
     * Longest a submitted run may stay unfinished before the poller gives up on it. A full 96-probe
     * suite takes a few minutes, so this only needs to catch runs the probe service silently dropped.
     */
    private Duration runTimeout = Duration.ofMinutes(30);

    /** How many unfinished runs one poll cycle refreshes. */
    private int pollBatchSize = 20;

    /** How many schedules one scheduler cycle fires. */
    private int scheduleBatchSize = 20;

    /** Delay between unfinished-run poll cycles. */
    private Duration pollInterval = Duration.ofSeconds(30);

    /** Delay between due-schedule scan cycles. */
    private Duration scheduleInterval = Duration.ofSeconds(60);

    /** Characters kept from each probe reason or error when compacting the stored report. */
    private int reportExcerptLength = 500;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = requireHttpUrl(baseUrl);
    }

    public String getRunPath() {
        return runPath;
    }

    public void setRunPath(String runPath) {
        this.runPath = requirePath(runPath, "Probe run path must start with /");
    }

    public String getHistoryPath() {
        return historyPath;
    }

    public void setHistoryPath(String historyPath) {
        this.historyPath = requirePath(historyPath, "Probe history path must start with /");
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = requirePositive(connectTimeout, "Probe connect timeout must be positive");
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requirePositive(requestTimeout, "Probe request timeout must be positive");
    }

    public Duration getRunTimeout() {
        return runTimeout;
    }

    public void setRunTimeout(Duration runTimeout) {
        this.runTimeout = requirePositive(runTimeout, "Probe run timeout must be positive");
    }

    public int getPollBatchSize() {
        return pollBatchSize;
    }

    public void setPollBatchSize(int pollBatchSize) {
        this.pollBatchSize = requirePositive(pollBatchSize, "Probe poll batch size must be positive");
    }

    public int getScheduleBatchSize() {
        return scheduleBatchSize;
    }

    public void setScheduleBatchSize(int scheduleBatchSize) {
        this.scheduleBatchSize = requirePositive(scheduleBatchSize, "Probe schedule batch size must be positive");
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = requirePositive(pollInterval, "Probe poll interval must be positive");
    }

    public Duration getScheduleInterval() {
        return scheduleInterval;
    }

    public void setScheduleInterval(Duration scheduleInterval) {
        this.scheduleInterval = requirePositive(scheduleInterval, "Probe schedule interval must be positive");
    }

    public int getReportExcerptLength() {
        return reportExcerptLength;
    }

    public void setReportExcerptLength(int reportExcerptLength) {
        this.reportExcerptLength = requirePositive(reportExcerptLength, "Probe report excerpt length must be positive");
    }

    /** Absolute URL of the run submission endpoint. */
    public String runEndpoint() {
        return baseUrl + runPath;
    }

    /** Absolute URL used to poll a single run. */
    public String runEndpoint(String providerRunId) {
        return baseUrl + runPath + "/" + providerRunId;
    }

    /** Absolute URL of the archived report of a run that is no longer live. */
    public String historyEndpoint(String providerRunId) {
        return baseUrl + historyPath + "/" + providerRunId;
    }

    private static String requireHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Probe base URL must not be blank");
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("Probe base URL must start with http:// or https://");
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String requirePath(String value, String message) {
        if (value == null || value.isBlank() || !value.trim().startsWith("/")) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static Duration requirePositive(Duration duration, String message) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return duration;
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
