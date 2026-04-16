package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.CommandFrameworkBuilder;

import java.util.Objects;

/**
 * Velocity entry point for the command framework.
 */
public final class VelocityCommandFramework

        extends CommandFrameworkBuilder<CommandSource, VelocityCommandFramework> {

    private VelocityCommandFramework(ProxyServer server, Object plugin) {
        super(new VelocityPlatformBridge(
                Objects.requireNonNull(server, "server"),
                Objects.requireNonNull(plugin, "plugin")
        ));
        this.bind(ProxyServer.class, server);
        this.bindPluginInstance(plugin);
    }

    /**
     * Creates a new Velocity builder.
     *
     * @param server the proxy server
     * @param plugin the owning plugin instance
     * @return the Velocity builder
     */
    public static VelocityCommandFramework velocity(ProxyServer server, Object plugin) {
        return new VelocityCommandFramework(server, plugin);
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
