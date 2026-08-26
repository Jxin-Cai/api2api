package com.api2api.ohs.http.dashboard.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.credential.model.ApiCredentialId;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.dashboard.DashboardTimeWindowHelper;
import com.api2api.ohs.http.dashboard.dto.GetFrontDashboardRequest;
import com.api2api.ohs.http.dashboard.dto.GetFrontKeyMetricsRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardCommandConverterTest {

    private final DashboardCommandConverter converter =
            new DashboardCommandConverter(new DashboardTimeWindowHelper());

    @Test
    void test_defaultsRecentCallsSizeToTwenty_when_sizeMissing() {
        GetFrontDashboardRequest request = new GetFrontDashboardRequest();

        var command = converter.toGetFrontDashboardCommand(request, UserAccountId.of(1L));

        assertThat(command.getRecentCallsSize()).isEqualTo(20);
    }

    @Test
    void test_defaultsTrendWindowToSevenDays_when_trendDaysMissing() {
        GetFrontKeyMetricsRequest request = new GetFrontKeyMetricsRequest();

        var command = converter.toGetFrontKeyMetricsCommand(request, UserAccountId.of(1L));

        assertThat(Duration.between(command.getTrendStartInclusive(), command.getTrendEndExclusive()))
                .isEqualTo(Duration.ofDays(7));
    }

    @Test
    void test_mapsCredentialIdsToDomainIds_when_credentialIdsProvided() {
        GetFrontKeyMetricsRequest request = GetFrontKeyMetricsRequest.builder()
                .credentialIds(List.of(3L, 5L))
                .build();

        var command = converter.toGetFrontKeyMetricsCommand(request, UserAccountId.of(1L));

        assertThat(command.getTrendCredentialIds())
                .containsExactly(ApiCredentialId.of(3L), ApiCredentialId.of(5L));
    }

    @Test
    void test_defaultsCredentialIdsToEmpty_when_credentialIdsMissing() {
        GetFrontKeyMetricsRequest request = new GetFrontKeyMetricsRequest();

        var command = converter.toGetFrontKeyMetricsCommand(request, UserAccountId.of(1L));

        assertThat(command.getTrendCredentialIds()).isEmpty();
    }
}
