package com.api2api.infr.client.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ProbeReportCompactorTest {

    @Test
    void test_drops_full_responses_when_compacting_probe_items() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        EvaluationProbeProperties properties = new EvaluationProbeProperties();
        ProbeReportCompactor compactor = new ProbeReportCompactor(objectMapper, properties);
        ObjectNode report = objectMapper.createObjectNode();
        report.put("runId", "run-1");
        report.put("modelId", "gpt-4o");
        ObjectNode item = report.putArray("items").addObject();
        item.put("probeId", "zh_reasoning");
        item.put("passed", true);
        item.put("response", "very long model output");
        item.put("passReason", "ok");

        String compacted = compactor.compact(report);

        assertThat(compacted).contains("zh_reasoning");
        assertThat(compacted).contains("\"passed\":\"passed\"");
        assertThat(compacted).doesNotContain("very long model output");
    }
}
