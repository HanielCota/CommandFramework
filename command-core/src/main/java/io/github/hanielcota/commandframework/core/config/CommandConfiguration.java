package io.github.hanielcota.commandframework.core.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Read-only view of external configuration that can override annotation defaults.
 *
 * <p>Implementations may load from YAML, JSON, HOCON, or properties files.
 * The framework uses this to apply runtime overrides to {@code CommandRoute}
 * settings such as cooldown, permission, and aliases.</p>
 */
public interface CommandConfiguration {

    /**
     * Reads a string property for the given route key.
     *
     * @param routeKey the canonical path of the route (e.g. "kit give")
     * @param property the property name (e.g. "permission")
     * @return the configured value if present; never {@code null}
     */
    @NonNull Optional<String> string(String routeKey, String property);

    /**
     * Reads a duration property for the given route key.
     *
     * @param routeKey the canonical path of the route
     * @param property the property name (e.g. "cooldown")
     * @return the configured duration if present; never {@code null}
     */
    @NonNull Optional<Duration> duration(String routeKey, String property);

    /**
     * Reads a string list property for the given route key.
     *
     * @param routeKey the canonical path of the route
     * @param property the property name (e.g. "aliases")
     * @return the configured list if present; never {@code null}
     */
    @NonNull Optional<List<String>> stringList(String routeKey, String property);

    /**
     * Reads a boolean property for the given route key.
     *
     * @param routeKey the canonical path of the route
     * @param property the property name (e.g. "async")
     * @return the configured boolean if present; never {@code null}
     */
    @NonNull Optional<Boolean> bool(String routeKey, String property);

    /** Returns a configuration that yields empty for every query. */
    static CommandConfiguration empty() {
        return new CommandConfiguration() {
            @Override
            public Optional<String> string(String routeKey, String property) {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> duration(String routeKey, String property) {
                return Optional.empty();
            }

            @Override
            public Optional<List<String>> stringList(String routeKey, String property) {
                return Optional.empty();
            }

            @Override
            public Optional<Boolean> bool(String routeKey, String property) {
                return Optional.empty();
            }
        };
    }
}
