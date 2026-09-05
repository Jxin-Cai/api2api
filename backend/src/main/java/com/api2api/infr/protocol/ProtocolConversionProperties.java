package com.api2api.infr.protocol;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "api2api.protocol")
class ProtocolConversionProperties {

    /**
     * Non-versioned reasoning model families. Versioned GPT models (gpt-5 and later)
     * are detected by {@link GptModelVersion} and do not need to be listed here.
     */
    private List<String> reasoningModelPrefixes = List.of("o1", "o3", "o4");
    private List<String> reasoningModelContains = List.of("codex");
}
