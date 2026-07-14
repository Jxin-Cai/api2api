package com.api2api.infr.config;

import com.api2api.domain.analytics.service.DashboardAnalyticsService;
import com.api2api.domain.gateway.service.DefaultGatewayInvocationService;
import com.api2api.domain.gateway.service.GatewayInvocationService;
import com.api2api.domain.routing.service.DefaultRoutingPolicyService;
import com.api2api.domain.routing.service.RoutingPolicyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfiguration {

    @Bean
    public RoutingPolicyService routingPolicyService() {
        return new DefaultRoutingPolicyService();
    }

    @Bean
    public GatewayInvocationService gatewayInvocationService() {
        return new DefaultGatewayInvocationService();
    }

    @Bean
    public DashboardAnalyticsService dashboardAnalyticsService() {
        return new DashboardAnalyticsService();
    }
}
