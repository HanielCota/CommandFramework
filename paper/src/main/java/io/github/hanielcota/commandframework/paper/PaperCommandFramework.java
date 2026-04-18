package io.github.hanielcota.commandframework.paper;

import io.github.hanielcota.commandframework.CommandFrameworkBuilder;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Paper entry point for the command framework.
 */
public final class PaperCommandFramework extends CommandFrameworkBuilder<CommandSender, PaperCommandFramework> {

    private PaperCommandFramework(JavaPlugin plugin) {
        super(new PaperPlatformBridge(Objects.requireNonNull(plugin, "plugin")));
        this.bind(JavaPlugin.class, plugin);
        this.bind(Server.class, plugin.getServer());
        this.bindPluginInstance(plugin);
    }

    /**
     * Creates a new Paper builder.
     *
     * @param plugin the owning plugin
     * @return the Paper builder
     */
    public static PaperCommandFramework paper(JavaPlugin plugin) {
        return new PaperCommandFramework(plugin);
    }

    @Override
    protected PaperCommandFramework self() {
        return this;
    }

    // Safe: plugin.getClass() is always a subclass of JavaPlugin, so the cast widens to Class<JavaPlugin>.
    @SuppressWarnings("unchecked")
    private void bindPluginInstance(JavaPlugin plugin) {
        this.bind((Class<JavaPlugin>) plugin.getClass(), plugin);
    }
}
