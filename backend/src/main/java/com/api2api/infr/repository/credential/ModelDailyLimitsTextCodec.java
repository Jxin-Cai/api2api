package com.api2api.infr.repository.credential;

import com.api2api.domain.credential.model.ModelDailyLimits;
import com.api2api.domain.credential.model.ModelName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/** Database text codec for the {@code model_groups.model_daily_limits} JSON object column. */
public final class ModelDailyLimitsTextCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Long>> LIMITS_TYPE = new TypeReference<>() {
    };

    private ModelDailyLimitsTextCodec() {
    }

    public static String encode(ModelDailyLimits limits) {
        Map<String, Long> raw = new LinkedHashMap<>();
        limits.limits().forEach((model, limit) -> raw.put(model.getValue(), limit));
        try {
            return OBJECT_MAPPER.writeValueAsString(raw);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to encode model daily limits", exception);
        }
    }

    public static ModelDailyLimits decode(String text) {
        if (text == null || text.isBlank()) {
            return ModelDailyLimits.empty();
        }
        try {
            Map<String, Long> raw = OBJECT_MAPPER.readValue(text, LIMITS_TYPE);
            Map<ModelName, Long> limits = new LinkedHashMap<>();
            raw.forEach((model, limit) -> {
                if (limit != null && limit > 0) {
                    limits.put(ModelName.of(model), limit);
                }
            });
            return ModelDailyLimits.of(limits);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to decode model daily limits", exception);
        }
    }
}
