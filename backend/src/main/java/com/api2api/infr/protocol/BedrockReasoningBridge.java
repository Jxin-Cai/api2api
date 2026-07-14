package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import com.api2api.domain.protocol.model.ProtocolConversionRouteContext;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/** Preserves Bedrock-bound reasoning state without exposing it to other upstream protocols. */
final class BedrockReasoningBridge {

    static final String SIGNATURE_PREFIX = "api2api-bedrock-reasoning:v1:";

    private BedrockReasoningBridge() {
    }

    static boolean isBedrockSignature(String signature) {
        return signature != null && signature.startsWith(SIGNATURE_PREFIX);
    }

    static String encode(String signature, ProtocolConversionRouteContext routeContext) {
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Bedrock reasoning signature must not be blank");
        }
        if (routeContext == null) {
            throw new IllegalArgumentException("Bedrock reasoning route context must not be null");
        }
        byte[] model = routeContext.upstreamModel().getBytes(StandardCharsets.UTF_8);
        byte[] state = signature.getBytes(StandardCharsets.UTF_8);
        ByteBuffer payload = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + model.length + state.length);
        payload.putLong(routeContext.providerChannelId());
        payload.putInt(model.length);
        payload.put(model);
        payload.put(state);
        return SIGNATURE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
    }

    static Optional<String> decode(String signature, ProtocolConversionRouteContext expectedRoute) {
        if (!isBedrockSignature(signature) || expectedRoute == null) {
            return Optional.empty();
        }
        try {
            ByteBuffer payload = ByteBuffer.wrap(
                    Base64.getUrlDecoder().decode(signature.substring(SIGNATURE_PREFIX.length())));
            long providerChannelId = payload.getLong();
            int modelLength = payload.getInt();
            if (modelLength <= 0 || modelLength > payload.remaining()) {
                return Optional.empty();
            }
            byte[] model = new byte[modelLength];
            payload.get(model);
            byte[] state = new byte[payload.remaining()];
            payload.get(state);
            String upstreamModel = new String(model, StandardCharsets.UTF_8);
            String decoded = new String(state, StandardCharsets.UTF_8);
            if (providerChannelId != expectedRoute.providerChannelId()
                    || !upstreamModel.equals(expectedRoute.upstreamModel())
                    || decoded.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(decoded);
        } catch (IllegalArgumentException | java.nio.BufferUnderflowException exception) {
            throw new ProtocolConversionException("BEDROCK_REASONING_SIGNATURE_DECODING_FAILED", exception);
        }
    }
}
