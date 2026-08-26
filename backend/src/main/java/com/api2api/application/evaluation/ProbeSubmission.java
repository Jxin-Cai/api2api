package com.api2api.application.evaluation;

import com.api2api.domain.channel.model.ModelName;
import com.api2api.domain.channel.model.ProviderHost;
import com.api2api.domain.channel.model.ProviderKeyRef;
import com.api2api.domain.evaluation.model.ProbeUpstreamFormat;
import java.util.Objects;
import lombok.Builder;

/**
 * Everything the probe service needs to evaluate one channel model.
 *
 * @param host           channel base URL the probe service should call
 * @param keyRef         plaintext channel API key
 * @param modelId        model id as the channel exposes it
 * @param upstreamFormat wire format the probe service should speak
 */
@Builder
public record ProbeSubmission(
        ProviderHost host,
        ProviderKeyRef keyRef,
        ModelName modelId,
        ProbeUpstreamFormat upstreamFormat
) {

    public ProbeSubmission {
        Objects.requireNonNull(host, "Provider host must not be null");
        Objects.requireNonNull(keyRef, "Provider key reference must not be null");
        Objects.requireNonNull(modelId, "Model id must not be null");
        Objects.requireNonNull(upstreamFormat, "Probe upstream format must not be null");
    }
}
