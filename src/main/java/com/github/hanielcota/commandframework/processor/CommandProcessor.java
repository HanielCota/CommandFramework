package com.github.hanielcota.commandframework.processor;

import com.mojang.brigadier.CommandDispatcher;
import com.github.hanielcota.commandframework.brigadier.BrigadierTreeBuilder;
import com.github.hanielcota.commandframework.brigadier.CommandMetadata;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import com.github.hanielcota.commandframework.registry.CommandDefinition;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandProcessor {

    Plugin plugin;
    ArgumentParserRegistry parserRegistry;
    CommandExecutor executor;
    CooldownService cooldownService;
    GlobalErrorHandler errorHandler;

    public void processAndRegister(List<CommandDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        var dispatcher = getDispatcher();
        if (dispatcher.isEmpty()) {
            return;
        }

        var builder = new BrigadierTreeBuilder(
            plugin,
            parserRegistry,
            executor,
            cooldownService,
            errorHandler
        );

        for (var definition : definitions) {
            var metadata = toMetadata(definition);
            if (metadata == null) {
                continue;
            }

            builder.buildAndRegister(dispatcher.get(), metadata);
        }
    }

    private CommandMetadata toMetadata(CommandDefinition definition) {
        if (definition == null) {
            return null;
        }

        var instance = createInstance(definition.getType());
        if (instance == null) {
            return null;
        }

        return CommandMetadata.builder()
            .commandAnnotation(definition.getAnnotation())
            .type(definition.getType())
            .instance(instance)
            .handlers(definition.getHandlers())
            .build();
    }

    private Object createInstance(Class<?> type) {
        if (type == null) {
            return null;
        }

        try {
            var constructors = type.getDeclaredConstructors();
            if (constructors.length == 0) {
                return type.getDeclaredConstructor().newInstance();
            }

            var constructor = constructors[0];
            constructor.setAccessible(true);

            var parameters = constructor.getParameterCount();
            if (parameters == 0) {
                return constructor.newInstance();
            }

            var args = new Object[parameters];
            for (int i = 0; i < parameters; i++) {
                var paramType = constructor.getParameterTypes()[i];
                args[i] = resolveDependency(paramType);
            }

            return constructor.newInstance(args);
        } catch (Exception e) {
            return null;
        }
    }

    private Object resolveDependency(Class<?> type) {
        if (type == null) {
            return null;
        }

        if (type.equals(Plugin.class)) {
            return plugin;
        }

        if (type.equals(ArgumentParserRegistry.class)) {
            return parserRegistry;
        }

        if (type.equals(CommandExecutor.class)) {
            return executor;
        }

        if (type.equals(CooldownService.class)) {
            return cooldownService;
        }

        if (type.equals(GlobalErrorHandler.class)) {
            return errorHandler;
        }

        return null;
    }

    private Optional<CommandDispatcher<CommandSender>> getDispatcher() {
        try {
            var server = Bukkit.getServer();
            var method = server.getClass().getMethod("getCommandDispatcher");
            var dispatcher = method.invoke(server);
            if (dispatcher instanceof CommandDispatcher<?> d) {
                @SuppressWarnings("unchecked")
                var typed = (CommandDispatcher<CommandSender>) d;
                return Optional.of(typed);
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }
}

