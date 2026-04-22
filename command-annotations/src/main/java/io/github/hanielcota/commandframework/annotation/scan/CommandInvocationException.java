package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.core.CommandException;
import java.util.Objects;

final class CommandInvocationException extends CommandException {

    CommandInvocationException(String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message"), Objects.requireNonNull(cause, "cause"));
    }
}
