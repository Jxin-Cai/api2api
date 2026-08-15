package com.api2api.infr.protocol;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "api2api.protocol")
class ProtocolConversionProperties {

    private List<String> reasoningModelPrefixes = List.of("gpt-5", "o1", "o3", "o4");
    private List<String> reasoningModelContains = List.of("codex");
}
