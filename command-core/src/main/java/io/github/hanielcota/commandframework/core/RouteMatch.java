package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Objects;

public record RouteMatch(CommandRoute route, List<String> arguments, int consumedLiterals) {

    public RouteMatch(CommandRoute route, List<String> arguments, int consumedLiterals) {
        this.route = Objects.requireNonNull(route, "route");
        this.arguments = List.copyOf(arguments);
        if (consumedLiterals < 0) {
            throw new IllegalArgumentException("Invalid consumed literals: expected zero or positive");
        }
        this.consumedLiterals = consumedLiterals;
    }
}
