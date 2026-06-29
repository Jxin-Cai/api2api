package com.api2api.infr.protocol;

import com.api2api.domain.protocol.model.ProtocolConversionException;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * SSE 行级转换工具，按 data 行逐条转换，避免为流式响应构造完整业务对象。
 */
@Component
public class SseEventTransformer {

    public String transform(String body, Function<String, String> dataTransformer) {
        Objects.requireNonNull(body, "body must not be null");
        Objects.requireNonNull(dataTransformer, "dataTransformer must not be null");
        StringBuilder result = new StringBuilder(body.length());
        String[] lines = body.split("\\R", -1);
        for (String line : lines) {
            if (line.startsWith("data:")) {
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    result.append(line);
                } else {
                    try {
                        result.append("data: ").append(dataTransformer.apply(data));
                    } catch (ProtocolConversionException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        throw new ProtocolConversionException("failed to transform SSE data event", e);
                    }
                }
            } else {
                result.append(line);
            }
            result.append('\n');
        }
        return result.toString();
    }
}
