package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProtocolSupportConfiguration {

    @Bean
    ProtocolJsonSupport protocolJsonSupport(ObjectMapper objectMapper) {
        return new ProtocolJsonSupport(objectMapper);
    }
}
