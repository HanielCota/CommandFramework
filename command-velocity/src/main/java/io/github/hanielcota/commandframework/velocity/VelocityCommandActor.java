package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandActor;
import java.util.Objects;
import net.kyori.adventure.text.Component;

public record VelocityCommandActor(CommandSource source) implements CommandActor {

    public VelocityCommandActor {
        Objects.requireNonNull(source, "source");
    }

    @Override
    public String uniqueId() {
        if (source instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return "velocity:" + name();
    }

    @Override
    public String name() {
        if (source instanceof Player player) {
            return player.getUsername();
        }
        if (source instanceof ConsoleCommandSource) {
            return "Console";
        }
        return source.getClass().getSimpleName();
    }

    @Override
    public ActorKind kind() {
        return switch (source) {
            case Player ignored -> ActorKind.PLAYER;
            case ConsoleCommandSource ignored -> ActorKind.CONSOLE;
            default -> ActorKind.OTHER;
        };
    }

    @Override
    public boolean hasPermission(String permission) {
        Objects.requireNonNull(permission, "permission");
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        Objects.requireNonNull(message, "message");
        source.sendMessage(Component.text(message));
    }
}
