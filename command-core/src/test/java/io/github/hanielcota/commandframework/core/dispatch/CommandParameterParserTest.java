package io.github.hanielcota.commandframework.core.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandParameter;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.TestActor;
import io.github.hanielcota.commandframework.core.argument.SingleArgumentParameterResolver;
import io.github.hanielcota.commandframework.core.argument.StringArgumentResolver;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandParameterParserTest {

    @Test
    void rejectsMissingRequiredParameter() {
        CommandRoute route = CommandRoute.builder("kit", (context, parameters) -> CommandResult.success())
                .parameters(List.of(stringParameter("target")))
                .build();
        CommandContext context = new CommandContext(new TestActor(ActorKind.PLAYER), route, "kit", List.of());

        ParameterParseOutcome outcome = new CommandParameterParser().parse(context);

        assertFalse(outcome.isSuccess());
        ParameterParseFailure failure = outcome.failure().orElseThrow();
        assertEquals("", failure.invalidValue());
        assertEquals("target", failure.expectedValue());
    }

    private CommandParameter<String> stringParameter(String name) {
        return new CommandParameter<>(
                name,
                String.class,
                new SingleArgumentParameterResolver<>(new StringArgumentResolver()),
                true
        );
    }
}
