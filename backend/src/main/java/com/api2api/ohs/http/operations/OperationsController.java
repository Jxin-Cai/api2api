package com.api2api.ohs.http.operations;

import com.api2api.application.operations.OperationsApplicationService;
import com.api2api.infr.monitoring.SystemSnapshot;
import com.api2api.ohs.http.ApiResponse;
import com.api2api.ohs.http.CurrentUserContextResolver;
import com.api2api.ohs.http.dashboard.dto.ChannelLatencyRankingResponse;
import com.api2api.ohs.http.dashboard.dto.ConcurrencyTrendPointResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OperationsController {
    private final OperationsApplicationService operations;
    private final CurrentUserContextResolver currentUser;

    public record TrafficResponse(Instant sampledAt, String zoneId,
            List<ConcurrencyTrendPointResponse> todayConcurrencyTrends,
            List<ChannelLatencyRankingResponse> dailySlowestChannels) { }

    @GetMapping("/api/admin/operations/system")
    public ApiResponse<SystemSnapshot> system(HttpServletRequest request) {
        return ApiResponse.success(operations.system(currentUser.resolveOperatorUserId(request)));
    }

    @GetMapping("/api/admin/operations/traffic")
    public ApiResponse<TrafficResponse> traffic(HttpServletRequest request,
            @RequestParam(defaultValue = "Asia/Shanghai") String zoneId) {
        var operator = currentUser.resolveOperatorUserId(request);
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneId);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid monitoring time zone", exception);
        }
        var data = operations.traffic(operator, zone);
        return ApiResponse.success(new TrafficResponse(data.sampledAt(), data.zoneId(),
                data.todayConcurrencyTrends().stream().map(point -> new ConcurrencyTrendPointResponse(point.bucketStart(), point.bucketEnd(), point.peakConcurrency())).toList(),
                data.dailySlowestChannels().stream().map(item -> ChannelLatencyRankingResponse.builder()
                        .rank(item.rank()).providerChannelId(item.providerChannelId().value())
                        .providerChannelName(item.providerChannelName().value())
                        .maxDurationMillis(item.maxDurationMillis()).avgDurationMillis(item.avgDurationMillis())
                        .maxFirstTokenMillis(item.maxFirstTokenMillis()).avgFirstTokenMillis(item.avgFirstTokenMillis())
                        .requestCount(item.requestCount()).build()).toList()));
    }
}
