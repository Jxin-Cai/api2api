package com.api2api.application.gateway;

/**
 * Converts between a decomposed multipart form and the JSON envelope the gateway pipeline carries as
 * {@code requestBody} for operations that {@link ProtocolOperation#acceptsMultipartForm()}.
 *
 * <p>The envelope keeps text fields as top-level members so the protocol contract can read
 * {@code model} and {@code stream} and route model mapping can rewrite {@code model} exactly as for
 * JSON requests.</p>
 */
public interface MultipartFormPayloadCodec {

    String encode(MultipartFormPayload payload);

    MultipartFormPayload decode(String envelope);
}
