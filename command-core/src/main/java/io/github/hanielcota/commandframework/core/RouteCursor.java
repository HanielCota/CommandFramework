package io.github.hanielcota.commandframework.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Tracks the current position while walking the command tree.
 */
public record RouteCursor(@Nullable CommandRoute routeValue, int consumedLiterals) {

    public static RouteCursor fromDefault(CommandNode rootNode) {
        Objects.requireNonNull(rootNode, "rootNode");
        return new RouteCursor(rootNode.defaultRoute().orElse(null), 0);
    }

    public RouteCursor next(CommandNode node, int consumedLiterals) {
        Objects.requireNonNull(node, "node");
        return node.route()
                .map(found -> new RouteCursor(found, consumedLiterals))
                .orElse(this);
    }

    public Optional<CommandRoute> route() {
        return Optional.ofNullable(routeValue);
    }

    public RouteMatch toMatch(List<String> arguments) {
        Objects.requireNonNull(arguments, "arguments");
        CommandRoute matchedRoute = Objects.requireNonNull(routeValue, "routeValue");
        List<String> remaining = new ArrayList<>(arguments.subList(consumedLiterals, arguments.size()));
        return new RouteMatch(matchedRoute, remaining, consumedLiterals);
    }
}
