package com.api2api.ohs.http.dashboard.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.dashboard.DashboardTimeWindowHelper;
import com.api2api.ohs.http.dashboard.dto.GetFrontDashboardRequest;
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
}
