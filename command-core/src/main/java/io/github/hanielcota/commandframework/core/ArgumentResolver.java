package io.github.hanielcota.commandframework.core;

import java.util.List;

/**
 * Resolves a single argument token into a typed value.
 *
 * @param <T> the type this resolver produces
 * @see io.github.hanielcota.commandframework.core.argument.SingleArgumentParameterResolver
 */
public interface ArgumentResolver<T> {

    /** The Java type produced by this resolver. */
    Class<T> type();

    /**
     * Parses a single raw argument token.
     *
     * @param input the raw argument together with the parameter name; never {@code null}
     * @return a successful or failed parse result; never {@code null}
     */
    ParseResult<T> parse(ArgumentInput input);

    /**
     * Returns tab-completion suggestions for this argument type.
     *
     * @param context the suggestion context; never {@code null}
     * @return a list of suggestions; never {@code null}
     */
    default List<String> suggest(SuggestionContext context) {
        return List.of();
    }
}
