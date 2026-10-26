package com.api2api.infr.protocol;

import java.util.UUID;

/** Generates identities for synthesized Claude messages, never for individual stream chunks. */
final class ClaudeMessageIds {

    private ClaudeMessageIds() {
    }

    static String create() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "");
    }

    static String preserveOrCreate(String upstreamId) {
        return upstreamId == null || upstreamId.isBlank() ? create() : upstreamId;
    }
}
