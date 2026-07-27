package com.api2api.infr.client.provider;

import com.api2api.domain.routing.model.RouteFailureType;

/**
 * Shared utility for classifying upstream HTTP failures and formatting error messages.
 */
public final class UpstreamFailureClassifier {

    static final int MAX_BODY_PREVIEW_LENGTH = 500;

    private UpstreamFailureClassifier() {}

    public static RouteFailureType fromHttpStatus(int statusCode) {
        return RouteFailureType.fromHttpStatus(statusCode);
    }

    public static boolean isModelUnavailable(int statusCode, String responseBody) {
        return RouteFailureType.isModelUnavailableResponse(statusCode, responseBody);
    }

    public static String compactErrorMessage(String prefix, int statusCode, String responseBody) {
        String message = prefix + " returned HTTP " + statusCode;
        if (responseBody == null || responseBody.isBlank()) {
            return message;
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        if (compact.length() > MAX_BODY_PREVIEW_LENGTH) {
            compact = compact.substring(0, MAX_BODY_PREVIEW_LENGTH) + "...";
        }
        return message + ": " + compact;
    }
}
