package io.github.hanielcota.commandframework.core;

/**
 * Interceptor that hooks into the command dispatch pipeline.
 *
 * <p>Interceptors are applied in registration order: all {@code before}
 * methods run first (left-to-right), then the executor runs, then all
 * {@code after} methods run (also left-to-right, each receiving the result
 * of the previous interceptor).</p>
 */
public interface CommandInterceptor {

    /**
     * Called before the executor runs.
     *
     * @param context the dispatch context
     * @return a successful result to continue, or a failed result to abort
     */
    default CommandResult before(CommandContext context) {
        return CommandResult.success();
    }

    /**
     * Called after the executor runs.
     *
     * @param context the dispatch context
     * @param result  the result returned by the executor or previous interceptor
     * @return the (possibly mutated) result
     */
    default CommandResult after(CommandContext context, CommandResult result) {
        return result;
    }
}
