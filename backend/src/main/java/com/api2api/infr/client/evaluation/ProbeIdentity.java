package com.api2api.infr.client.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Which model the probe service believes actually served the requests.
 *
 * <p>The probe service reports several independent identity estimators side by side. They are read
 * in descending trustworthiness, so the strongest estimator that committed to an answer wins and the
 * rest are ignored.
 *
 * @param family         detected model family, for example {@code openai}
 * @param model          detected concrete model, for example {@code openai/gpt-5.6-luna}
 * @param confidence     estimator confidence between 0 and 1, absent when not reported
 * @param familyMismatch whether the detected family contradicts the family the channel claims
 */
public record ProbeIdentity(
        String family,
        String model,
        BigDecimal confidence,
        Boolean familyMismatch
) {

    private static final List<String> ESTIMATORS_BY_TRUST = List.of("v4", "ikp", "v3g", "v3");
    private static final int CONFIDENCE_SCALE = 4;

    /**
     * Reads the identity section of a probe report.
     *
     * @param identityAssessment {@code identityAssessment} node, possibly missing
     * @return detected identity, or empty when every estimator abstained
     */
    public static Optional<ProbeIdentity> read(JsonNode identityAssessment) {
        if (identityAssessment == null || !identityAssessment.isObject()) {
            return Optional.empty();
        }
        Boolean familyMismatch = readFamilyMismatch(identityAssessment);
        for (String estimator : ESTIMATORS_BY_TRUST) {
            JsonNode node = identityAssessment.path(estimator);
            if (!node.isObject() || node.path("abstained").asBoolean(false)) {
                continue;
            }
            Optional<ProbeIdentity> identity = readEstimator(node, familyMismatch);
            if (identity.isPresent()) {
                return identity;
            }
        }
        return Optional.empty();
    }

    private static Optional<ProbeIdentity> readEstimator(JsonNode estimator, Boolean familyMismatch) {
        JsonNode top = estimator.path("top");
        if (top.isObject() && top.hasNonNull("modelId")) {
            return Optional.of(new ProbeIdentity(
                    text(top, "family"),
                    text(top, "modelId"),
                    confidence(top.path("score")),
                    familyMismatch));
        }
        // v3g reports its winner flat instead of nesting it under "top".
        if (estimator.hasNonNull("topModel")) {
            return Optional.of(new ProbeIdentity(
                    familyOf(text(estimator, "topModel")),
                    text(estimator, "topModel"),
                    confidence(estimator.path("confidence")),
                    familyMismatch));
        }
        JsonNode firstCandidate = estimator.path("candidates").path(0);
        if (firstCandidate.isObject() && firstCandidate.hasNonNull("modelId")) {
            return Optional.of(new ProbeIdentity(
                    text(firstCandidate, "family"),
                    text(firstCandidate, "modelId"),
                    confidence(firstCandidate.path("score")),
                    familyMismatch));
        }
        return Optional.empty();
    }

    private static Boolean readFamilyMismatch(JsonNode identityAssessment) {
        JsonNode mismatch = identityAssessment.path("v3").path("familyMismatch");
        return mismatch.isBoolean() ? mismatch.asBoolean() : null;
    }

    private static String familyOf(String modelId) {
        if (modelId == null) {
            return null;
        }
        int separator = modelId.indexOf('/');
        return separator > 0 ? modelId.substring(0, separator) : null;
    }

    private static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asText() : null;
    }

    private static BigDecimal confidence(JsonNode score) {
        if (!score.isNumber()) {
            return null;
        }
        BigDecimal value = score.decimalValue();
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            return null;
        }
        return value.setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }
}
