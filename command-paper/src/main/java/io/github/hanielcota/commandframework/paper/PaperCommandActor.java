package io.github.hanielcota.commandframework.paper;

import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandActor;
import java.util.Objects;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * Paper adapter for {@link CommandActor}.
 *
 * <p>When a {@link Plugin} reference is provided, {@link #sendMessage(String)}
 * automatically schedules message delivery on the server main thread if the call
 * originates from an async context. Without a plugin reference, messages are
 * sent immediately — callers must ensure they are on the main thread.</p>
 */
public final class PaperCommandActor implements CommandActor {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private final CommandSender sender;
    private final @Nullable Plugin plugin;

    public PaperCommandActor(CommandSender sender) {
        this(sender, null);
    }

    public PaperCommandActor(CommandSender sender, @Nullable Plugin plugin) {
        this.sender = Objects.requireNonNull(sender, "sender");
        this.plugin = plugin;
    }

    public CommandSender sender() {
        return sender;
    }

    @Override
    public String uniqueId() {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        if (sender instanceof ConsoleCommandSender) {
            return "paper:console";
        }
        return "paper:" + sender.getClass().getSimpleName() + ":" + Objects.requireNonNullElse(sender.getName(), "unknown");
    }

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public ActorKind kind() {
        return switch (sender) {
            case Player ignored -> ActorKind.PLAYER;
            case ConsoleCommandSender ignored -> ActorKind.CONSOLE;
            default -> ActorKind.OTHER;
        };
    }

    @Override
    public boolean hasPermission(String permission) {
        Objects.requireNonNull(permission, "permission");
        return sender.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        Objects.requireNonNull(message, "message");
        var component = SERIALIZER.deserialize(message);
        if (plugin != null && !Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(component));
        } else {
            sender.sendMessage(component);
        }
    }
}
