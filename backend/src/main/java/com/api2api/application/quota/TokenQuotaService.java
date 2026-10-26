package com.api2api.application.quota;

import com.api2api.application.BusinessException;
import com.api2api.domain.credential.model.ApiCredential;
import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.repository.ApiCredentialRepository;
import com.api2api.domain.quota.*;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenQuotaService {
    private final TokenQuotaRepository repository;
    private final ApiCredentialRepository credentials;
    private final TokenQuotaWindow windows;

    /** Runs inside gateway admission's short transaction, before reserving in-flight usage. */
    @Transactional(propagation = Propagation.MANDATORY)
    public ApiCredential lockCredential(ApiCredential credential) {
        repository.lockCredential(credential.getId().value());
        return credentials.findById(credential.getId())
                .orElseThrow(() -> new BusinessException("API_CREDENTIAL_INVALID"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertAvailable(ApiCredential credential, ModelName model) {
        repository.lockGroup(credential.getModelGroupId().value());
        for (TokenQuotaRule rule : repository.findApplicable(credential.getModelGroupId().value(), credential.getId().value())) {
            if (!rule.appliesTo(model.value())) continue;
            var window = windows.current(rule.period());
            if (repository.consumed(rule, window).compareTo(rule.limitTokens()) >= 0) {
                throw new TokenQuotaExceededException(rule,
                        rule.period() == QuotaPeriod.LIFETIME ? null : window.endExclusive());
            }
        }
    }

    public Set<ModelName> limitedModels(ApiCredential credential) {
        Set<ModelName> limited = new LinkedHashSet<>();
        for (TokenQuotaRule rule : repository.findApplicable(credential.getModelGroupId().value(), credential.getId().value())) {
            if (!rule.enabled() || repository.consumed(rule, windows.current(rule.period())).compareTo(rule.limitTokens()) < 0) continue;
            if (rule.model().isEmpty()) limited.addAll(credential.getModelWhitelist().getModels());
            else if (credential.getModelWhitelist().getModels().contains(ModelName.of(rule.model()))) limited.add(ModelName.of(rule.model()));
        }
        return Set.copyOf(limited);
    }
}
