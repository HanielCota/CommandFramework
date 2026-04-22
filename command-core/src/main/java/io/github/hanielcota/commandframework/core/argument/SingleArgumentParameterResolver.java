package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.ArgumentResolver;
import io.github.hanielcota.commandframework.core.ParameterParseContext;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParameterSuggestionContext;
import io.github.hanielcota.commandframework.core.ParseResult;
import io.github.hanielcota.commandframework.core.SuggestionContext;
import java.util.List;
import java.util.Objects;

public record SingleArgumentParameterResolver<T>(ArgumentResolver<T> resolver) implements ParameterResolver<T> {

    public SingleArgumentParameterResolver {
        Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public Class<T> type() {
        return resolver.type();
    }

    @Override
    public boolean consumesInput() {
        return true;
    }

    @Override
    public ParseResult<T> resolve(ParameterParseContext context) {
        Objects.requireNonNull(context, "context");
        if (context.index() >= context.arguments().size()) {
            return ParseResult.failure("", context.parameter().name());
        }
        String value = context.arguments().get(context.index());
        return resolver.parse(new ArgumentInput(value, context.parameter().name()));
    }

    @Override
    public List<String> suggest(ParameterSuggestionContext context) {
        Objects.requireNonNull(context, "context");
        SuggestionContext suggestion = new SuggestionContext(
                context.actor(), context.route(), context.currentInput(), context.arguments()
        );
        return resolver.suggest(suggestion);
    }
}
