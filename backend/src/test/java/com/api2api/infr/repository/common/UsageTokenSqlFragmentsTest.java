package com.api2api.infr.repository.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsageTokenSqlFragmentsTest {

    @Test
    void test_prefixes_total_token_columns_when_alias_is_provided() {
        assertThat(UsageTokenSqlFragments.totalTokensWithPrefix("r."))
                .isEqualTo(
                        "COALESCE(r.input_tokens, 0)"
                                + " + COALESCE(r.output_tokens, 0)"
                                + " + COALESCE(r.cache_creation_input_tokens, 0)"
                                + " + COALESCE(r.cache_read_input_tokens, 0)"
                );
    }
}
