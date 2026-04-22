package io.github.hanielcota.commandframework.core.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight metrics abstraction for command dispatch observability.
 *
 * <p>Implementations may bridge to Micrometer, Prometheus, or platform-specific
 * metric registries. The framework records counters and timers automatically
 * when a non-noop implementation is supplied to the {@code CommandDispatcher}.</p>
 */
public interface CommandMetrics {

    /**
     * Increments a counter by one.
     *
     * @param name the metric name
     * @param tags key-value pairs; never {@code null}
     */
    void increment(String name, Map<String, String> tags);

    /**
     * Records a timer value.
     *
     * @param name     the metric name
     * @param tags     key-value pairs; never {@code null}
     * @param duration the elapsed duration
     */
    void record(String name, Map<String, String> tags, Duration duration);

    /** Returns a metrics instance that discards all recordings. */
    static CommandMetrics noop() {
        return new CommandMetrics() {
            @Override
            public void increment(String name, Map<String, String> tags) {}

            @Override
            public void record(String name, Map<String, String> tags, Duration duration) {}
        };
    }

    /** Creates an immutable tag map from alternating key-value strings. */
    static Map<String, String> tags(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid tags: expected even number of key-values");
        }
        var builder = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            builder.put(Objects.requireNonNull(keyValues[i], "tag key"), Objects.requireNonNull(keyValues[i + 1], "tag value"));
        }
        return Map.copyOf(builder);
    }
}
