package com.api2api.infr.repository.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.infr.repository.analytics.InFlightConcurrencyCalculator.Interval;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InFlightConcurrencyCalculatorTest {

    private static final Instant T0 = Instant.parse("2026-09-03T00:00:00Z");

    @Test
    void test_counts_overlapping_requests_when_they_share_a_bucket() {
        List<TimeBucket> buckets = TimeBucket.split(T0, T0.plusSeconds(600), Duration.ofSeconds(300), null);
        List<Interval> intervals = List.of(
                new Interval(T0.plusSeconds(10), T0.plusSeconds(60)),
                new Interval(T0.plusSeconds(20), T0.plusSeconds(40)),
                new Interval(T0.plusSeconds(30), T0.plusSeconds(35))
        );

        List<Integer> peaks = InFlightConcurrencyCalculator.peakPerBucket(intervals, buckets);

        assertThat(peaks).containsExactly(3, 0);
    }

    @Test
    void test_carries_running_request_into_next_bucket_when_it_spans_the_boundary() {
        List<TimeBucket> buckets = TimeBucket.split(T0, T0.plusSeconds(600), Duration.ofSeconds(300), null);
        List<Interval> intervals = List.of(new Interval(T0.plusSeconds(200), T0.plusSeconds(400)));

        List<Integer> peaks = InFlightConcurrencyCalculator.peakPerBucket(intervals, buckets);

        assertThat(peaks).containsExactly(1, 1);
    }

    @Test
    void test_does_not_overlap_back_to_back_requests_when_end_equals_next_start() {
        List<TimeBucket> buckets = TimeBucket.split(T0, T0.plusSeconds(300), Duration.ofSeconds(300), null);
        List<Interval> intervals = List.of(
                new Interval(T0.plusSeconds(10), T0.plusSeconds(20)),
                new Interval(T0.plusSeconds(20), T0.plusSeconds(30))
        );

        List<Integer> peaks = InFlightConcurrencyCalculator.peakPerBucket(intervals, buckets);

        assertThat(peaks).containsExactly(1);
    }

    @Test
    void test_stops_buckets_at_cutoff_when_window_extends_into_the_future() {
        List<TimeBucket> buckets = TimeBucket.split(T0, T0.plusSeconds(3600), Duration.ofSeconds(600), T0.plusSeconds(1500));

        assertThat(buckets).hasSize(3);
        assertThat(buckets.get(2).start()).isEqualTo(T0.plusSeconds(1200));
    }
}
