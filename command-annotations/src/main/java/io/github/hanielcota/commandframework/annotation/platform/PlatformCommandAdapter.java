package io.github.hanielcota.commandframework.annotation.platform;

import io.github.hanielcota.commandframework.annotation.scan.AnnotatedCommandScanner;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.CommandRoute;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base for platform-specific command adapters.
 */
public abstract class PlatformCommandAdapter {

    private final CommandDispatcher dispatcher;
    private final AnnotatedCommandScanner scanner;
    private final Set<CommandRoot> registeredRoots = ConcurrentHashMap.newKeySet();
    private final List<CommandRoute> registeredRoutes = new CopyOnWriteArrayList<>();

    protected PlatformCommandAdapter(CommandDispatcher dispatcher, AnnotatedCommandScanner scanner) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public CommandDispatcher dispatcher() {
        return dispatcher;
    }

    public List<CommandRoute> scan(Object commandInstance) {
        return scanner.scan(Objects.requireNonNull(commandInstance, "commandInstance"));
    }

    public void registerRoutes(List<CommandRoute> routes) {
        Objects.requireNonNull(routes, "routes").forEach(route -> {
            Objects.requireNonNull(route, "route");
            dispatcher.register(route);
            registeredRoutes.add(route);
        });
        registerPlatformRoots();
    }

    public void registerAnnotated(Object commandInstance) {
        registerRoutes(scan(Objects.requireNonNull(commandInstance, "commandInstance")));
    }

    private void registerPlatformRoots() {
        for (CommandRoot root : dispatcher.roots()) {
            if (!registeredRoots.contains(root)) {
                try {
                    registerRoot(root);
                    registeredRoots.add(root);
                } catch (RuntimeException exception) {
                    dispatcher.logger().warn("Failed to register command root: " + root.label(), exception);
                }
            }
        }
    }

    public void unregisterAll() {
        for (CommandRoute route : registeredRoutes) {
            dispatcher.unregister(route);
        }
        registeredRoutes.clear();
        for (CommandRoot root : registeredRoots) {
            unregisterRoot(root);
        }
        registeredRoots.clear();
    }

    public void shutdown() {
        unregisterAll();
    }

    protected abstract void registerRoot(CommandRoot root);

    protected abstract void unregisterRoot(CommandRoot root);

    protected Set<CommandRoot> registeredRoots() {
        return registeredRoots;
    }
}
