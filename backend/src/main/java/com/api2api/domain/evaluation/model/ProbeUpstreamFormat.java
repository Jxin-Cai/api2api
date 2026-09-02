package com.api2api.domain.evaluation.model;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Locale;
import java.util.Optional;

/**
 * Wire format the probe service should speak when calling the channel under evaluation.
 */
public enum ProbeUpstreamFormat {

    OPENAI("openai"),
    ANTHROPIC("anthropic");

    private final String wireValue;

    ProbeUpstreamFormat(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static ProbeUpstreamFormat fromProtocol(ProtocolType protocol) {
        return switch (protocol) {
            case CLAUDE_MESSAGES, AWS_BEDROCK_CLAUDE_MESSAGES -> ANTHROPIC;
            case OPENAI_CHAT_COMPLETIONS, OPENAI_RESPONSES, OPENAI_IMAGES -> OPENAI;
        };
    }

    public static Optional<ProbeUpstreamFormat> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
