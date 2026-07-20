package com.api2api.infr.repository.credential;

import com.api2api.domain.credential.model.ModelName;
import com.api2api.domain.credential.model.ModelWhitelist;
import java.util.LinkedHashSet;
import java.util.Set;

/** Database text codec shared by credential and model-group persistence. */
public final class ModelWhitelistTextCodec {

    private ModelWhitelistTextCodec() {
    }

    public static String encode(ModelWhitelist whitelist) {
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (ModelName model : whitelist.getModels()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(model.getValue())).append('"');
            first = false;
        }
        return builder.append(']').toString();
    }

    public static ModelWhitelist decode(String text) {
        if (text == null || text.isBlank() || "[]".equals(text.trim())) {
            return ModelWhitelist.empty();
        }
        String normalized = text.trim();
        Set<ModelName> models = new LinkedHashSet<>();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            String body = normalized.substring(1, normalized.length() - 1).trim();
            if (!body.isEmpty()) {
                for (String item : body.split(",")) {
                    addModel(models, item);
                }
            }
        } else {
            for (String item : normalized.split(",")) {
                addModel(models, item);
            }
        }
        return ModelWhitelist.of(models);
    }

    private static void addModel(Set<ModelName> models, String item) {
        String value = item.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        value = value.replace("\\\"", "\"").replace("\\\\", "\\");
        if (!value.isBlank()) {
            models.add(ModelName.of(value));
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
