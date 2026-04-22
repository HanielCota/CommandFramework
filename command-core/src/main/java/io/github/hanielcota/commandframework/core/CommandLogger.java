package io.github.hanielcota.commandframework.core;

import org.jspecify.annotations.Nullable;

/**
 * Lightweight logging abstraction used by the framework.
 *
 * <p>Adapters should bridge this to the platform logger (Bukkit
 * {@code java.util.logging.Logger}, Velocity {@code Logger}, SLF4J, etc.).</p>
 */
public interface CommandLogger {

    /** Logs a trace message (very verbose, usually disabled in production). */
    default void trace(String message) {
        // no-op by default so existing implementations don't break
    }

    /** Logs a debug message (useful for development). */
    default void debug(String message) {
        // no-op by default so existing implementations don't break
    }

    /** Logs a warning together with an optional stack trace. */
    void warn(String message, @Nullable Throwable throwable);

    /** Returns a logger that discards all output. */
    static CommandLogger noop() {
        return new CommandLogger() {
            @Override
            public void trace(String message) {}

            @Override
            public void debug(String message) {}

            @Override
            public void warn(String message, @Nullable Throwable throwable) {}
        };
    }
}
