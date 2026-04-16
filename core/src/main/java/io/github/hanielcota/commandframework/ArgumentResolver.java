package io.github.hanielcota.commandframework;

import java.util.List;
import java.util.Objects;

/**
 * Resolves a command argument into a Java value.
 *
 * @param <T> the supported type
 */
public interface ArgumentResolver<T> {

    /**
     * Returns the raw Java type supported by this resolver.
     *
     * @return the supported raw type
     */
    Class<T> type();

    /**
     * Resolves the raw input into a Java value.
     *
     * @param context the resolution context
     * @param input   the raw argument input
     * @return the resolved value
     * @throws ArgumentResolveException when the input cannot be resolved
     */
    T resolve(ArgumentResolutionContext context, String input) throws ArgumentResolveException;

    /**
     * Returns tab-completion suggestions for the current partial input.
     *
     * @param actor        the sender actor
     * @param currentInput the current partial token
     * @return the suggestion list
     */
    default List<String> suggest(CommandActor actor, String currentInput) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(currentInput, "currentInput");
        return List.of();
    }
}
