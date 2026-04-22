package io.github.hanielcota.commandframework.core.pipeline;

import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandResult;

/**
 * Represents the remaining stages in the dispatch pipeline.
 */
@FunctionalInterface
public interface DispatchContinuation {

    CommandResult proceed(CommandContext context);

    static DispatchContinuation terminal() {
        return TerminalContinuation.INSTANCE;
    }
}
