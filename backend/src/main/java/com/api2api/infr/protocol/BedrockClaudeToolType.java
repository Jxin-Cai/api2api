package com.api2api.infr.protocol;

import java.util.Arrays;
import java.util.Optional;

enum BedrockClaudeToolType {
    CUSTOM("custom", null),
    COMPUTER_20241022("computer_20241022", "computer-use-2024-10-22"),
    COMPUTER_20250124("computer_20250124", "computer-use-2025-01-24"),
    COMPUTER_20251124("computer_20251124", "computer-use-2025-11-24"),
    BASH_20241022("bash_20241022", "computer-use-2024-10-22"),
    BASH_20250124("bash_20250124", "computer-use-2025-01-24"),
    TEXT_EDITOR_20241022("text_editor_20241022", "computer-use-2024-10-22"),
    TEXT_EDITOR_20250124("text_editor_20250124", "computer-use-2025-01-24"),
    TEXT_EDITOR_20250728("text_editor_20250728", null),
    MEMORY_20250818("memory_20250818", "context-management-2025-06-27"),
    TOOL_SEARCH_REGEX("tool_search_tool_regex", "tool-search-tool-2025-10-19");

    private final String wireValue;
    private final String requiredBeta;

    BedrockClaudeToolType(String wireValue, String requiredBeta) {
        this.wireValue = wireValue;
        this.requiredBeta = requiredBeta;
    }

    static Optional<BedrockClaudeToolType> fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.wireValue.equals(value))
                .findFirst();
    }

    Optional<String> requiredBeta() {
        return Optional.ofNullable(requiredBeta);
    }
}
