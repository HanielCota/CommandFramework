package com.github.hanielcota.commandframework.brigadier;

import com.github.hanielcota.commandframework.annotation.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.github.hanielcota.commandframework.cooldown.CooldownKey;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.execution.CommandInvocationContext;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Constrói árvores de comandos Brigadier a partir de metadados de comandos.
 * Converte anotações e métodos em estruturas de comandos do Brigadier.
 */
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BrigadierTreeBuilder {

    Plugin plugin;
    ArgumentParserRegistry parserRegistry;
    CommandExecutor executor;
    CooldownService cooldownService;
    GlobalErrorHandler errorHandler;

    /**
     * Constrói e registra um comando no dispatcher do Brigadier.
     *
     * @param dispatcher Dispatcher do Brigadier onde registrar o comando
     * @param metadata   Metadados do comando a ser construído
     */
    public void buildAndRegister(CommandDispatcher<CommandSender> dispatcher, CommandMetadata metadata) {
        if (dispatcher == null) {
            return;
        }

        if (metadata == null) {
            return;
        }

        var root = buildRoot(metadata);
        if (root == null) {
            return;
        }

        dispatcher.register(root);
    }

    private LiteralArgumentBuilder<CommandSender> buildRoot(CommandMetadata metadata) {
        var annotation = metadata.getCommandAnnotation();
        var name = annotation.name();

        var root = LiteralArgumentBuilder.<CommandSender>literal(name);

        var permission = findPermission(metadata.getType());
        if (permission != null) {
            root.requires(source -> hasPermission(source, permission.value()));
        }

        var defaultMethod = findDefaultMethod(metadata);
        if (defaultMethod != null) {
            root.executes(context -> executeDefault(context, metadata, defaultMethod));
        }

        for (var handler : metadata.getHandlers()) {
            buildSubCommand(root, handler, metadata);
        }

        return root;
    }

    private void buildSubCommand(LiteralArgumentBuilder<CommandSender> root, Method handler, CommandMetadata metadata) {
        if (handler == null) {
            return;
        }

        var subAnnotation = handler.getAnnotation(SubCommand.class);
        if (subAnnotation == null) {
            return;
        }

        var path = subAnnotation.value();
        if (path == null || path.isBlank()) {
            return;
        }

        var parts = path.split("\\s+");
        var current = root;

        for (var part : parts) {
            current = current.then(LiteralArgumentBuilder.<CommandSender>literal(part));
        }

        var permission = findPermission(handler);
        if (permission != null) {
            current.requires(source -> hasPermission(source, permission.value()));
        }

        var parameters = handler.getParameters();
        for (var parameter : parameters) {
            if (isContextParameter(parameter)) {
                continue;
            }

            var argument = buildArgument(parameter);
            if (argument == null) {
                continue;
            }

            current = current.then(argument);
        }

        current.executes(context -> executeHandler(context, metadata, handler));
    }

    private RequiredArgumentBuilder<CommandSender, ?> buildArgument(Parameter parameter) {
        if (parameter == null) {
            return null;
        }

        var type = parameter.getType();
        var parser = parserRegistry.find(type);
        if (parser.isEmpty()) {
            return null;
        }

        var p = parser.get();
        var name = parameterName(parameter);
        var brigadierType = p.brigadierType();

        @SuppressWarnings("unchecked")
        ArgumentType<Object> argType = (ArgumentType<Object>) brigadierType;
        var builder = RequiredArgumentBuilder.<CommandSender, Object>argument(name, argType);

        var tabCompletion = parameter.getAnnotation(TabCompletion.class);
        if (tabCompletion != null) {
            var provider = buildSuggestionProvider(tabCompletion);
            if (provider != null) {
                builder.suggests(provider);
            }
        }

        return builder;
    }

    private SuggestionProvider<CommandSender> buildSuggestionProvider(TabCompletion annotation) {
        if (annotation == null) {
            return null;
        }

        var providerClass = annotation.provider();
        if (providerClass == null || providerClass == TabCompletion.NoProvider.class) {
            return null;
        }

        try {
            var instance = providerClass.getDeclaredConstructor().newInstance();
            if (instance instanceof SuggestionProvider<CommandSender> provider) {
                return provider;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private int executeDefault(CommandContext<CommandSender> context, CommandMetadata metadata, Method method) {
        var sender = context.getSource();
        if (sender == null) {
            return 0;
        }

        // Verifica permissão do método
        var methodPermission = findPermission(method);
        if (methodPermission != null && !hasPermission(sender, methodPermission.value())) {
            errorHandler.handleNoPermission(sender, methodPermission.value())
                .ifPresent(sender::sendMessage);
            return 0;
        }

        var invocationContext = CommandInvocationContext.builder()
            .sender(sender)
            .label(metadata.getCommandAnnotation().name())
            .arguments(List.of())
            .brigadierContext(context)
            .handlerMethod(method)
            .handlerInstance(metadata.getInstance())
            .build();

        checkCooldownAndExecute(invocationContext, metadata, null);
        return 1;
    }

    private int executeHandler(CommandContext<CommandSender> context, CommandMetadata metadata, Method handler) {
        var sender = context.getSource();
        if (sender == null) {
            return 0;
        }

        var subAnnotation = handler.getAnnotation(SubCommand.class);
        if (subAnnotation == null) {
            return 0;
        }

        // Verifica permissão do método (verificação adicional para garantir mensagem de erro)
        var methodPermission = findPermission(handler);
        if (methodPermission != null && !hasPermission(sender, methodPermission.value())) {
            errorHandler.handleNoPermission(sender, methodPermission.value())
                .ifPresent(sender::sendMessage);
            return 0;
        }

        var invocationContext = CommandInvocationContext.builder()
            .sender(sender)
            .label(metadata.getCommandAnnotation().name())
            .arguments(extractArguments(context, handler))
            .brigadierContext(context)
            .handlerMethod(handler)
            .handlerInstance(metadata.getInstance())
            .build();

        checkCooldownAndExecute(invocationContext, metadata, subAnnotation.value());
        return 1;
    }

    private void checkCooldownAndExecute(CommandInvocationContext context, CommandMetadata metadata, String sub) {
        var sender = context.getSender();
        var uuid = senderUUID(sender);
        var command = metadata.getCommandAnnotation().name();

        // Verifica cooldown do método primeiro, depois da classe
        var cooldown = findCooldown(context.getHandlerMethod(), metadata.getType());
        if (cooldown != null) {
            var key = new CooldownKey(uuid, command, sub);
            var remainingTime = cooldownService.getRemainingTime(key);
            if (remainingTime.isPresent()) {
                errorHandler.handleCooldown(sender, remainingTime.get());
                return;
            }

            cooldownService.putOnCooldown(key, Duration.ofSeconds(cooldown.seconds()));
        }

        executor.execute(context);
    }

    private List<String> extractArguments(CommandContext<CommandSender> context, Method handler) {
        var parameters = handler.getParameters();
        var args = new java.util.ArrayList<String>();

        for (var parameter : parameters) {
            if (isContextParameter(parameter)) {
                continue;
            }

            var name = parameterName(parameter);
            var parser = parserRegistry.find(parameter.getType());
            if (parser.isEmpty()) {
                continue;
            }

            var value = parser.get().parse(context, name);
            if (value.isPresent()) {
                args.add(value.get().toString());
            }
        }

        return List.copyOf(args);
    }

    private String parameterName(Parameter parameter) {
        if (parameter == null) {
            return "arg";
        }

        var name = parameter.getName();
        if (name == null || name.isBlank()) {
            return "arg";
        }

        return name;
    }

    private boolean isContextParameter(Parameter parameter) {
        if (parameter == null) {
            return false;
        }

        var type = parameter.getType();
        return type.equals(CommandSender.class) || type.equals(CommandInvocationContext.class);
    }

    private Method findDefaultMethod(CommandMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        var methods = metadata.getType().getDeclaredMethods();
        for (var method : methods) {
            if (method.isAnnotationPresent(DefaultCommand.class)) {
                return method;
            }
        }

        return null;
    }

    private RequiredPermission findPermission(Class<?> type) {
        if (type == null) {
            return null;
        }

        return type.getAnnotation(RequiredPermission.class);
    }

    private RequiredPermission findPermission(Method method) {
        if (method == null) {
            return null;
        }

        return method.getAnnotation(RequiredPermission.class);
    }

    /**
     * Busca anotação @Cooldown primeiro no método, depois na classe.
     * Permite herança de cooldown da classe para todos os métodos.
     */
    private Cooldown findCooldown(Method method, Class<?> type) {
        if (method == null) {
            return null;
        }

        // Primeiro tenta o método
        var methodCooldown = method.getAnnotation(Cooldown.class);
        if (methodCooldown != null) {
            return methodCooldown;
        }

        // Depois tenta a classe
        if (type != null) {
            return type.getAnnotation(Cooldown.class);
        }

        return null;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null) {
            return false;
        }

        if (permission == null || permission.isBlank()) {
            return true;
        }

        return sender.hasPermission(permission);
    }

    private UUID senderUUID(CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player player) {
            return player.getUniqueId();
        }

        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
}
