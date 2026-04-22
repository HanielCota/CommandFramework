package io.github.hanielcota.commandframework.core.config;

import io.github.hanielcota.commandframework.core.CommandRoute;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Applies {@link CommandConfiguration} overrides to a {@link CommandRoute}.
 */
public final class ConfigurationOverlay {

    private final CommandConfiguration config;

    public ConfigurationOverlay(CommandConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Returns a new route with configuration overrides applied.
     *
     * @param route the original route; never {@code null}
     * @return the possibly overridden route
     */
    public CommandRoute apply(CommandRoute route) {
        CommandRoute checkedRoute = Objects.requireNonNull(route, "route");
        String key = checkedRoute.canonicalPath();
        CommandRoute.Builder builder = CommandRoute.builder(checkedRoute.root(), checkedRoute.executor())
                .aliases(checkedRoute.aliases())
                .path(checkedRoute.path())
                .permission(checkedRoute.permission())
                .senderRequirement(checkedRoute.senderRequirement())
                .cooldown(checkedRoute.cooldown())
                .description(checkedRoute.description())
                .syntax(checkedRoute.syntax())
                .async(checkedRoute.async())
                .parameters(checkedRoute.parameters());
        checkedRoute.interceptors().forEach(builder::interceptor);

        config.string(key, "permission").ifPresent(builder::permission);
        config.duration(key, "cooldown").ifPresent(builder::cooldown);
        config.stringList(key, "aliases").ifPresent(list -> builder.aliases(new LinkedHashSet<>(list)));
        config.string(key, "description").ifPresent(builder::description);
        config.string(key, "syntax").ifPresent(builder::syntax);
        config.bool(key, "async").ifPresent(builder::async);

        return builder.build();
    }
}
