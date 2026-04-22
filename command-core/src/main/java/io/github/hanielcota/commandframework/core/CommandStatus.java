package io.github.hanielcota.commandframework.core;

/**
 * Outcome status of a command dispatch.
 */
public enum CommandStatus {
    SUCCESS,
    ACCEPTED,
    FAILURE,
    NOT_FOUND,
    NO_PERMISSION,
    INVALID_SENDER,
    COOLDOWN,
    RATE_LIMITED,
    INVALID_USAGE,
    ERROR
}
