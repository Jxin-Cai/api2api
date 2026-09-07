package com.api2api.ohs.http.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.api2api.application.dashboard.DashboardApplicationService;
import com.api2api.domain.analytics.model.AnalyticsTimeWindow;
import com.api2api.domain.analytics.model.UsageDistributionItem;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.GlobalExceptionAdvice;
import com.api2api.ohs.http.dashboard.converter.DashboardCommandConverter;
import com.api2api.ohs.http.dashboard.converter.DashboardResponseConverter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DashboardControllerTest {

    private final DashboardApplicationService dashboardApplicationService = mock(DashboardApplicationService.class);
    private final DashboardTimeWindowHelper timeWindowHelper = new DashboardTimeWindowHelper();
    private MockMvc mvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(
                        dashboardApplicationService,
                        mock(DashboardCommandConverter.class),
                        mock(DashboardResponseConverter.class),
                        new CurrentUserContextResolver(),
                        timeWindowHelper))
                .setControllerAdvice(new GlobalExceptionAdvice())
                .build();
        session = new MockHttpSession();
        session.setAttribute(CurrentUserContextResolver.CURRENT_USER_ID_SESSION_KEY, 1L);
    }

    @Test
    void test_queriesCurrentMonthWindow_when_frontDistributionsRequested() throws Exception {
        String zoneId = "Asia/Shanghai";
        when(dashboardApplicationService.getDistribution(any(), eq(UserAccountId.of(1L)), eq(true)))
                .thenReturn(List.of(new UsageDistributionItem("gpt-4", 8)));
        when(dashboardApplicationService.getDistribution(any(), eq(UserAccountId.of(1L)), eq(false)))
                .thenReturn(List.of(new UsageDistributionItem("openai", 8)));

        mvc.perform(get("/api/dashboard/distributions").session(session).param("zoneId", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.models[0].name").value("gpt-4"));

        assertCapturedWindowIsCurrentMonth(zoneId, UserAccountId.of(1L));
    }

    @Test
    void test_queriesCurrentMonthWindow_when_adminDistributionsRequestedWithTrendDays() throws Exception {
        String zoneId = "Asia/Shanghai";
        when(dashboardApplicationService.getDistribution(any(), isNull(), eq(true)))
                .thenReturn(List.of(new UsageDistributionItem("claude", 3)));
        when(dashboardApplicationService.getDistribution(any(), isNull(), eq(false)))
                .thenReturn(List.of(new UsageDistributionItem("anthropic", 3)));

        mvc.perform(get("/api/admin/dashboard/distributions")
                        .session(session)
                        .param("zoneId", zoneId)
                        .param("trendDays", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channels[0].name").value("anthropic"));

        assertCapturedWindowIsCurrentMonth(zoneId, null);
    }

    private void assertCapturedWindowIsCurrentMonth(String zoneId, UserAccountId userAccountId) {
        ArgumentCaptor<AnalyticsTimeWindow> windowCaptor = ArgumentCaptor.forClass(AnalyticsTimeWindow.class);
        if (userAccountId == null) {
            verify(dashboardApplicationService).getDistribution(windowCaptor.capture(), isNull(), eq(true));
        } else {
            verify(dashboardApplicationService).getDistribution(windowCaptor.capture(), eq(userAccountId), eq(true));
        }
        AnalyticsTimeWindow window = windowCaptor.getValue();
        assertThat(window.zoneId()).isEqualTo(zoneId);
        assertThat(window.startInclusive()).isEqualTo(timeWindowHelper.getMonthStartInclusive(zoneId));
        assertThat(window.endExclusive()).isEqualTo(timeWindowHelper.getMonthEndExclusive(zoneId));
        assertThat(window.endExclusive())
                .isNotEqualTo(timeWindowHelper.getTrendEndExclusive(zoneId));
    }
}
