package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Objects;

/**
 * Describes a single parameter of a command route.
 *
 * @param <T> the Java type this parameter resolves to
 */
public record CommandParameter<T>(
        String name,
        Class<T> type,
        ParameterResolver<T> resolver,
        boolean visibleInUsage
) {

    public CommandParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(resolver, "resolver");
    }

    /** Whether this parameter consumes tokens from the argument list. */
    public boolean consumesInput() {
        return resolver.consumesInput();
    }

    /** Delegates to the underlying {@link ParameterResolver}. */
    public ParseResult<T> resolve(ParameterParseContext context) {
        return resolver.resolve(context);
    }

    /** Delegates to the underlying {@link ParameterResolver}. */
    public List<String> suggest(ParameterSuggestionContext context) {
        return resolver.suggest(context);
    }
}
