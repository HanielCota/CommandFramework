package com.github.hanielcota.commandframework;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.messaging.MessageProvider;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import com.github.hanielcota.commandframework.parser.builtin.BuiltinParsers;
import com.github.hanielcota.commandframework.processor.CommandProcessor;
import com.github.hanielcota.commandframework.registry.CommandScanner;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandFramework {

    Plugin plugin;
    MessageProvider messageProvider;
    ArgumentParserRegistry parserRegistry;
    CommandScanner scanner;
    CommandExecutor executor;
    CommandProcessor processor;
    Cache<Class<?>, Object> handlerCache;

    public static CommandFramework create(Plugin plugin, MessageProvider messageProvider, Cache<Class<?>, Object> handlerCache) {
        var parserRegistry = ArgumentParserRegistry.create();
        BuiltinParsers.registerAll(parserRegistry);

        var scanner = new CommandScanner();
        var executor = new CommandExecutor(plugin, parserRegistry);

        var cooldownService = CooldownService.create(java.time.Duration.ofMinutes(5));
        var errorHandler = new GlobalErrorHandler(messageProvider);

        var processor = new CommandProcessor(
                plugin,
                parserRegistry,
                executor,
                cooldownService,
                errorHandler
        );

        return new CommandFramework(
                plugin,
                messageProvider,
                parserRegistry,
                scanner,
                executor,
                processor,
                handlerCache
        );
    }

    public void registerPackage(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> scanAndRegister(basePackage));
    }

    private void scanAndRegister(String basePackage) {
        var definitions = scanner.scan(basePackage);
        if (definitions.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> processor.processAndRegister(definitions));
    }

    public void reload() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var definitions = scanner.scan(plugin.getClass().getPackage().getName());
            Bukkit.getScheduler().runTask(plugin, () -> processor.processAndRegister(definitions));
        });
    }

    public ArgumentParserRegistry getParserRegistry() {
        return parserRegistry;
    }
}

 