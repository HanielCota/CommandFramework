package io.github.hanielcota.commandframework.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.*;

/**
 * Immutable metadata records shared across the framework internals: the compiled representation of
 * a scanned command class and of each executor method, plus the per-invocation records the
 * dispatcher passes down its pipeline (tokenised input, selected executor, prepared invocation).
 *
 * <p>All records in this file enforce deep defensive copies of their collection components
 * ({@link List#copyOf} / {@link Set#copyOf}) inside their compact constructors, so instances are
 * safe to share across threads indefinitely.
 */
record CommandDefinition(
        Object instance,
        String name,
        List<String> aliases,
        String description,
        ExecutorDefinition rootExecutor,
        Map<String, ExecutorDefinition> executorsBySubcommand,
        Set<String> confirmationCommandNames
) {
    CommandDefinition {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(name, "name");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(executorsBySubcommand, "executorsBySubcommand");
        confirmationCommandNames = Set.copyOf(Objects.requireNonNull(confirmationCommandNames, "confirmationCommandNames"));
    }

    List<String> labels() {
        ArrayList<String> labels = new ArrayList<>(1 + this.aliases.size());
        labels.add(this.name);
        labels.addAll(this.aliases);
        return List.copyOf(labels);
    }
}

record ExecutorDefinition(
        Method method,
        String subcommand,
        String description,
        String permission,
        boolean requirePlayer,
        boolean async,
        CooldownDefinition cooldown,
        ConfirmDefinition confirm,
        List<ParameterDefinition> parameters
) {
    ExecutorDefinition {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(subcommand, "subcommand");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(permission, "permission");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    }
}

record ParameterDefinition(
        Parameter parameter,
        String name,
        Class<?> rawType,
        Class<?> resolvedType,
        boolean sender,
        boolean javaOptional,
        boolean optional,
        String optionalValue,
        boolean greedy,
        int maxLength
) {
    ParameterDefinition {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(resolvedType, "resolvedType");
        Objects.requireNonNull(optionalValue, "optionalValue");
    }
}

record CooldownDefinition(Duration duration, String bypassPermission) {
    CooldownDefinition {
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(bypassPermission, "bypassPermission");
    }
}

record ConfirmDefinition(Duration expiresIn, String commandName) {
    ConfirmDefinition {
        Objects.requireNonNull(expiresIn, "expiresIn");
        Objects.requireNonNull(commandName, "commandName");
    }
}

record TokenizedInput(List<String> tokens, boolean trailingSpace) {
    TokenizedInput {
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens"));
    }
}

record Selection(ExecutorDefinition executor, List<String> argumentTokens, String commandPath) {
    Selection {
        Objects.requireNonNull(executor, "executor");
        argumentTokens = List.copyOf(Objects.requireNonNull(argumentTokens, "argumentTokens"));
        Objects.requireNonNull(commandPath, "commandPath");
    }
}

record PreparedInvocation(
        CommandDefinition command,
        ExecutorDefinition executor,
        String label,
        String commandPath,
        List<Object> preparedArguments
) {
    PreparedInvocation {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(commandPath, "commandPath");
        preparedArguments = List.copyOf(Objects.requireNonNull(preparedArguments, "preparedArguments"));
    }
}
