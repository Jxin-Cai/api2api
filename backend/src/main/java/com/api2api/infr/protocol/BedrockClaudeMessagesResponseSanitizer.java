package com.api2api.infr.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;

final class BedrockClaudeMessagesResponseSanitizer {

    private static final String BEDROCK_EXTENSION_PREFIX = "amazon-bedrock-";

    private BedrockClaudeMessagesResponseSanitizer() {
    }

    static void removeProviderExtensions(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            List<String> providerFields = new ArrayList<>();
            object.fieldNames().forEachRemaining(field -> {
                if (field.startsWith(BEDROCK_EXTENSION_PREFIX)) {
                    providerFields.add(field);
                }
            });
            providerFields.forEach(object::remove);
            object.elements().forEachRemaining(
                    BedrockClaudeMessagesResponseSanitizer::removeProviderExtensions);
            return;
        }
        if (node.isArray()) {
            node.elements().forEachRemaining(
                    BedrockClaudeMessagesResponseSanitizer::removeProviderExtensions);
        }
    }
}
