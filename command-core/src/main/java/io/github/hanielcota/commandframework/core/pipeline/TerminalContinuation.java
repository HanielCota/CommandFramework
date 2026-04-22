package io.github.hanielcota.commandframework.core.pipeline;

import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandResult;
import java.util.Objects;

/**
 * Terminal continuation that returns a success result.
 */
public final class TerminalContinuation implements DispatchContinuation {

    public static final TerminalContinuation INSTANCE = new TerminalContinuation();

    private TerminalContinuation() {
    }

    @Override
    public CommandResult proceed(CommandContext context) {
        Objects.requireNonNull(context, "context");
        return CommandResult.success();
    }
}
