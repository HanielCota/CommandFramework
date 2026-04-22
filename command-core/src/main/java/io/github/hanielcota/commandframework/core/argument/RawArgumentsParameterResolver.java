package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ParameterParseContext;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.List;
import java.util.Objects;

public final class RawArgumentsParameterResolver implements ParameterResolver<String[]> {

    @Override
    public Class<String[]> type() {
        return String[].class;
    }

    @Override
    public boolean consumesInput() {
        return true;
    }

    @Override
    public ParseResult<String[]> resolve(ParameterParseContext context) {
        Objects.requireNonNull(context, "context");
        List<String> remaining = context.arguments().subList(context.index(), context.arguments().size());
        return ParseResult.success(remaining.toArray(String[]::new), remaining.size());
    }
}
