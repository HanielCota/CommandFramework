package io.github.hanielcota.commandframework.paper;

public final class CommandUnregisterException extends RuntimeException {

    public CommandUnregisterException(String message) {
        super(message);
    }

    public CommandUnregisterException(String message, Throwable cause) {
        super(message, cause);
    }
}
