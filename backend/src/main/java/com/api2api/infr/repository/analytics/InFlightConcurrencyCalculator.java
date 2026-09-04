package com.api2api.infr.repository.analytics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Sweep-line calculator that turns request in-flight intervals into per-bucket peak concurrency.
 *
 * <p>An interval {@code [start, end)} contributes +1 at {@code start} and -1 at {@code end}. At identical
 * instants the -1 is applied first so back-to-back requests are not counted as overlapping. The peak of a
 * bucket also accounts for requests that were already running when the bucket began.
 */
final class InFlightConcurrencyCalculator {

    private InFlightConcurrencyCalculator() {
    }

    /**
     * Half-open in-flight interval of one request; {@code end} must be strictly after {@code start}.
     */
    record Interval(Instant start, Instant end) {

        Interval {
            Objects.requireNonNull(start, "Interval start must not be null");
            Objects.requireNonNull(end, "Interval end must not be null");
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Interval end must be after interval start");
            }
        }
    }

    /**
     * @return peak concurrency per bucket, in the same order as {@code buckets}
     */
    static List<Integer> peakPerBucket(List<Interval> intervals, List<TimeBucket> buckets) {
        List<Event> events = new ArrayList<>(intervals.size() * 2);
        for (Interval interval : intervals) {
            events.add(new Event(interval.start(), +1));
            events.add(new Event(interval.end(), -1));
        }
        events.sort(Comparator.comparing(Event::time).thenComparingInt(Event::delta));

        List<Integer> peaks = new ArrayList<>(buckets.size());
        int current = 0;
        int index = 0;
        for (TimeBucket bucket : buckets) {
            while (index < events.size() && events.get(index).time().isBefore(bucket.start())) {
                current += events.get(index).delta();
                index++;
            }
            int peak = current;
            while (index < events.size() && events.get(index).time().isBefore(bucket.end())) {
                current += events.get(index).delta();
                peak = Math.max(peak, current);
                index++;
            }
            peaks.add(peak);
        }
        return peaks;
    }

    private record Event(Instant time, int delta) {
    }
}
