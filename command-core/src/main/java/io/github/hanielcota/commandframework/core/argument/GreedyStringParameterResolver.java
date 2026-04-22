package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ParameterParseContext;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.List;
import java.util.Objects;

/**
 * Resolves a {@link String} by consuming all remaining argument tokens joined
 * by a single space.
 */
public final class GreedyStringParameterResolver implements ParameterResolver<String> {

    @Override
    public Class<String> type() {
        return String.class;
    }

    @Override
    public boolean consumesInput() {
        return true;
    }

    @Override
    public ParseResult<String> resolve(ParameterParseContext context) {
        Objects.requireNonNull(context, "context");
        List<String> remaining = context.arguments().subList(context.index(), context.arguments().size());
        if (remaining.isEmpty()) {
            return ParseResult.failure("", context.parameter().name());
        }
        return ParseResult.success(String.join(" ", remaining), remaining.size());
    }
}
