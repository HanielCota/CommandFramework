package io.github.hanielcota.commandframework.core;

import io.github.hanielcota.commandframework.core.route.CommandLiteralNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class CommandRouteRegistry implements RouteResolver {

    private final CommandLiteralNormalizer normalizer = new CommandLiteralNormalizer();
    private final Map<String, CommandRoot> aliasToRoot = new ConcurrentHashMap<>();
    private final Map<String, CommandRoot> roots = new ConcurrentHashMap<>();
    private final Map<CommandRoute, CommandNode> routeToNode = new ConcurrentHashMap<>();
    private final Map<CommandRoute, Set<String>> routeToAliases = new ConcurrentHashMap<>();

    public synchronized void register(CommandRoute route) {
        CommandRoute checkedRoute = Objects.requireNonNull(route, "route");
        String normalizedRoot = normalizer.normalize(checkedRoute.root());
        CommandRoot root = rootFor(checkedRoute, normalizedRoot);
        validateAliases(checkedRoute, root);
        CommandNode node = registerPath(checkedRoute, root.node());
        registerAliases(checkedRoute, root, normalizedRoot);
        routeToNode.put(checkedRoute, node);
        routeToAliases.put(checkedRoute, new LinkedHashSet<>(checkedRoute.aliases()));
    }

    public synchronized void unregister(CommandRoute route) {
        CommandRoute checkedRoute = Objects.requireNonNull(route, "route");
        CommandNode node = routeToNode.remove(checkedRoute);
        if (node == null) {
            return;
        }
        node.defaultRoute().ifPresent(dr -> {
            if (dr == checkedRoute) {
                node.clearDefaultRoute();
            }
        });
        node.route().ifPresent(r -> {
            if (r == checkedRoute) {
                node.clearRoute();
            }
        });
        cleanupAliases(checkedRoute);
        pruneEmptyNodes(checkedRoute);
    }

    private void cleanupAliases(CommandRoute route) {
        Set<String> removedAliases = routeToAliases.remove(route);
        if (removedAliases == null) {
            return;
        }
        Set<String> remainingAliases = collectRemainingAliases();
        Set<String> normalizedRemaining = new LinkedHashSet<>();
        for (String alias : remainingAliases) {
            normalizedRemaining.add(normalizer.normalize(alias));
        }
        String normalizedRoot = normalizer.normalize(route.root());
        CommandRoot root = roots.get(normalizedRoot);
        if (root == null) {
            return;
        }
        for (String alias : removedAliases) {
            String normalized = normalizer.normalize(alias);
            if (!normalizedRemaining.contains(normalized)) {
                aliasToRoot.remove(normalized);
            }
        }
        Set<String> kept = new LinkedHashSet<>();
        for (String alias : root.aliases()) {
            if (normalizedRemaining.contains(normalizer.normalize(alias))) {
                kept.add(alias);
            }
        }
        if (!kept.equals(root.aliases())) {
            CommandRoot updated = new CommandRoot(root.label(), kept, root.node());
            roots.put(normalizedRoot, updated);
            aliasToRoot.replaceAll((key, value) -> value == root ? updated : value);
        }
        if (!rootStillUsed(route.root())) {
            roots.remove(normalizedRoot);
            aliasToRoot.remove(normalizedRoot);
        }
    }

    private Set<String> collectRemainingAliases() {
        Set<String> all = new LinkedHashSet<>();
        for (Set<String> aliases : routeToAliases.values()) {
            all.addAll(aliases);
        }
        return all;
    }

    private boolean rootStillUsed(String rootLabel) {
        String normalized = normalizer.normalize(rootLabel);
        return routeToNode.keySet().stream().anyMatch(r -> normalizer.normalize(r.root()).equals(normalized));
    }

    /**
 * Resolves a command route by label and arguments.
     *
     * <p><strong>Thread-safety:</strong> Registration ({@link #register}) and
     * unregistration ({@link #unregister}) are synchronized. Resolution
     * ({@link #resolve}) reads from concurrent maps and is safe to call from
     * any thread. However, if routes are registered or unregistered concurrently
     * with resolution, a transient {@link RouteResolution.NotFound} may occur.
     * In practice, registration happens at startup and resolution at runtime,
     * so contention is rare. Avoid registering or unregistering routes while
     * the server is handling commands.</p>
     */
    @Override
    public RouteResolution resolve(String label, List<String> arguments) {
        String checkedLabel = Objects.requireNonNull(label, "label");
        Objects.requireNonNull(arguments, "arguments");
        List<String> checkedArguments = List.copyOf(arguments);
        CommandRoot root = aliasToRoot.get(normalizer.normalize(checkedLabel));
        if (root == null) {
            return RouteResolution.notFound(checkedLabel, "registered command");
        }
        return findRoute(root, checkedArguments);
    }

    @Override
    public List<CommandRoot> roots() {
        return roots.values().stream()
                .sorted(Comparator.comparing(CommandRoot::label))
                .toList();
    }

    @Override
    public List<String> rootSuggestions(String prefix) {
        String checkedPrefix = Objects.requireNonNull(prefix, "prefix");
        String normalizedPrefix = normalizer.normalize(checkedPrefix);
        return aliasToRoot.keySet().stream()
                .filter(label -> label.startsWith(normalizedPrefix))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public Optional<CommandRoot> root(String label) {
        String checkedLabel = Objects.requireNonNull(label, "label");
        return Optional.ofNullable(aliasToRoot.get(normalizer.normalize(checkedLabel)));
    }

    private CommandRoot rootFor(CommandRoute route, String normalizedRoot) {
        CommandRoot existingRoot = roots.get(normalizedRoot);
        if (existingRoot != null) {
            return existingRoot;
        }
        CommandRoot existingAliasTarget = aliasToRoot.get(normalizedRoot);
        if (existingAliasTarget != null) {
            throw new RouteConfigurationException(
                    "Invalid root '" + route.root() + "' for route '" + route.canonicalPath()
                            + "': expected unused root label"
            );
        }
        CommandRoot root = createRoot(route);
        roots.put(normalizedRoot, root);
        return root;
    }

    private CommandRoot createRoot(CommandRoute route) {
        CommandRoot root = new CommandRoot(route.root(), Set.of(), new CommandNode(route.root()));
        aliasToRoot.put(normalizer.normalize(route.root()), root);
        return root;
    }

    private void validateAliases(CommandRoute route, CommandRoot root) {
        for (String alias : route.aliases()) {
            CommandRoot existing = aliasToRoot.get(normalizer.normalize(alias));
            rejectAliasConflict(route, alias, existing, root);
        }
    }

    private void registerAliases(CommandRoute route, CommandRoot root, String normalizedRoot) {
        Set<String> aliases = new LinkedHashSet<>(root.aliases());
        for (String alias : route.aliases()) {
            String normalized = normalizer.normalize(alias);
            CommandRoot existing = aliasToRoot.putIfAbsent(normalized, root);
            rejectAliasConflict(route, alias, existing, root);
            aliases.add(alias);
        }
        if (aliases.equals(root.aliases())) {
            return;
        }
        CommandRoot updated = new CommandRoot(root.label(), aliases, root.node());
        roots.put(normalizedRoot, updated);
        aliasToRoot.replaceAll((key, value) -> value == root ? updated : value);
    }

    private void rejectAliasConflict(CommandRoute route, String alias, @Nullable CommandRoot existing, CommandRoot root) {
        if (existing == null || existing == root) {
            return;
        }
        throw new RouteConfigurationException(
                "Invalid alias '" + alias + "' for route '" + route.canonicalPath() + "': expected unused alias"
        );
    }

    private CommandNode registerPath(CommandRoute route, CommandNode rootNode) {
        if (route.path().isEmpty()) {
            rootNode.setDefaultRoute(route);
            return rootNode;
        }
        CommandNode target = commandNodeFor(route, rootNode);
        target.setRoute(route);
        return target;
    }

    private CommandNode commandNodeFor(CommandRoute route, CommandNode rootNode) {
        CommandNode current = rootNode;
        for (String segment : route.path()) {
            current = current.childOrCreate(normalizer.normalize(segment));
        }
        return current;
    }

    private RouteResolution findRoute(CommandRoot root, List<String> arguments) {
        RouteCursor cursor = walk(root.node(), arguments);
        if (cursor.route().isPresent()) {
            return RouteResolution.found(cursor.toMatch(arguments));
        }
        return RouteResolution.notFound(root.label(), "registered subcommand");
    }

    private RouteCursor walk(CommandNode rootNode, List<String> arguments) {
        RouteCursor cursor = RouteCursor.fromDefault(rootNode);
        CommandNode current = rootNode;
        for (int index = 0; index < arguments.size(); index++) {
            Optional<CommandNode> child = current.child(normalizer.normalize(arguments.get(index)));
            if (child.isEmpty()) {
                return cursor;
            }
            current = child.get();
            cursor = cursor.next(current, index + 1);
        }
        return cursor;
    }

    private void pruneEmptyNodes(CommandRoute route) {
        List<String> path = route.path();
        if (path.isEmpty()) {
            return;
        }
        String normalizedRoot = normalizer.normalize(route.root());
        CommandRoot root = roots.get(normalizedRoot);
        if (root == null) {
            return;
        }
        List<CommandNode> nodeChain = new ArrayList<>();
        List<String> normalizedSegments = new ArrayList<>();
        nodeChain.add(root.node());
        CommandNode current = root.node();
        for (String segment : path) {
            String normalized = normalizer.normalize(segment);
            normalizedSegments.add(normalized);
            Optional<CommandNode> child = current.child(normalized);
            if (child.isEmpty()) {
                break;
            }
            current = child.get();
            nodeChain.add(current);
        }
        for (int i = nodeChain.size() - 1; i > 0; i--) {
            CommandNode node = nodeChain.get(i);
            if (!node.isEmpty()) {
                break;
            }
            CommandNode parent = nodeChain.get(i - 1);
            parent.removeChild(normalizedSegments.get(i - 1));
        }
    }
}
