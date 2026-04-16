package io.github.hanielcota.commandframework;

import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Represents a platform-neutral command sender.
 */
public interface CommandActor {

    /**
     * Returns the sender display name.
     *
     * @return the sender display name
     */
    String name();

    /**
     * Returns the sender identity.
     *
     * <p>Platform integrations should return a stable UUID. Player-based features such as confirmations
     * are keyed by this value.</p>
     *
     * @return the stable sender identity
     */
    UUID uniqueId();

    /**
     * Returns whether the sender is a player.
     *
     * @return {@code true} when the sender is a player
     */
    boolean isPlayer();

    /**
     * Returns whether the sender currently has the supplied permission.
     *
     * @param permission the permission to test
     * @return {@code true} when the permission is granted
     */
    boolean hasPermission(String permission);

    /**
     * Sends a message to the sender.
     *
     * <p>Implementations should silently ignore delivery when the sender is no longer available.</p>
     *
     * @param message the message to send
     */
    void sendMessage(Component message);

    /**
     * Returns whether the sender is still available for framework-managed interactions.
     *
     * @return {@code true} when the sender can still receive framework-managed messages
     */
    boolean isAvailable();

    /**
     * Returns the native platform sender.
     *
     * @return the platform sender
     */
    Object platformSender();
}
