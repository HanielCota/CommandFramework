package io.github.hanielcota.commandframework.paper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers commands via Paper's modern Brigadier lifecycle API (1.20.6+).
 *
 * <p>Unlike {@link BukkitPaperCommandRegistrar}, this registrar provides native
 * client-side command visibility, rich argument types, and Brigadier-based
 * tab-completion.</p>
 *
 * <p><strong>Unregistration limitation:</strong> Paper does not expose a public
 * Brigadier unregister API. When {@link #unregister} is called, the handler
 * is deactivated so that subsequent invocations become no-ops, but the command
 * remains registered in the Brigadier tree until the proxy restarts.</p>
 */
final class PaperBrigadierRegistrar implements PaperCommandRegistrar {

    private final Map<String, BrigadierHandler> handlers = new ConcurrentHashMap<>();

    @Override
    public void register(JavaPlugin plugin, CommandRoot root, CommandDispatcher dispatcher) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        if (handlers.containsKey(root.label())) {
            return;
        }

        String description = root.node().route().map(CommandRoute::description).filter(d -> !d.isBlank()).orElse("CommandFramework command");
        BrigadierHandler handler = new BrigadierHandler(dispatcher, root.label(), plugin);
        if (handlers.putIfAbsent(root.label(), handler) != null) {
            return;
        }
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(
                    root.label(),
                    description,
                    root.aliases().stream().toList(),
                    handler
            );
        });
    }

    @Override
    public void unregister(JavaPlugin plugin, CommandRoot root, CommandDispatcher dispatcher) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        BrigadierHandler handler = handlers.remove(root.label());
        if (handler != null) {
            handler.deactivate();
        }
    }

    private static final class BrigadierHandler implements BasicCommand {

        private final CommandDispatcher dispatcher;
        private final String alias;
        private final Plugin plugin;
        private final Cache<CommandSourceStack, PaperCommandActor> actorCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(5))
                .build();
        private volatile boolean active = true;

        BrigadierHandler(CommandDispatcher dispatcher, String alias, Plugin plugin) {
            this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
            this.alias = Objects.requireNonNull(alias, "alias");
            this.plugin = Objects.requireNonNull(plugin, "plugin");
        }

        void deactivate() {
            active = false;
            actorCache.invalidateAll();
        }

        @Override
        public void execute(CommandSourceStack sourceStack, String[] args) {
            if (!active) {
                return;
            }
            dispatcher.dispatch(actorFor(sourceStack), alias, args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack sourceStack, String[] args) {
            if (!active) {
                return List.of();
            }
            return dispatcher.suggest(actorFor(sourceStack), alias, args);
        }

        private PaperCommandActor actorFor(CommandSourceStack sourceStack) {
            return actorCache.get(sourceStack, s -> new PaperCommandActor(s.getSender(), plugin));
        }
    }
}
