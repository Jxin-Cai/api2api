package com.api2api.application.gateway;

import java.util.List;
import java.util.Objects;

/**
 * A {@code multipart/form-data} request body decomposed into ordered text fields and file parts.
 *
 * <p>Part order is preserved because providers may treat repeated names such as {@code image[]} as a
 * positional list.</p>
 */
public record MultipartFormPayload(List<TextField> fields, List<FilePart> files) {

    public MultipartFormPayload {
        fields = List.copyOf(Objects.requireNonNull(fields, "fields must not be null"));
        files = List.copyOf(Objects.requireNonNull(files, "files must not be null"));
    }

    public boolean hasPart(String name) {
        return fields.stream().anyMatch(field -> field.name().equals(name))
                || files.stream().anyMatch(file -> file.name().equals(name));
    }

    public record TextField(String name, String value) {

        public TextField {
            name = requireName(name);
            value = Objects.requireNonNull(value, "field value must not be null");
        }
    }

    public record FilePart(String name, String filename, String contentType, byte[] content) {

        public FilePart {
            name = requireName(name);
            content = Objects.requireNonNull(content, "file content must not be null").clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }

        public int size() {
            return content.length;
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("multipart part name must not be blank");
        }
        return name;
    }
}
