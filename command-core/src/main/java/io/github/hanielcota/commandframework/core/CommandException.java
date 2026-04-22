package io.github.hanielcota.commandframework.core;

import java.util.Objects;

/**
 * Base exception for all framework-specific runtime failures.
 *
 * <p>Extends {@link RuntimeException} because command failures are generally
 * unrecoverable at the call-site and should propagate to the platform adapter
 * or be caught for centralized error handling.</p>
 */
public class CommandException extends RuntimeException {

    public CommandException(String message) {
        super(Objects.requireNonNull(message, "message"));
    }

    public CommandException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message"), Objects.requireNonNull(cause, "cause"));
    }
}
