package io.github.hanielcota.commandframework;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the outcome of a command execution.
 */
public sealed interface CommandResult permits
        CommandResult.CooldownActive,
        CommandResult.Failure,
        CommandResult.Handled,
        CommandResult.HelpShown,
        CommandResult.InvalidArgs,
        CommandResult.NoPermission,
        CommandResult.PendingConfirmation,
        CommandResult.PlayerOnly,
        CommandResult.RateLimited,
        CommandResult.Success {

    /**
     * Returns a success result.
     *
     * @return the success result
     */
    static Success success() {
        return Success.INSTANCE;
    }

    /**
     * Returns a handled result.
     *
     * @return the handled result
     */
    static Handled handled() {
        return Handled.INSTANCE;
    }

    /**
     * Returns a generic failure result.
     *
     * @param key the message key
     * @return the failure result
     */
    static Failure failure(MessageKey key) {
        return new Failure(key, Map.of());
    }

    /**
     * Returns a generic failure result with placeholders.
     *
     * @param key          the message key
     * @param placeholders the message placeholders
     * @return the failure result
     */
    static Failure failure(MessageKey key, Map<String, String> placeholders) {
        return new Failure(key, placeholders);
    }

    /**
     * A successful command outcome.
     */
    record Success() implements CommandResult {
        private static final Success INSTANCE = new Success();
    }

    /**
     * A handled command outcome where no additional framework action is required.
     */
    record Handled() implements CommandResult {
        private static final Handled INSTANCE = new Handled();
    }

    /**
     * A command outcome that maps to a message key.
     *
     * @param key          the message key
     * @param placeholders the placeholder values
     */
    record Failure(MessageKey key, Map<String, String> placeholders) implements CommandResult {
        public Failure {
            Objects.requireNonNull(key, "key");
            placeholders = Map.copyOf(Objects.requireNonNull(placeholders, "placeholders"));
        }
    }

    /**
     * Signals invalid arguments.
     *
     * @param argumentName the invalid argument name
     * @param input        the invalid input
     */
    record InvalidArgs(String argumentName, String input) implements CommandResult {
        public InvalidArgs {
            Objects.requireNonNull(argumentName, "argumentName");
            Objects.requireNonNull(input, "input");
        }
    }

    /**
     * Signals a permission failure.
     *
     * @param permission the missing permission
     */
    record NoPermission(String permission) implements CommandResult {
        public NoPermission {
            Objects.requireNonNull(permission, "permission");
        }
    }

    /**
     * Signals that the command is player-only.
     */
    record PlayerOnly() implements CommandResult {
    }

    /**
     * Signals that the command is currently on cooldown.
     *
     * @param remaining the remaining cooldown
     */
    record CooldownActive(Duration remaining) implements CommandResult {
        public CooldownActive {
            Objects.requireNonNull(remaining, "remaining");
        }
    }

    /**
     * Signals that the command is pending explicit confirmation.
     *
     * @param commandName the confirmation command name
     * @param expiresIn   the remaining confirmation window
     */
    record PendingConfirmation(String commandName, Duration expiresIn) implements CommandResult {
        public PendingConfirmation {
            Objects.requireNonNull(commandName, "commandName");
            Objects.requireNonNull(expiresIn, "expiresIn");
        }
    }

    /**
     * Signals that the command help was displayed.
     */
    record HelpShown() implements CommandResult {
    }

    /**
     * Signals that the execution was silently rate-limited.
     */
    record RateLimited() implements CommandResult {
    }
}
