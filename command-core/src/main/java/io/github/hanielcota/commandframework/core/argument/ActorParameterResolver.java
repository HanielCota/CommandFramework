package io.github.hanielcota.commandframework.core.argument;

import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.ParameterParseContext;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import java.util.Objects;

public final class ActorParameterResolver implements ParameterResolver<CommandActor> {

    @Override
    public Class<CommandActor> type() {
        return CommandActor.class;
    }

    @Override
    public boolean consumesInput() {
        return false;
    }

    @Override
    public ParseResult<CommandActor> resolve(ParameterParseContext context) {
        Objects.requireNonNull(context, "context");
        return ParseResult.success(context.commandContext().actor(), 0);
    }
}
