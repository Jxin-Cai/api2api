package com.api2api.domain.gateway.model;

import com.api2api.domain.protocol.model.ConversionResult;
import java.util.Objects;

/**
 * Immutable trace of request and response protocol conversions for one gateway invocation.
 */
public final class ConversionTrace {

    private final ConversionResult requestConversion;
    private final ConversionResult responseConversion;

    private ConversionTrace(ConversionResult requestConversion, ConversionResult responseConversion) {
        if (requestConversion == null && responseConversion == null) {
            throw new IllegalArgumentException("At least one conversion result must be recorded");
        }
        this.requestConversion = requestConversion;
        this.responseConversion = responseConversion;
    }

    public static ConversionTrace withRequestConversion(ConversionResult result) {
        return new ConversionTrace(Objects.requireNonNull(result, "Request conversion result must not be null"), null);
    }

    public static ConversionTrace withResponseConversion(ConversionResult result) {
        return new ConversionTrace(null, Objects.requireNonNull(result, "Response conversion result must not be null"));
    }

    public ConversionTrace withRequestConversionRecorded(ConversionResult result) {
        return new ConversionTrace(Objects.requireNonNull(result, "Request conversion result must not be null"), responseConversion);
    }

    public ConversionTrace withResponseConversionRecorded(ConversionResult result) {
        return new ConversionTrace(requestConversion, Objects.requireNonNull(result, "Response conversion result must not be null"));
    }

    public ConversionResult requestConversion() {
        return requestConversion;
    }

    public ConversionResult responseConversion() {
        return responseConversion;
    }

    public boolean passthrough() {
        boolean requestPassthrough = requestConversion == null || requestConversion.passthrough();
        boolean responsePassthrough = responseConversion == null || responseConversion.passthrough();
        return requestPassthrough && responsePassthrough;
    }

    public ConversionResult getRequestConversion() {
        return requestConversion;
    }

    public ConversionResult getResponseConversion() {
        return responseConversion;
    }

    public boolean isPassthrough() {
        return passthrough();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversionTrace that)) {
            return false;
        }
        return Objects.equals(requestConversion, that.requestConversion)
                && Objects.equals(responseConversion, that.responseConversion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestConversion, responseConversion);
    }
}
