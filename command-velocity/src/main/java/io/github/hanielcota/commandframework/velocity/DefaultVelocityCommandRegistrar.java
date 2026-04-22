package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import java.util.Objects;

final class DefaultVelocityCommandRegistrar implements VelocityCommandRegistrar {

    @Override
    public void register(ProxyServer server, Object plugin, CommandRoot root, CommandDispatcher dispatcher) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        CommandManager velocityCommands = server.getCommandManager();
        String[] aliases = root.aliases().toArray(String[]::new);
        CommandMeta meta = velocityCommands.metaBuilder(root.label()).aliases(aliases).plugin(plugin).build();
        velocityCommands.register(meta, new VelocityRawCommandBridge(dispatcher));
    }
}
