package com.api2api.infr.client.evaluation;

import com.api2api.application.evaluation.ChannelEvaluationApplicationService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically refreshes unfinished evaluation runs and fires due schedules.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelEvaluationJobs {

    @NonNull
    private final ChannelEvaluationApplicationService channelEvaluationApplicationService;

    @NonNull
    private final EvaluationProbeProperties properties;

    private final AtomicBoolean polling = new AtomicBoolean(false);
    private final AtomicBoolean scheduling = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${api2api.evaluation-probe.poll-interval:30s}")
    public void pollInFlight() {
        if (!polling.compareAndSet(false, true)) {
            return;
        }
        try {
            int refreshed = channelEvaluationApplicationService.refreshInFlight(
                    properties.getPollBatchSize(),
                    properties.getRunTimeout()
            );
            if (refreshed > 0) {
                log.info("Refreshed {} in-flight channel evaluations", refreshed);
            }
        } catch (RuntimeException exception) {
            log.error("Failed to poll in-flight channel evaluations", exception);
        } finally {
            polling.set(false);
        }
    }

    @Scheduled(fixedDelayString = "${api2api.evaluation-probe.schedule-interval:60s}")
    public void fireDueSchedules() {
        if (!scheduling.compareAndSet(false, true)) {
            return;
        }
        try {
            int fired = channelEvaluationApplicationService.fireDueSchedules(properties.getScheduleBatchSize());
            if (fired > 0) {
                log.info("Fired {} due channel evaluation schedules", fired);
            }
        } catch (RuntimeException exception) {
            log.error("Failed to fire due channel evaluation schedules", exception);
        } finally {
            scheduling.set(false);
        }
    }
}
