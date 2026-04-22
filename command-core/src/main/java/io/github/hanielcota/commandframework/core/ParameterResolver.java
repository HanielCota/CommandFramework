package io.github.hanielcota.commandframework.core;

import java.util.List;

/**
 * Resolves a method parameter from the raw command arguments.
 *
 * @param <T> the type this resolver produces
 */
public interface ParameterResolver<T> {

    /** The Java type produced by this resolver. */
    Class<T> type();

    /**
     * Whether this resolver consumes tokens from the argument list.
     * Return {@code false} for injected values such as {@link CommandActor}.
     */
    boolean consumesInput();

    /**
     * Attempts to resolve the value from the current parse context.
     *
     * @param context the parse context; never {@code null}
     * @return a successful or failed parse result; never {@code null}
     */
    ParseResult<T> resolve(ParameterParseContext context);

    /**
     * Returns tab-completion suggestions for this parameter.
     *
     * @param context the suggestion context; never {@code null}
     * @return a list of suggestions; never {@code null}
     */
    default List<String> suggest(ParameterSuggestionContext context) {
        return List.of();
    }
}
