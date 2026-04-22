package io.github.hanielcota.commandframework.core;

/**
 * Represents the entity executing a command.
 *
 * <p>This abstraction decouples the framework from platform-specific sender
 * types (e.g. Bukkit {@code CommandSender}, Velocity {@code CommandSource}).
 * All implementations must be thread-safe for read operations because platform
 * adapters may cache actors in concurrent structures.</p>
 */
public interface CommandActor {

    /**
     * Returns a stable identifier for this actor.
     *
     * <p>For players this should be the UUID string; for console or other
     * senders it should be a deterministic synthetic id.</p>
     */
    String uniqueId();

    /** Returns the display name of this actor. */
    String name();

    /** Returns the kind of actor (player, console, or other). */
    ActorKind kind();

    /**
     * Checks whether this actor has the given permission.
     *
     * @param permission the permission node; never {@code null}
     */
    boolean hasPermission(String permission);

    /**
     * Sends a plain-text message to this actor.
     *
     * @param message the message to send; never {@code null}
     */
    void sendMessage(String message);

    /** Convenience method equivalent to {@code kind() == ActorKind.PLAYER}. */
    default boolean isPlayer() {
        return kind() == ActorKind.PLAYER;
    }

    /** Convenience method equivalent to {@code kind() == ActorKind.CONSOLE}. */
    default boolean isConsole() {
        return kind() == ActorKind.CONSOLE;
    }
}
