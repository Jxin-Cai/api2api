package com.api2api.domain.protocolcontract.model;

import java.util.Objects;

/** Protocol-neutral request facts extracted by an executable protocol contract. */
public record ParsedGatewayRequest(
        String rawBody,
        String model,
        boolean streaming,
        boolean toolCallingRequired,
        boolean reasoningRequired
) {

    public ParsedGatewayRequest {
        rawBody = requireText(rawBody, "rawBody");
        model = requireText(model, "model");
    }

    private static String requireText(String value, String fieldName) {
        String text = Objects.requireNonNull(value, fieldName + " must not be null");
        if (text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text;
    }
}
