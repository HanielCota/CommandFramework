package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Set;

final class CommandRouteValidator {

    private CommandRouteValidator() {
    }

    static void validate(String root, Set<String> aliases, List<String> path, CommandExecutor executor) {
        validateLiteral(root, "root command");
        aliases.forEach(alias -> validateLiteral(alias, "command alias"));
        path.forEach(segment -> validateLiteral(segment, "route segment"));
        if (executor == null) {
            throw new RouteConfigurationException("Invalid executor: expected non-null command executor");
        }
    }

    static void validateGreedyPosition(List<CommandParameter<?>> parameters) {
        boolean seenGreedy = false;
        for (CommandParameter<?> parameter : parameters) {
            if (!parameter.consumesInput()) {
                continue;
            }
            if (seenGreedy) {
                throw new RouteConfigurationException(
                        "Invalid parameters: greedy parameter must be the last consuming parameter"
                );
            }
            if (parameter.resolver() instanceof io.github.hanielcota.commandframework.core.argument.GreedyStringParameterResolver
                    || parameter.resolver() instanceof io.github.hanielcota.commandframework.core.argument.RawArgumentsParameterResolver) {
                seenGreedy = true;
            }
        }
    }

    private static void validateLiteral(String literal, String expected) {
        if (literal == null || literal.isBlank()) {
            throw new RouteConfigurationException("Invalid command literal '" + literal + "': expected " + expected);
        }
        if (literal.chars().anyMatch(Character::isWhitespace)) {
            throw new RouteConfigurationException("Invalid command literal '" + literal + "': expected single word");
        }
    }
}
