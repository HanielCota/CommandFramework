package io.github.hanielcota.commandframework.core;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class CommandNode {

    private final String literal;
    private final Map<String, CommandNode> children = new ConcurrentHashMap<>();
    private volatile @Nullable CommandRoute route;
    private volatile @Nullable CommandRoute defaultRoute;

    public CommandNode(String literal) {
        this.literal = Objects.requireNonNull(literal, "literal");
    }

    public String literal() {
        return literal;
    }

    public CommandNode childOrCreate(String childLiteral) {
        Objects.requireNonNull(childLiteral, "childLiteral");
        return children.computeIfAbsent(childLiteral, CommandNode::new);
    }

    public Optional<CommandNode> child(String childLiteral) {
        Objects.requireNonNull(childLiteral, "childLiteral");
        return Optional.ofNullable(children.get(childLiteral));
    }

    public List<String> childLiteralsStartingWith(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return children.keySet().stream()
                .filter(value -> value.startsWith(prefix))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public Optional<CommandRoute> route() {
        return Optional.ofNullable(route);
    }

    public Optional<CommandRoute> defaultRoute() {
        return Optional.ofNullable(defaultRoute);
    }

    public synchronized void setRoute(CommandRoute route) {
        CommandRoute checkedRoute = Objects.requireNonNull(route, "route");
        if (this.route != null) {
            throw duplicateRoute(checkedRoute);
        }
        this.route = checkedRoute;
    }

    public synchronized void setDefaultRoute(CommandRoute route) {
        CommandRoute checkedRoute = Objects.requireNonNull(route, "route");
        if (this.defaultRoute != null) {
            throw duplicateRoute(checkedRoute);
        }
        this.defaultRoute = checkedRoute;
    }

    public synchronized void clearRoute() {
        this.route = null;
    }

    public synchronized void clearDefaultRoute() {
        this.defaultRoute = null;
    }

    public synchronized boolean isEmpty() {
        return route == null && defaultRoute == null && children.isEmpty();
    }

    public void removeChild(String childLiteral) {
        Objects.requireNonNull(childLiteral, "childLiteral");
        children.remove(childLiteral);
    }

    private RouteConfigurationException duplicateRoute(CommandRoute route) {
        return new RouteConfigurationException(
                "Invalid route '" + route.canonicalPath() + "': expected unique canonical route"
        );
    }
}
