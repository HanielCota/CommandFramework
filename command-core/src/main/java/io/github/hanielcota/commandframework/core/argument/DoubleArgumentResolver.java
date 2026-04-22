package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.Objects;

public record DoubleArgumentResolver(Class<Double> type) implements ArgumentResolver<Double> {

    public DoubleArgumentResolver {
        Objects.requireNonNull(type, "type");
    }

    @Override
    public ParseResult<Double> parse(ArgumentInput input) {
        Objects.requireNonNull(input, "input");
        try {
            return ParseResult.success(Double.parseDouble(input.rawValue()), 1);
        } catch (NumberFormatException ignored) {
            return ParseResult.failure(input.rawValue(), "decimal");
        }
    }
}
