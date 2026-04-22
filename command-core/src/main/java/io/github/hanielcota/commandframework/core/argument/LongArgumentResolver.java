package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.Objects;

public record LongArgumentResolver(Class<Long> type) implements ArgumentResolver<Long> {

    public LongArgumentResolver {
        Objects.requireNonNull(type, "type");
    }

    @Override
    public ParseResult<Long> parse(ArgumentInput input) {
        Objects.requireNonNull(input, "input");
        try {
            return ParseResult.success(Long.parseLong(input.rawValue()), 1);
        } catch (NumberFormatException ignored) {
            return ParseResult.failure(input.rawValue(), "long");
        }
    }
}
