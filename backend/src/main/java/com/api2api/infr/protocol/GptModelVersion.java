package com.api2api.infr.protocol;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed {@code gpt-<major>[.<minor>]} version of an OpenAI model name, used for
 * protocol capability gating.
 *
 * <p>Variant suffixes are ignored ({@code gpt-5.6-luna} → 5.6, {@code gpt-6-astra} → 6.0,
 * {@code gpt-5-codex} → 5.0). A missing minor version is treated as {@code 0}, so a new
 * major release ({@code gpt-6}) always satisfies a gate expressed against an older minor
 * release ({@code gpt-5.6}) instead of silently losing every capability.
 */
record GptModelVersion(int major, int minor) implements Comparable<GptModelVersion> {

    /** First GPT generation whose whole lineup is reasoning-only. */
    static final GptModelVersion GPT_5 = new GptModelVersion(5, 0);
    /** Introduced the Responses {@code tool_search} built-in tool. */
    static final GptModelVersion GPT_5_4 = new GptModelVersion(5, 4);
    /** Introduced persisted reasoning, programmatic tool calling and {@code max} effort. */
    static final GptModelVersion GPT_5_6 = new GptModelVersion(5, 6);

    private static final Pattern MODEL_PATTERN = Pattern.compile("^gpt-(\\d{1,9})(?:\\.(\\d{1,9}))?(?!\\d)");

    private static final Comparator<GptModelVersion> ORDER = Comparator
            .comparingInt(GptModelVersion::major)
            .thenComparingInt(GptModelVersion::minor);

    static Optional<GptModelVersion> parse(String model) {
        if (model == null) {
            return Optional.empty();
        }
        Matcher matcher = MODEL_PATTERN.matcher(model.trim().toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return Optional.empty();
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return Optional.of(new GptModelVersion(major, minor));
    }

    static boolean isAtLeast(String model, GptModelVersion required) {
        return parse(model).filter(version -> version.compareTo(required) >= 0).isPresent();
    }

    @Override
    public int compareTo(GptModelVersion other) {
        return ORDER.compare(this, other);
    }
}
