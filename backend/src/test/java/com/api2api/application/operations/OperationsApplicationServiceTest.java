package com.api2api.application.operations;

import com.api2api.application.BusinessException;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.repository.DashboardAnalyticsRepository;
import com.api2api.domain.user.model.*;
import com.api2api.domain.user.repository.UserAccountRepository;
import com.api2api.infr.monitoring.SystemMetricsCollector;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationsApplicationServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final DashboardAnalyticsRepository analytics = mock(DashboardAnalyticsRepository.class);
    private final SystemMetricsCollector collector = mock(SystemMetricsCollector.class);
    private final OperationsApplicationService service = new OperationsApplicationService(users, analytics, collector);
    private final UserAccountId id = UserAccountId.of(1L);

    private void user(UserRole role, UserAccountStatus status) {
        when(users.findById(id)).thenReturn(Optional.of(UserAccount.rehydrate(id, Username.of("operator"),
                DisplayName.of("Operator"), role, status, null, Instant.now(), Instant.now())));
    }

    @Test
    void test_deniesSystemMetrics_when_operatorIsRegularUser() {
        // Arrange
        user(UserRole.USER, UserAccountStatus.ACTIVE);
        // Act / Assert
        assertThatThrownBy(() -> service.system(id)).isInstanceOf(IllegalStateException.class).hasMessageContaining("ACCESS_DENIED");
    }

    @Test
    void test_neverCollectsHostData_when_operatorIsRegularUser() {
        // Arrange
        user(UserRole.USER, UserAccountStatus.ACTIVE);
        // Act
        catchThrowable(() -> service.system(id));
        // Assert
        verifyNoInteractions(collector);
    }

    @Test
    void test_deniesTraffic_when_operatorIsRegularUser() {
        // Arrange
        user(UserRole.USER, UserAccountStatus.ACTIVE);
        // Act / Assert
        assertThatThrownBy(() -> service.traffic(id, ZoneId.of("UTC"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void test_deniesSystemMetrics_when_operatorMissing() {
        // Arrange
        when(users.findById(id)).thenReturn(Optional.empty());
        // Act / Assert
        assertThatThrownBy(() -> service.system(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void test_collectsSystemMetrics_when_operatorIsAdmin() {
        // Arrange
        user(UserRole.ADMIN, UserAccountStatus.ACTIVE);
        // Act
        service.system(id);
        // Assert
        verify(collector).snapshot();
    }

    @Test
    void test_preservesConcurrencyBucketSize_when_requestingTraffic() {
        // Arrange
        user(UserRole.ADMIN, UserAccountStatus.ACTIVE);
        // Act
        service.traffic(id, ZoneId.of("Asia/Shanghai"));
        // Assert
        verify(analytics).calculateConcurrencyTrends(any(), eq(Duration.ofMinutes(5)), any());
    }

    @Test
    void test_usesRequestedLocalDay_when_requestingTraffic() {
        // Arrange
        user(UserRole.ADMIN, UserAccountStatus.ACTIVE);
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        // Act
        var result = service.traffic(id, zone);
        // Assert
        var window = ArgumentCaptor.forClass(AnalyticsTimeWindow.class);
        verify(analytics).findSlowestChannels(window.capture(), eq(5));
        assertThat(window.getValue().startInclusive()).isEqualTo(result.sampledAt().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant());
    }
}
