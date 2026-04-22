package io.github.hanielcota.commandframework.paper;

import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers commands on Paper.
 *
 * <p>Implementations should prefer Paper's Brigadier lifecycle API
 * ({@link io.papermc.paper.command.brigadier.Commands}) when possible. Note that
 * Brigadier-registered commands cannot be cleanly unregistered — the
 * {@code unregister} default method is a no-op for registrars that have no
 * public API for command removal.</p>
 */
@FunctionalInterface
public interface PaperCommandRegistrar {

    void register(JavaPlugin plugin, CommandRoot root, CommandDispatcher dispatcher);

    default void unregister(JavaPlugin plugin, CommandRoot root, CommandDispatcher dispatcher) {
        // Paper does not expose a public API for unregistering commands.
    }
}
