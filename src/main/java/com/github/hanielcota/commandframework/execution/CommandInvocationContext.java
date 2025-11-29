package com.github.hanielcota.commandframework.execution;

import com.mojang.brigadier.context.CommandContext;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Value
@Builder
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandInvocationContext {

    CommandSender sender;
    String label;
    List<String> arguments;
    CommandContext<CommandSender> brigadierContext;
    Method handlerMethod;
    Object handlerInstance;

    public Optional<CompletionStage<?>> asAsyncResult(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof CompletionStage<?> stage) {
            return Optional.of(stage);
        }

        return Optional.empty();
    }

    public Optional<Component> asComponent(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Component component) {
            return Optional.of(component);
        }

        if (value instanceof String string) {
            return Optional.of(Component.text(string));
        }

        return Optional.empty();
    }
}


