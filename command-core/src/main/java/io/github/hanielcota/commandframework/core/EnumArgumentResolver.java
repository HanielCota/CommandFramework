package io.github.hanielcota.commandframework.core;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record EnumArgumentResolver<T>(Class<T> enumType) implements ArgumentResolver<T> {

    public EnumArgumentResolver {
        Objects.requireNonNull(enumType, "enumType");
        if (!enumType.isEnum()) {
            throw new IllegalArgumentException("Expected enum type, got: " + enumType.getName());
        }
    }

    @Override
    public Class<T> type() {
        return enumType;
    }

    @Override
    public ParseResult<T> parse(ArgumentInput input) {
        Objects.requireNonNull(input, "input");
        T[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return ParseResult.failure(input.rawValue(), expectedValues());
        }
        for (T constant : constants) {
            if (constantName(constant).equalsIgnoreCase(input.rawValue())) {
                return ParseResult.success(constant, 1);
            }
        }
        return ParseResult.failure(input.rawValue(), expectedValues());
    }

    @Override
    public List<String> suggest(SuggestionContext context) {
        Objects.requireNonNull(context, "context");
        String prefix = context.currentInput().toLowerCase(Locale.ROOT);
        T[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return List.of();
        }
        return Arrays.stream(constants)
                .map(this::constantName)
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private String expectedValues() {
        T[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return "";
        }
        return String.join("|", Arrays.stream(constants).map(this::constantName).toList());
    }

    private String constantName(T constant) {
        return ((Enum<?>) constant).name().toLowerCase(Locale.ROOT);
    }
}
