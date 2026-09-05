package com.api2api.infr.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ClaudeResponsesMediaMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void test_appendsMediaExtension_when_documentTitleHasNoExtension() {
        // Arrange
        ObjectNode block = objectMapper.createObjectNode();
        block.put("title", "quarterly report");

        // Act
        String filename = ClaudeResponsesMediaMapper.filenameFor(block, "application/pdf");

        // Assert
        assertThat(filename).isEqualTo("quarterly report.pdf");
    }

    @Test
    void test_keepsTitleExtension_when_documentTitleAlreadyHasOne() {
        // Arrange
        ObjectNode block = objectMapper.createObjectNode();
        block.put("title", "notes.txt");

        // Act
        String filename = ClaudeResponsesMediaMapper.filenameFor(block, "text/plain");

        // Assert
        assertThat(filename).isEqualTo("notes.txt");
    }

    @Test
    void test_mapsDocxMediaType_when_filenameExtensionIsKnown() {
        // Arrange
        String filename = "brief.docx";

        // Act
        String mediaType = ClaudeResponsesMediaMapper.mediaTypeFromFilename(filename, "application/pdf");

        // Assert
        assertThat(mediaType)
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
