package io.github.hanielcota.commandframework.core.dispatch;

import io.github.hanielcota.commandframework.core.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandParameterParser {

    public ParameterParseOutcome parse(CommandContext context) {
        Objects.requireNonNull(context, "context");
        List<ParsedParameter<?>> parsed = new ArrayList<>();
        int index = 0;
        for (CommandParameter<?> parameter : context.route().parameters()) {
            ParameterStep step = parseOne(context, parameter, index, parsed);
            if (!step.isSuccess()) {
                return ParameterParseOutcome.failure(step.invalidValue(), step.expectedValue());
            }
            index += step.consumedTokens();
        }
        return finish(context, index, parsed);
    }

    private <T> ParameterStep parseOne(CommandContext context, CommandParameter<T> parameter, int index, List<ParsedParameter<?>> parsed) {
        ParameterParseContext parseContext = new ParameterParseContext(context, parameter, context.arguments(), index);
        ParseResult<T> result = parameter.resolve(parseContext);
        if (result == null) {
            return ParameterStep.failure("null", "non-null parse result from resolver");
        }
        if (!result.isSuccess()) {
            return ParameterStep.failure(result.invalidValue(), result.expectedValue());
        }
        parsed.add(new ParsedParameter<>(parameter, result.value()));
        return ParameterStep.success(result.consumedTokens());
    }

    private ParameterParseOutcome finish(CommandContext context, int index, List<ParsedParameter<?>> parsed) {
        if (index < context.arguments().size()) {
            return ParameterParseOutcome.failure(context.arguments().get(index), "no extra arguments");
        }
        return ParameterParseOutcome.success(parsed);
    }

    private record ParameterStep(boolean success, int consumedTokens, String invalidValue, String expectedValue) {
        static ParameterStep success(int consumedTokens) {
            return new ParameterStep(true, consumedTokens, "", "");
        }

        static ParameterStep failure(String invalidValue, String expectedValue) {
            return new ParameterStep(false, 0, invalidValue, expectedValue);
        }

        boolean isSuccess() {
            return success;
        }
    }
}
