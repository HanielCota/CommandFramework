package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.annotation.Alias;
import io.github.hanielcota.commandframework.annotation.Async;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Default;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.OnlyConsole;
import io.github.hanielcota.commandframework.annotation.OnlyPlayer;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.annotation.Syntax;
import io.github.hanielcota.commandframework.core.RouteConfigurationException;
import io.github.hanielcota.commandframework.core.SenderRequirement;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class RouteAnnotationReader {

    boolean isCommandMethod(Method method) {
        Objects.requireNonNull(method, "method");
        return method.isAnnotationPresent(Default.class) || method.isAnnotationPresent(Subcommand.class);
    }

    RouteAnnotationModel read(Class<?> commandType, Command command, Method method) {
        Objects.requireNonNull(commandType, "commandType");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(method, "method");
        validateMethodKind(method);
        Set<String> aliases = new java.util.LinkedHashSet<>(java.util.Arrays.asList(command.aliases()));
        Alias aliasAnnotation = method.getAnnotation(Alias.class);
        if (aliasAnnotation != null) {
            aliases.addAll(List.of(aliasAnnotation.value()));
        }
        return new RouteAnnotationModel(
                command.value(),
                aliases,
                path(method),
                permission(commandType, method),
                senderRequirement(commandType, method),
                cooldown(commandType, method),
                description(commandType, method),
                syntax(commandType, method),
                async(commandType, method)
        );
    }

    private void validateMethodKind(Method method) {
        boolean hasDefault = method.isAnnotationPresent(Default.class);
        boolean hasSubcommand = method.isAnnotationPresent(Subcommand.class);
        if (hasDefault ^ hasSubcommand) {
            return;
        }
        throw new RouteConfigurationException("Invalid method '" + method.getName() + "': expected exactly one of @Default or @Subcommand");
    }

    private List<String> path(Method method) {
        Subcommand subcommand = method.getAnnotation(Subcommand.class);
        if (subcommand == null) {
            return List.of();
        }
        String value = subcommand.value().trim();
        if (value.isBlank()) {
            throw new RouteConfigurationException("Invalid subcommand '" + value + "': expected non-empty path");
        }
        return Arrays.stream(value.split("\\s+"))
                .filter(segment -> !segment.isBlank())
                .toList();
    }

    private String permission(Class<?> commandType, Method method) {
        Permission methodPermission = method.getAnnotation(Permission.class);
        if (methodPermission != null) {
            return methodPermission.value();
        }
        Permission typePermission = commandType.getAnnotation(Permission.class);
        return typePermission == null ? "" : typePermission.value();
    }

    private SenderRequirement senderRequirement(Class<?> commandType, Method method) {
        validateSenderAnnotations(commandType);
        validateSenderAnnotations(method);
        if (method.isAnnotationPresent(OnlyPlayer.class)) {
            return SenderRequirement.PLAYER;
        }
        if (method.isAnnotationPresent(OnlyConsole.class)) {
            return SenderRequirement.CONSOLE;
        }
        return senderRequirement(commandType);
    }

    private SenderRequirement senderRequirement(Class<?> commandType) {
        if (commandType.isAnnotationPresent(OnlyPlayer.class)) {
            return SenderRequirement.PLAYER;
        }
        if (commandType.isAnnotationPresent(OnlyConsole.class)) {
            return SenderRequirement.CONSOLE;
        }
        return SenderRequirement.ANY;
    }

    private void validateSenderAnnotations(AnnotatedElement element) {
        if (!element.isAnnotationPresent(OnlyPlayer.class) || !element.isAnnotationPresent(OnlyConsole.class)) {
            return;
        }
        throw new RouteConfigurationException("Invalid sender restriction: expected only one sender annotation");
    }

    private Duration cooldown(Class<?> commandType, Method method) {
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        if (cooldown == null) {
            cooldown = commandType.getAnnotation(Cooldown.class);
        }
        if (cooldown == null) {
            return Duration.ZERO;
        }
        if (cooldown.value() < 0) {
            throw new RouteConfigurationException("Invalid cooldown '" + cooldown.value() + "': expected zero or positive");
        }
        return Duration.ofMillis(cooldown.unit().toMillis(cooldown.value()));
    }

    private String description(Class<?> commandType, Method method) {
        Description methodDescription = method.getAnnotation(Description.class);
        if (methodDescription != null) {
            return methodDescription.value();
        }
        Description typeDescription = commandType.getAnnotation(Description.class);
        return typeDescription == null ? "" : typeDescription.value();
    }

    private String syntax(Class<?> commandType, Method method) {
        Syntax methodSyntax = method.getAnnotation(Syntax.class);
        if (methodSyntax != null) {
            return methodSyntax.value();
        }
        Syntax typeSyntax = commandType.getAnnotation(Syntax.class);
        return typeSyntax == null ? "" : typeSyntax.value();
    }

    private boolean async(Class<?> commandType, Method method) {
        if (method.isAnnotationPresent(Async.class)) {
            return true;
        }
        return commandType.isAnnotationPresent(Async.class);
    }
}
