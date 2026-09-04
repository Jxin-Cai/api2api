package com.api2api.ohs.http.gateway;

import com.api2api.application.gateway.MultipartFormPayload;
import com.api2api.application.gateway.MultipartFormPayload.FilePart;
import com.api2api.application.gateway.MultipartFormPayload.TextField;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Reads a {@code multipart/form-data} gateway request into a {@link MultipartFormPayload}.
 *
 * <p>Parts are read through the Servlet API rather than {@code getParameterMap()} so that query
 * string parameters are not mistaken for form fields and part order is preserved.</p>
 */
@Component
public class MultipartFormRequestReader {

    public MultipartFormPayload read(HttpServletRequest request) {
        if (!isMultipartForm(request.getContentType())) {
            throw new IllegalArgumentException("Request body must be multipart/form-data");
        }
        Collection<Part> parts = partsOf(request);
        List<TextField> fields = new ArrayList<>();
        List<FilePart> files = new ArrayList<>();
        for (Part part : parts) {
            if (part.getSubmittedFileName() == null) {
                fields.add(new TextField(part.getName(), new String(bytesOf(part), charsetOf(part))));
            } else {
                files.add(new FilePart(part.getName(), part.getSubmittedFileName(), part.getContentType(), bytesOf(part)));
            }
        }
        return new MultipartFormPayload(fields, files);
    }

    private static boolean isMultipartForm(String contentType) {
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private static Collection<Part> partsOf(HttpServletRequest request) {
        try {
            return request.getParts();
        } catch (IOException | ServletException exception) {
            throw new IllegalArgumentException("Malformed multipart/form-data request body", exception);
        }
    }

    private static byte[] bytesOf(Part part) {
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read multipart part " + part.getName(), exception);
        }
    }

    private static Charset charsetOf(Part part) {
        String contentType = part.getContentType();
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            Charset charset = MediaType.parseMediaType(contentType).getCharset();
            return charset == null ? StandardCharsets.UTF_8 : charset;
        } catch (IllegalArgumentException invalidMediaTypeOrCharset) {
            return StandardCharsets.UTF_8;
        }
    }
}
