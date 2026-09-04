package com.api2api.application.gateway;

import com.api2api.domain.channel.model.ProtocolType;
import java.util.Optional;

/**
 * Protocol-level operation requested by the client.
 *
 * <p>A protocol may expose several endpoints (Claude Messages has {@code /count_tokens}, OpenAI
 * Images has {@code /edits} and {@code /variations}). Modelling them as operations keeps auxiliary
 * endpoints on the same routing, authentication and quota pipeline as invocations while letting the
 * upstream client pick the matching provider path and body encoding.</p>
 */
public enum ProtocolOperation {

    /** The protocol's primary endpoint, available on every protocol. */
    INVOKE(null, true, true, false),
    COUNT_TOKENS(ProtocolType.CLAUDE_MESSAGES, false, false, false),
    IMAGE_EDITS(ProtocolType.OPENAI_IMAGES, true, true, true),
    IMAGE_VARIATIONS(ProtocolType.OPENAI_IMAGES, true, false, true);

    private final ProtocolType owningProtocol;
    private final boolean billable;
    private final boolean supportsStreaming;
    private final boolean multipartForm;

    ProtocolOperation(
            ProtocolType owningProtocol,
            boolean billable,
            boolean supportsStreaming,
            boolean multipartForm
    ) {
        this.owningProtocol = owningProtocol;
        this.billable = billable;
        this.supportsStreaming = supportsStreaming;
        this.multipartForm = multipartForm;
    }

    /**
     * The protocol that defines this operation; empty for {@link #INVOKE}, which every protocol has.
     */
    public Optional<ProtocolType> owningProtocol() {
        return Optional.ofNullable(owningProtocol);
    }

    public boolean availableOn(ProtocolType protocolType) {
        return owningProtocol == null || owningProtocol == protocolType;
    }

    /**
     * Token counting and other auxiliary endpoints are not model completions. Reserving quota for
     * them would let a client that probes context size starve concurrent real invocations.
     */
    public boolean billable() {
        return billable;
    }

    public boolean supportsStreaming() {
        return supportsStreaming;
    }

    /**
     * Auxiliary endpoints exist only on the protocol that defined them. Routing them through a
     * converter would POST a transformed body to a path the target protocol does not implement.
     */
    public boolean requiresNativeProtocol() {
        return this != INVOKE;
    }

    /**
     * Operations that the client submits as {@code multipart/form-data} rather than JSON. The gateway
     * carries their form as an encoded JSON envelope so the JSON-centric pipeline (model lookup,
     * model mapping, passthrough) stays unchanged, and re-encodes the form for the provider.
     */
    public boolean acceptsMultipartForm() {
        return multipartForm;
    }
}
