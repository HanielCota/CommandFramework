package com.github.hanielcota.commandframework;

import com.github.hanielcota.commandframework.command.HelloCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandFrameworkPlugin extends JavaPlugin {
    CommandFramework framework;

    @Override
    public void onEnable() {
        framework = new CommandFramework(this);

        framework.registerCommands(new HelloCommand());

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
