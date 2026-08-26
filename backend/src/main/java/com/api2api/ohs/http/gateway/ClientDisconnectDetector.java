package com.api2api.ohs.http.gateway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.IdentityHashMap;
import java.util.Locale;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * Detects that the downstream HTTP client closed the connection while the gateway was writing.
 *
 * <p>This is a transport abort on the servlet response, not an upstream provider failure.
 */
final class ClientDisconnectDetector {

    private ClientDisconnectDetector() {
    }

    static boolean isClientDisconnect(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        IdentityHashMap<Throwable, Boolean> seen = new IdentityHashMap<>();
        return walk(throwable, seen);
    }

    private static boolean walk(Throwable throwable, IdentityHashMap<Throwable, Boolean> seen) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (seen.put(current, Boolean.TRUE) != null) {
                break;
            }
            if (matches(current)) {
                return true;
            }
            if (current instanceof UncheckedIOException unchecked && walk(unchecked.getCause(), seen)) {
                return true;
            }
            for (Throwable suppressed : current.getSuppressed()) {
                if (walk(suppressed, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matches(Throwable throwable) {
        if (throwable instanceof AsyncRequestNotUsableException) {
            return true;
        }
        String className = throwable.getClass().getName();
        if (className.endsWith(".ClientAbortException")) {
            return true;
        }
        if (!(throwable instanceof IOException)) {
            return false;
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("broken pipe")
                || normalized.contains("servletoutputstream failed to flush");
    }
}
