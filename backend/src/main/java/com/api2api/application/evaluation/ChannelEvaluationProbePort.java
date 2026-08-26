package com.api2api.application.evaluation;

/**
 * Outbound port to the external probe service that scores a channel model.
 */
public interface ChannelEvaluationProbePort {

    /**
     * Submits an evaluation and returns immediately with the identifier the probe service assigned.
     * The run itself executes asynchronously upstream and must be polled via {@link #fetch(String)}.
     *
     * @param submission channel endpoint, credential and model to evaluate
     * @return probe service run identifier
     */
    String submit(ProbeSubmission submission);

    /**
     * Reads the current state of a previously submitted run.
     *
     * @param providerRunId identifier returned by {@link #submit(ProbeSubmission)}
     * @return current run state, including the score once it succeeded
     */
    ProbeRunSnapshot fetch(String providerRunId);
}
