package com.api2api.infr.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

class ProbeVerdictTest {

    @Test
    void test_maps_warning_string_when_passed_is_textual() {
        assertThat(ProbeVerdict.of(TextNode.valueOf("warning"))).isEqualTo(ProbeVerdict.WARNING);
        assertThat(ProbeVerdict.of(BooleanNode.TRUE)).isEqualTo(ProbeVerdict.PASSED);
        assertThat(ProbeVerdict.of(BooleanNode.FALSE)).isEqualTo(ProbeVerdict.FAILED);
        assertThat(ProbeVerdict.of(null)).isEqualTo(ProbeVerdict.UNKNOWN);
    }
}
