package io.github.hanielcota.commandframework.core;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of a command execution or pipeline stage.
 *
 * @param status     the outcome status
 * @param detailText optional human-readable detail
 * @param retryAfter cooldown or rate-limit retry duration
 */
public record CommandResult(
        CommandStatus status,
        String detailText,
        Duration retryAfter
) {

    private static final CommandResult SUCCESS = new CommandResult(CommandStatus.SUCCESS, "", Duration.ZERO);

    public CommandResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detailText, "detailText");
        Objects.requireNonNull(retryAfter, "retryAfter");
        if (retryAfter.isNegative()) {
            throw new IllegalArgumentException("Invalid retry duration: expected zero or positive");
        }
    }

    /** A shared successful result instance. */
    public static CommandResult success() {
        return SUCCESS;
    }

    /** Creates an accepted result for asynchronous dispatch. */
    public static CommandResult accepted() {
        return new CommandResult(CommandStatus.ACCEPTED, "", Duration.ZERO);
    }

    /** Creates a failure result without detail text. */
    public static CommandResult failure(CommandStatus status) {
        return new CommandResult(status, "", Duration.ZERO);
    }

    /** Creates a failure result with detail text. */
    public static CommandResult failure(CommandStatus status, String detail) {
        return new CommandResult(status, detail, Duration.ZERO);
    }

    /** Creates a cooldown result with the remaining duration. */
    public static CommandResult cooldown(Duration remaining) {
        return new CommandResult(CommandStatus.COOLDOWN, "", remaining);
    }

    /** Whether this result represents a successful execution. */
    public boolean isSuccess() {
        return status == CommandStatus.SUCCESS;
    }

    /** Returns the detail text if non-blank. */
    public Optional<String> detail() {
        if (detailText.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(detailText);
    }
}
