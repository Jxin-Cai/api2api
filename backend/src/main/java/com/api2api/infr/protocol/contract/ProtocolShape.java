package com.api2api.infr.protocol.contract;

import com.api2api.domain.protocolcontract.model.ProtocolContractViolationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

/** Executable structural view over a contract's field references. */
public final class ProtocolShape {

    private final ProtocolShapeKind kind;
    private final List<ProtocolFieldRef> fields;
    private final boolean enforceRequired;

    public ProtocolShape(ProtocolShapeKind kind, List<ProtocolFieldRef> fields, boolean enforceRequired) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.fields = List.copyOf(fields);
        this.enforceRequired = enforceRequired;
    }

    public JsonNode parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new ProtocolContractViolationException(kind + " payload must be a JSON object");
        }
        fields.forEach(field -> field.validate(root, kind, enforceRequired));
        return root;
    }

    public ProtocolFieldRef requireField(String path) {
        return fields.stream().filter(field -> field.path().equals(path)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + kind + " field: " + path));
    }

    public List<ProtocolFieldRef> fields() {
        return fields;
    }

    public ProtocolShapeKind kind() {
        return kind;
    }
}
