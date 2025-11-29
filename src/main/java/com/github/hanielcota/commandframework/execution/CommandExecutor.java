package com.github.hanielcota.commandframework.execution;

import com.github.hanielcota.commandframework.annotation.Async;
import com.github.hanielcota.commandframework.model.CommandResult;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandExecutor {

    Plugin plugin;
    ArgumentParserRegistry parserRegistry;

    public void execute(CommandInvocationContext context) {
        if (context == null) {
            return;
        }

        var method = context.getHandlerMethod();
        if (method == null) {
            return;
        }

        if (method.isAnnotationPresent(Async.class)) {
            runAsync(context);
            return;
        }

        runSync(context);
    }

    private void runAsync(CommandInvocationContext context) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> invokeAndHandle(context));
    }

    private void runSync(CommandInvocationContext context) {
        Bukkit.getScheduler().runTask(plugin, () -> invokeAndHandle(context));
    }

    private void invokeAndHandle(CommandInvocationContext context) {
        var result = invoke(context);
        if (result.isEmpty()) {
            return;
        }

        var value = result.get();
        var async = context.asAsyncResult(value);
        if (async.isPresent()) {
            handleAsync(context, async.get());
            return;
        }

        handleSyncResult(context.getSender(), value);
    }

    private Optional<Object> invoke(CommandInvocationContext context) {
        var method = context.getHandlerMethod();
        var instance = context.getHandlerInstance();

        try {
            var resolver = new CommandParameterResolver();
            var parameters = resolver.resolve(method, context);
            var value = method.invoke(instance, parameters.toArray());
            return Optional.ofNullable(value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return Optional.of(e);
        }
    }

    private void handleAsync(CommandInvocationContext context, CompletionStage<?> stage) {
        stage.whenComplete((value, throwable) -> {
            if (throwable != null) {
                sendError(context.getSender(), throwable);
                return;
            }

            if (value == null) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> handleSyncResult(context.getSender(), value));
        });
    }

    private void handleSyncResult(CommandSender sender, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof CommandResult.Success success) {
            send(sender, success.message());
            return;
        }

        if (value instanceof CommandResult.Failure failure) {
            send(sender, failure.message());
            return;
        }

        if (value instanceof CommandResult.NoOp) {
            return;
        }

        if (value instanceof Component component) {
            send(sender, component);
            return;
        }

        if (value instanceof String string) {
            send(sender, Component.text(string));
            return;
        }
    }

    private void sendError(CommandSender sender, Throwable throwable) {
        if (sender == null) {
            return;
        }

        if (throwable == null) {
            return;
        }

        var message = Component.text("Ocorreu um erro ao executar o comando.");
        send(sender, message);
    }

    private void send(CommandSender sender, Component component) {
        if (sender == null) {
            return;
        }

        if (component == null) {
            return;
        }

        sender.sendMessage(component);
    }

    private final class CommandParameterResolver {

        private CommandParameterResolver() {
        }

        java.util.List<Object> resolve(Method method, CommandInvocationContext context) {
            var parameters = method.getParameters();
            var values = new java.util.ArrayList<>();

            for (var parameter : parameters) {
                var resolved = resolveParameter(parameter, context);
                if (resolved.isPresent()) {
                    values.add(resolved.get());
                }
            }

            return java.util.List.copyOf(values);
        }

        private Optional<Object> resolveParameter(Parameter parameter, CommandInvocationContext context) {
            if (parameter == null) {
                return Optional.empty();
            }

            var type = parameter.getType();
            if (type.equals(CommandSender.class)) {
                return Optional.of(context.getSender());
            }

            if (type.equals(CommandInvocationContext.class)) {
                return Optional.of(context);
            }

            var brigadierContext = context.getBrigadierContext();
            if (brigadierContext == null) {
                return Optional.empty();
            }

            var name = parameterName(parameter);
            var parser = parserRegistry.find(type);
            if (parser.isEmpty()) {
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Optional<Object> result = (Optional<Object>) parser.get().parse(brigadierContext, name);
            return result;
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
    }
}


