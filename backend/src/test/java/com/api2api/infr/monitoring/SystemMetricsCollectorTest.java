package com.api2api.infr.monitoring;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.api2api.infr.monitoring.SystemSnapshot.*;

class SystemMetricsCollectorTest {
    private final DatabasePool idlePool = new DatabasePool(1, 9, 10, 10, 0);

    @Test
    void test_reportsHealthy_when_allMetricsBelowThreshold() {
        // Arrange / Act
        Health health = SystemMetricsCollector.health(10.0, 20.0, 30.0, 40.0, 50.0, idlePool);
        // Assert
        assertThat(health).isEqualTo(Health.HEALTHY);
    }

    @Test
    void test_reportsWarning_when_resourceReachesEightyPercent() {
        // Arrange / Act
        Health health = SystemMetricsCollector.health(80.0, 20.0, 30.0, 40.0, 50.0, idlePool);
        // Assert
        assertThat(health).isEqualTo(Health.WARNING);
    }

    @Test
    void test_reportsCritical_when_resourceReachesNinetyFivePercent() {
        // Arrange / Act
        Health health = SystemMetricsCollector.health(10.0, 95.0, 30.0, 40.0, 50.0, idlePool);
        // Assert
        assertThat(health).isEqualTo(Health.CRITICAL);
    }

    @Test
    void test_reportsCritical_when_normalizedLoadReachesOnePointFive() {
        // Arrange / Act
        Health health = SystemMetricsCollector.health(10.0, 20.0, 30.0, 150.0, 50.0, idlePool);
        // Assert
        assertThat(health).isEqualTo(Health.CRITICAL);
    }

    @Test
    void test_reportsWarning_when_databaseHasWaiters() {
        // Arrange
        DatabasePool busy = new DatabasePool(10, 0, 10, 10, 1);
        // Act
        Health health = SystemMetricsCollector.health(10.0, 20.0, 30.0, 40.0, 50.0, busy);
        // Assert
        assertThat(health).isEqualTo(Health.WARNING);
    }

    @Test
    void test_doesNotClaimHealthy_when_metricUnavailable() {
        // Arrange / Act
        Health health = SystemMetricsCollector.health(null, 20.0, 30.0, 40.0, 50.0, idlePool);
        // Assert
        assertThat(health).isEqualTo(Health.UNKNOWN);
    }

    @Test
    void test_preservesCriticalSignal_when_otherMetricMissing() {
        // Arrange / Act
        Health health = SystemMetricsCollector.health(null, 95.0, 30.0, 40.0, 50.0, idlePool);
        // Assert
        assertThat(health).isEqualTo(Health.CRITICAL);
    }

    @Test
    void test_reusesSnapshot_when_requestedWithinFiveSeconds() {
        // Arrange
        var collector = new SystemMetricsCollector(new HostResourceReader(), mock(DataSource.class), "/proc", "/");
        var first = collector.snapshot();
        // Act
        var second = collector.snapshot();
        // Assert
        assertThat(second).isSameAs(first);
    }
}
