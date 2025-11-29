package com.github.hanielcota.commandframework.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.hanielcota.commandframework.CommandFramework;
import com.github.hanielcota.commandframework.cache.FrameworkCaches;
import com.github.hanielcota.commandframework.messaging.MiniMessageProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Exemplo de plugin usando a Command Framework.
 */
public class ExamplePlugin extends JavaPlugin {

    private CommandFramework framework;
    private BukkitAudiences audiences;

    @Override
    public void onEnable() {
        audiences = BukkitAudiences.create(this);
        var miniMessage = MiniMessage.miniMessage();
        var messageProvider = new MiniMessageProvider(audiences, miniMessage);

        Cache<Class<?>, Object> handlerCache = FrameworkCaches.handlerInstances();
        framework = CommandFramework.create(this, messageProvider, handlerCache);

        framework.registerPackage("com.github.hanielcota.commandframework.example");
    }

    @Override
    public void onDisable() {
        if (audiences != null) {
            audiences.close();
        }
    }
}

