package io.github.hanielcota.commandframework.paper;

import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers and unregisters commands using the Bukkit {@link SimpleCommandMap}.
 *
 * <p><strong>Warning:</strong> Unregistration uses reflection to access the internal
 * {@code knownCommands} map of {@code SimpleCommandMap}. This may break on future
 * Minecraft/Paper versions if the internal structure changes. Unregistered commands
 * may also remain visible in tab-completion for already-connected clients until they
 * reconnect.</p>
 */
final class BukkitPaperCommandRegistrar implements PaperCommandRegistrar {

    private static final Logger LOG = Logger.getLogger(BukkitPaperCommandRegistrar.class.getName());

    @Override
    public void register(JavaPlugin plugin, CommandRoot root, CommandDispatcher dispatcher) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        String fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
        boolean primaryRegistered = Bukkit.getCommandMap().register(fallbackPrefix, new PaperCommandBridge(root, dispatcher, plugin));
        if (!primaryRegistered && Bukkit.getCommandMap().getCommand(fallbackPrefix + ":" + root.label()) == null) {
            throw new IllegalStateException("Unable to register Paper command: " + root.label());
        }
    }

    @Override
    public void unregister(JavaPlugin plugin, CommandRoot root, CommandDispatcher dispatcher) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(dispatcher, "dispatcher");
        try {
            SimpleCommandMap commandMap = (SimpleCommandMap) Bukkit.getCommandMap();
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            knownCommands.remove(root.label());
            knownCommands.remove(plugin.getName().toLowerCase(Locale.ROOT) + ":" + root.label());
            for (String alias : root.aliases()) {
                knownCommands.remove(alias);
                knownCommands.remove(plugin.getName().toLowerCase(Locale.ROOT) + ":" + alias);
            }
        } catch (ReflectiveOperationException exception) {
            LOG.warning("Unable to unregister Paper command via reflection: " + root.label() + " - " + exception.getMessage());
        }
    }
}
