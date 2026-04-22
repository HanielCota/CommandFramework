package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import java.util.Objects;

@FunctionalInterface
public interface VelocityCommandRegistrar {

    void register(ProxyServer server, Object plugin, CommandRoot root, CommandDispatcher dispatcher);

    default void unregister(ProxyServer server, Object plugin, CommandRoot root, CommandDispatcher dispatcher) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        server.getCommandManager().unregister(root.label());
        for (String alias : root.aliases()) {
            server.getCommandManager().unregister(alias);
        }
    }
}
