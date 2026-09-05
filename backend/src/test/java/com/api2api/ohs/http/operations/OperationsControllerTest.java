package com.api2api.ohs.http.operations;

import com.api2api.application.operations.OperationsApplicationService;
import com.api2api.domain.analytics.model.ChannelLatencyRanking;
import com.api2api.domain.analytics.model.ConcurrencyTrendPoint;
import com.api2api.domain.channel.model.ProviderChannelId;
import com.api2api.domain.channel.model.ProviderChannelName;
import com.api2api.domain.user.model.UserAccountId;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.GlobalExceptionAdvice;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OperationsControllerTest {
    private final OperationsApplicationService operations = mock(OperationsApplicationService.class);
    private MockMvc mvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new OperationsController(operations, new CurrentUserContextResolver()))
                .setControllerAdvice(new GlobalExceptionAdvice()).build();
        session = new MockHttpSession();
        session.setAttribute(CurrentUserContextResolver.CURRENT_USER_ID_SESSION_KEY, 1L);
    }

    @Test
    void test_returnsUnauthorized_when_systemRequestHasNoSession() throws Exception {
        // Arrange / Act / Assert
        mvc.perform(get("/api/admin/operations/system")).andExpect(status().isUnauthorized());
    }

    @Test
    void test_returnsUnauthorized_when_trafficRequestHasNoSession() throws Exception {
        // Arrange / Act / Assert
        mvc.perform(get("/api/admin/operations/traffic")).andExpect(status().isUnauthorized());
    }

    @Test
    void test_returnsBadRequest_when_timeZoneInvalid() throws Exception {
        // Arrange / Act / Assert
        mvc.perform(get("/api/admin/operations/traffic").session(session).param("zoneId", "invalid-zone"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test_serializesConcurrencyContract_when_trafficRequested() throws Exception {
        // Arrange
        Instant now = Instant.parse("2026-09-05T10:00:00Z");
        when(operations.traffic(UserAccountId.of(1L), ZoneId.of("UTC"))).thenReturn(
                new OperationsApplicationService.TrafficSnapshot(now, "UTC",
                        List.of(new ConcurrencyTrendPoint(now.minusSeconds(300), now, 7)), List.of()));
        // Act / Assert
        mvc.perform(get("/api/admin/operations/traffic").session(session).param("zoneId", "UTC"))
                .andExpect(jsonPath("$.data.todayConcurrencyTrends[0].peakConcurrency").value(7));
    }

    @Test
    void test_serializesChannelIdentifiers_when_trafficRequested() throws Exception {
        // Arrange
        Instant now = Instant.parse("2026-09-05T10:00:00Z");
        when(operations.traffic(UserAccountId.of(1L), ZoneId.of("UTC"))).thenReturn(
                new OperationsApplicationService.TrafficSnapshot(now, "UTC", List.of(),
                        List.of(new ChannelLatencyRanking(1, ProviderChannelId.of(2L), ProviderChannelName.of("test-channel"), 500, 250, 100, 50, 3))));
        // Act / Assert
        mvc.perform(get("/api/admin/operations/traffic").session(session).param("zoneId", "UTC"))
                .andExpect(jsonPath("$.data.dailySlowestChannels[0].providerChannelId").value(2));
    }
}
