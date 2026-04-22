package io.github.hanielcota.commandframework.core.pipeline;

import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandResult;

/**
 * A single stage in the command dispatch pipeline.
 */
@FunctionalInterface
public interface CommandDispatchStage {

    /**
     * Processes the context and either returns a result or delegates to the next stage.
     *
     * @param context      the command context
     * @param continuation the remaining stages
     * @return the command result
     */
    CommandResult process(CommandContext context, DispatchContinuation continuation);
}
