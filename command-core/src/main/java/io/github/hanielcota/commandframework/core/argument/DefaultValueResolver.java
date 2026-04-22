package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.ParameterParseContext;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.List;
import java.util.Objects;

/**
 * Wraps another resolver and returns a default value when the caller omits
 * the argument.
 */
public final class DefaultValueResolver<T> implements ParameterResolver<T> {

    private final ParameterResolver<T> delegate;
    private final String defaultValue;

    public DefaultValueResolver(ParameterResolver<T> delegate, String defaultValue) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    @Override
    public Class<T> type() {
        return delegate.type();
    }

    @Override
    public boolean consumesInput() {
        return delegate.consumesInput();
    }

    @Override
    public ParseResult<T> resolve(ParameterParseContext context) {
        Objects.requireNonNull(context, "context");
        if (context.index() >= context.arguments().size()) {
            ParameterParseContext synthetic = new ParameterParseContext(
                    context.commandContext(), context.parameter(), List.of(defaultValue), 0);
            return delegate.resolve(synthetic);
        }
        return delegate.resolve(context);
    }
}
