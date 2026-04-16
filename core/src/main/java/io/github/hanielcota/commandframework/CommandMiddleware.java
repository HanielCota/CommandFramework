package io.github.hanielcota.commandframework;

/**
 * Intercepts command execution before the framework pipeline runs.
 */
@FunctionalInterface
public interface CommandMiddleware {

    /**
     * Handles the current command context.
     *
     * @param context the immutable command context
     * @param chain   the remaining middleware chain
     * @return the command result
     */
    CommandResult handle(CommandContext context, Chain chain);

    /**
     * Represents the remaining middleware chain.
     */
    @FunctionalInterface
    interface Chain {

        /**
         * Continues the command pipeline.
         *
         * @param context the immutable command context
         * @return the command result
         */
        CommandResult proceed(CommandContext context);
    }
}
