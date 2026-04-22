package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import io.github.hanielcota.commandframework.core.SuggestionContext;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public record BooleanArgumentResolver(Class<Boolean> type) implements ArgumentResolver<Boolean> {

    public BooleanArgumentResolver {
        Objects.requireNonNull(type, "type");
    }

    @Override
    public ParseResult<Boolean> parse(ArgumentInput input) {
        Objects.requireNonNull(input, "input");
        if ("true".equalsIgnoreCase(input.rawValue())) {
            return ParseResult.success(Boolean.TRUE, 1);
        }
        if ("false".equalsIgnoreCase(input.rawValue())) {
            return ParseResult.success(Boolean.FALSE, 1);
        }
        return ParseResult.failure(input.rawValue(), "true|false");
    }

    @Override
    public List<String> suggest(SuggestionContext context) {
        Objects.requireNonNull(context, "context");
        return Stream.of("true", "false")
                .filter(value -> value.startsWith(context.currentInput().toLowerCase(Locale.ROOT)))
                .toList();
    }
}
