package com.api2api.infr.client.provider;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.FilePart;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;

/**
 * Serialises a {@link MultipartFormPayload} as an RFC 7578 {@code multipart/form-data} body.
 *
 * <p>The gateway generates its own boundary rather than reusing the client's: the form was
 * re-assembled from parsed parts (model mapping may have rewritten a field), so the original framing
 * no longer applies.</p>
 */
final class MultipartFormBodyWriter {

    private static final String CRLF = "\r\n";
    private static final String BOUNDARY_PREFIX = "api2api-";
    private static final String DEFAULT_FILE_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private MultipartFormBodyWriter() {
    }

    static UpstreamRequestBody write(MultipartFormPayload payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        String boundary = BOUNDARY_PREFIX + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (TextField field : payload.fields()) {
            writeAscii(out, "--" + boundary + CRLF);
            writeAscii(out, "Content-Disposition: form-data; name=\"" + quote(field.name()) + "\"" + CRLF);
            writeAscii(out, CRLF);
            out.writeBytes(field.value().getBytes(StandardCharsets.UTF_8));
            writeAscii(out, CRLF);
        }
        for (FilePart file : payload.files()) {
            writeAscii(out, "--" + boundary + CRLF);
            writeAscii(out, "Content-Disposition: form-data; name=\"" + quote(file.name()) + "\""
                    + "; filename=\"" + quote(filenameOf(file)) + "\"" + CRLF);
            writeAscii(out, "Content-Type: " + contentTypeOf(file) + CRLF);
            writeAscii(out, CRLF);
            out.writeBytes(file.content());
            writeAscii(out, CRLF);
        }
        writeAscii(out, "--" + boundary + "--" + CRLF);
        return new UpstreamRequestBody(out.toByteArray(), MediaType.MULTIPART_FORM_DATA_VALUE + "; boundary=" + boundary);
    }

    private static String filenameOf(FilePart file) {
        return file.filename() == null || file.filename().isBlank() ? file.name() : file.filename();
    }

    private static String contentTypeOf(FilePart file) {
        return file.contentType() == null || file.contentType().isBlank()
                ? DEFAULT_FILE_CONTENT_TYPE
                : file.contentType();
    }

    /**
     * Percent-encodes the characters that would break the quoted-string framing of the
     * Content-Disposition header, matching what browsers send for such names.
     */
    private static String quote(String value) {
        return value.replace("\r", "%0D").replace("\n", "%0A").replace("\"", "%22");
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }
}
