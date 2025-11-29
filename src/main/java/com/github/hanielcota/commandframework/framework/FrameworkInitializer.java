package com.github.hanielcota.commandframework.framework;

import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.messaging.MessageProvider;
import com.github.hanielcota.commandframework.messaging.MiniMessageProvider;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import com.github.hanielcota.commandframework.parser.builtin.BuiltinParsers;
import com.github.hanielcota.commandframework.processor.CommandProcessor;
import com.github.hanielcota.commandframework.registry.CommandScanner;
import com.github.hanielcota.commandframework.cache.FrameworkCaches;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;

import java.time.Duration;

public class FrameworkInitializer {
    private final Plugin plugin;

    public FrameworkInitializer(Plugin plugin) {
        this.plugin = plugin;
    }

    public FrameworkConfiguration initialize(MessageProvider messageProvider) {
        var handlerCache = FrameworkCaches.handlerInstances();
        var parserRegistry = ArgumentParserRegistry.create();
        BuiltinParsers.registerAll(parserRegistry);
        var scanner = new CommandScanner();
        var executor = new CommandExecutor(plugin, parserRegistry);
        var cooldownService = CooldownService.create(Duration.ofHours(1));
        var errorHandler = new GlobalErrorHandler(messageProvider);
        var processor = new CommandProcessor(
                plugin,
                parserRegistry,
                executor,
                cooldownService,
                errorHandler
        );
        return new FrameworkConfiguration(
                messageProvider,
                parserRegistry,
                scanner,
                executor,
                processor,
                handlerCache
        );
    }

    public FrameworkConfiguration initializeWithAdventure() {
        var audiences = BukkitAudiences.create(plugin);
        var miniMessage = MiniMessage.miniMessage();
        var messageProvider = new MiniMessageProvider(audiences, miniMessage);
        return initialize(messageProvider);
    }
}

