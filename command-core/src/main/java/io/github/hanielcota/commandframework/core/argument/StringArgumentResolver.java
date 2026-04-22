package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.Objects;

public final class StringArgumentResolver implements ArgumentResolver<String> {

    @Override
    public Class<String> type() {
        return String.class;
    }

    @Override
    public ParseResult<String> parse(ArgumentInput input) {
        Objects.requireNonNull(input, "input");
        return ParseResult.success(input.rawValue(), 1);
    }
}
