package com.api2api.application.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api2api.bootstrap-admin")
public record BootstrapAdminProperties(
        String username,
        String password
) {
}
