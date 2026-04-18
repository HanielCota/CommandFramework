package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandFrameworkBuilder;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Velocity entry point for the command framework.
 */
public final class VelocityCommandFramework

        extends CommandFrameworkBuilder<CommandSource, VelocityCommandFramework> {

    private VelocityCommandFramework(
            ProxyServer server,
            Object plugin,
            BiPredicate<CommandActor, Player> playerSuggestFilter
    ) {
        super(new VelocityPlatformBridge(
                Objects.requireNonNull(server, "server"),
                Objects.requireNonNull(plugin, "plugin"),
                Objects.requireNonNull(playerSuggestFilter, "playerSuggestFilter")
        ));
        this.bind(ProxyServer.class, server);
        this.bindPluginInstance(plugin);
    }

    /**
     * Creates a new Velocity builder with no player-visibility filter - every online player is
     * suggested during tab-completion.
     *
     * @param server the proxy server
     * @param plugin the owning plugin instance
     * @return the Velocity builder
     */
    public static VelocityCommandFramework velocity(ProxyServer server, Object plugin) {
        return new VelocityCommandFramework(server, plugin, (actor, target) -> true);
    }

    /**
     * Creates a new Velocity builder with a custom player-visibility filter applied to player
     * argument suggestions. Velocity has no native vanish/canSee API, so this hook lets plugins
     * that integrate with proxy-level vanish or permission systems hide specific players from
     * tab-completion.
     *
     * @param server the proxy server
     * @param plugin the owning plugin instance
     * @param playerSuggestFilter predicate that returns {@code true} when {@code target} should be
     *                            suggested to {@code actor}; evaluated for each online player
     * @return the Velocity builder
     */
    public static VelocityCommandFramework velocity(
            ProxyServer server,
            Object plugin,
            BiPredicate<CommandActor, Player> playerSuggestFilter
    ) {
        return new VelocityCommandFramework(server, plugin, playerSuggestFilter);
    }

    @Override
    protected VelocityCommandFramework self() {
        return this;
    }

    @SuppressWarnings("unchecked")
    private void bindPluginInstance(Object plugin) {
        this.bind((Class<Object>) plugin.getClass(), plugin);
    }
}
