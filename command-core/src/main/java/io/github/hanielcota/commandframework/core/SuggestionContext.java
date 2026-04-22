package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Objects;

public record SuggestionContext(
        CommandActor actor,
        CommandRoute route,
        String currentInput,
        List<String> arguments
) {

    public SuggestionContext(
            CommandActor actor,
            CommandRoute route,
            String currentInput,
            List<String> arguments
    ) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.route = Objects.requireNonNull(route, "route");
        this.currentInput = Objects.requireNonNull(currentInput, "currentInput");
        this.arguments = List.copyOf(arguments);
    }
}
