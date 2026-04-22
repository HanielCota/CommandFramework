package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Objects;

public record ParameterParseContext(
        CommandContext commandContext,
        CommandParameter<?> parameter,
        List<String> arguments,
        int index
) {

    public ParameterParseContext(
            CommandContext commandContext,
            CommandParameter<?> parameter,
            List<String> arguments,
            int index
    ) {
        this.commandContext = Objects.requireNonNull(commandContext, "commandContext");
        this.parameter = Objects.requireNonNull(parameter, "parameter");
        this.arguments = List.copyOf(arguments);
        if (index < 0) {
            throw new IllegalArgumentException("Invalid argument index: expected zero or positive");
        }
        this.index = index;
    }
}
