package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Objects;

public record ParameterSuggestionContext(
        CommandActor actor,
        CommandRoute route,
        CommandParameter<?> parameter,
        String currentInput,
        List<String> arguments) {

    public ParameterSuggestionContext(
            CommandActor actor,
            CommandRoute route,
            CommandParameter<?> parameter,
            String currentInput,
            List<String> arguments) {

        this.actor = Objects.requireNonNull(actor, "actor");
        this.route = Objects.requireNonNull(route, "route");
        this.parameter = Objects.requireNonNull(parameter, "parameter");
        this.currentInput = Objects.requireNonNull(currentInput, "currentInput");
        this.arguments = List.copyOf(arguments);
    }
}
