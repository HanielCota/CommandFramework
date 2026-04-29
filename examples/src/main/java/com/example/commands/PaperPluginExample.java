package com.example.commands;

import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperPluginExample extends JavaPlugin {

    private PaperCommandFramework commands;

    @Override
    public void onEnable() {
        try {
            commands = PaperCommandFramework.builder(this)
                    .messageProvider(new CustomMessageProvider())
                    .interceptor(new AdminCommand.AdminInterceptor())
                    .build();

            commands.registerAnnotated(new KitCommand());
            commands.registerAnnotated(new AdminCommand());
            commands.registerAnnotated(new DebugCommand(commands.dispatcher()));

            getLogger().info("Comandos registrados com sucesso!");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Falha ao registrar comandos", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (commands != null) {
            commands.shutdown();
        }
    }
}
