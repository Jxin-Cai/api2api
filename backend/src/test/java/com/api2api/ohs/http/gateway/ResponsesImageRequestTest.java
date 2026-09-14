package com.api2api.ohs.http.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ResponsesImageRequestTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void test_returns_null_when_request_has_no_tools() throws Exception {
        ObjectNode body = mapper.createObjectNode().put("model", "gpt-5.6-sol").put("input", "Hello");

        ResponsesImageRequest request = ResponsesImageRequest.parse(mapper, body.toString(), "gpt-image-2.5");

        assertThat(request).isNull();
    }

    @Test
    void test_injects_hosted_tool_when_agent_tools_omit_image_generation() throws Exception {
        ObjectNode body = mapper.createObjectNode().put("model", "gpt-5.6-sol").put("input", "Draw a cat");
        body.putArray("tools").addObject().put("type", "function").put("name", "shell");

        ResponsesImageRequest request = ResponsesImageRequest.parse(mapper, body.toString(), "gpt-image-2.5");

        assertThat(request.body().at("/tools/1/type").asText()).isEqualTo("image_generation");
    }

    @Test
    void test_sends_function_instead_of_hosted_tool_when_planning() throws Exception {
        ObjectNode body = mapper.createObjectNode().put("model", "gpt-5.6-sol").put("input", "Draw a cat");
        body.putArray("tools").addObject().put("type", "image_generation");
        ResponsesImageRequest request = ResponsesImageRequest.parse(mapper, body.toString(), "gpt-image-2.5");

        ObjectNode planned = request.plannerRequest(mapper);

        assertThat(planned.at("/tools/0/name").asText()).isEqualTo("api2api_generate_image");
    }

    @Test
    void test_treats_blank_hosted_call_as_image_call() throws Exception {
        ObjectNode body = mapper.createObjectNode().put("model", "gpt-5.6-sol").put("input", "Draw a cat");
        body.putArray("tools").addObject().put("type", "image_generation");
        ResponsesImageRequest request = ResponsesImageRequest.parse(mapper, body.toString(), "gpt-image-2.5");
        ObjectNode item = mapper.createObjectNode().put("type", "image_generation_call").put("status", "in_progress");

        boolean imageCall = request.isImageCall(item);

        assertThat(imageCall).isTrue();
    }

    @Test
    void test_treats_completed_hosted_call_as_image_call_to_bridge() throws Exception {
        ObjectNode body = mapper.createObjectNode().put("model", "gpt-5.6-sol").put("input", "Draw a cat");
        body.putArray("tools").addObject().put("type", "image_generation");
        ResponsesImageRequest request = ResponsesImageRequest.parse(mapper, body.toString(), "gpt-image-2.5");
        ObjectNode item = mapper.createObjectNode().put("type", "image_generation_call")
                .put("status", "completed").put("result", "aW1hZ2U=");

        boolean imageCall = request.isImageCall(item);

        assertThat(imageCall).isTrue();
    }
}
