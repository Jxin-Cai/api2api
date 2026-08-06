package com.api2api.infr.repository.common;

import java.util.Objects;

/**
 * Single source of truth for token cost-weighting SQL fragments.
 *
 * <p>The coefficients (output 5×, cache_read 0.1×, cache_creation 1.25×) encode the platform's
 * token pricing strategy. Any change to these coefficients must be made here and here only.
 */
public final class UsageTokenSqlFragments {

    public static final String ACTUAL_TOKENS_SQL =
            "input_tokens::numeric + output_tokens::numeric * 5"
            + " + cache_read_input_tokens::numeric * 0.1"
            + " + cache_creation_input_tokens::numeric * 1.25";

    private UsageTokenSqlFragments() {
    }

    /**
     * Returns the weighted token expression with every column prefixed by {@code prefix}.
     *
     * <p>Example: {@code withPrefix("r.")} produces
     * {@code r.input_tokens::numeric + r.output_tokens::numeric * 5 + ...}.
     *
     * @param prefix table alias followed by a dot (e.g. {@code "r."}), or blank to use bare column names
     */
    public static String withPrefix(String prefix) {
        Objects.requireNonNull(prefix, "Column prefix must not be null");
        if (prefix.isBlank()) {
            return ACTUAL_TOKENS_SQL;
        }
        return prefix + ACTUAL_TOKENS_SQL.replace(" + ", " + " + prefix);
    }
}
