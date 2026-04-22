package io.github.hanielcota.commandframework.core;

import java.util.List;

/**
 * Functional interface for the business logic of a command route.
 *
 * <p>Implementations receive the fully parsed context and a list of
 * {@link ParsedParameter} values. They must return a {@link CommandResult}
 * indicating success or a specific failure status.</p>
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Executes the command logic.
     *
     * @param context    the dispatch context; never {@code null}
     * @param parameters the parsed parameter values; never {@code null}
     * @return the result of execution; never {@code null}
     */
    CommandResult execute(CommandContext context, List<ParsedParameter<?>> parameters);
}
