package com.api2api.application.gateway;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Everything the gateway accepted verbatim from the client besides the request body.
 *
 * <p>The gateway receives inbound requests in full and defers every decision about what to drop or
 * rewrite to the upstream call policy, so no receiving component needs to know provider specifics.</p>
 */
public record InboundRequestContext(
        Map<String, List<String>> headers,
        String rawQuery,
        ProtocolOperation operation,
        String clientIp
) {

    public InboundRequestContext {
        headers = copyHeaders(headers);
        rawQuery = normalizeQuery(rawQuery);
        operation = operation == null ? ProtocolOperation.INVOKE : operation;
        clientIp = clientIp == null || clientIp.isBlank() ? null : clientIp.trim();
    }

    public static InboundRequestContext empty() {
        return new InboundRequestContext(Map.of(), null, ProtocolOperation.INVOKE, null);
    }

    public static InboundRequestContext of(
            Map<String, List<String>> headers, String rawQuery, ProtocolOperation operation, String clientIp
    ) {
        return new InboundRequestContext(headers, rawQuery, operation, clientIp);
    }

    public static InboundRequestContext ofHeaders(Map<String, List<String>> headers) {
        return new InboundRequestContext(headers, null, ProtocolOperation.INVOKE, null);
    }

    public String clientIp() { return clientIp; }

    public boolean hasRawQuery() {
        return rawQuery != null;
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null) {
            return null;
        }
        String trimmed = rawQuery.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().trim(),
                        entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left
                ));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof InboundRequestContext that
                && Objects.equals(headers, that.headers)
                && Objects.equals(rawQuery, that.rawQuery)
                && operation == that.operation
                && Objects.equals(clientIp, that.clientIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, rawQuery, operation, clientIp);
    }
}
