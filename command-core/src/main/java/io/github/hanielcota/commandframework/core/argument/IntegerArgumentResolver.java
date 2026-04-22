package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.Objects;

public record IntegerArgumentResolver(Class<Integer> type) implements ArgumentResolver<Integer> {

    public IntegerArgumentResolver {
        Objects.requireNonNull(type, "type");
    }

    @Override
    public ParseResult<Integer> parse(ArgumentInput input) {
        Objects.requireNonNull(input, "input");
        try {
            return ParseResult.success(Integer.parseInt(input.rawValue()), 1);
        } catch (NumberFormatException ignored) {
            return ParseResult.failure(input.rawValue(), "integer");
        }
    }
}
