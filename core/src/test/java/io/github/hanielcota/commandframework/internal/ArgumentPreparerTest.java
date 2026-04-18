package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.PlatformBridge;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgumentPreparerTest {

    @Test
    void prepareThrowsMissingForRequiredPositional() {
        ParameterDefinition parameter = new ParameterDefinition(
                "name", String.class, String.class, false, false, false, "", false, 64);
        CommandDefinition command = commandWith(rootExecutor(List.of(parameter)));
        Selection selection = new Selection(command.rootExecutor(), List.of(), command.name());
        ArgumentPreparer preparer = new ArgumentPreparer(stringResolvers(), bridgeWithPlayerType(null));

        ArgumentPreparer.MissingArgumentException exception = assertThrows(
                ArgumentPreparer.MissingArgumentException.class,
                () -> preparer.prepare(mock(CommandActor.class), command, command.name(), selection));
        assertEquals("name", exception.argumentName);
    }

    @Test
    void prepareThrowsTooManyWhenOverflow() {
        ParameterDefinition parameter = new ParameterDefinition(
                "value", String.class, String.class, false, false, false, "", false, 64);
        CommandDefinition command = commandWith(rootExecutor(List.of(parameter)));
        Selection selection = new Selection(command.rootExecutor(), List.of("first", "second"), command.name());
        ArgumentPreparer preparer = new ArgumentPreparer(stringResolvers(), bridgeWithPlayerType(null));

        assertThrows(
                ArgumentPreparer.TooManyArgumentsException.class,
                () -> preparer.prepare(mock(CommandActor.class), command, command.name(), selection));
    }

    @Test
    void bindArgumentsMapsSenderByType() {
        ParameterDefinition senderParameter = new ParameterDefinition(
                "actor", CommandActor.class, CommandActor.class, true, false, false, "", false, 64);
        ExecutorDefinition executor = rootExecutor(List.of(senderParameter));
        CommandDefinition command = commandWith(executor);
        Selection selection = new Selection(executor, List.of(), command.name());
        ArgumentPreparer preparer = new ArgumentPreparer(stringResolvers(), bridgeWithPlayerType(null));
        CommandActor actor = mock(CommandActor.class);
        when(actor.platformSender()).thenReturn(new Object());

        PreparedInvocation invocation = preparer.prepare(actor, command, command.name(), selection);
        Object[] arguments = preparer.bindArguments(actor, invocation);

        assertEquals(1, arguments.length);
        assertSame(actor, arguments[0]);
    }

    @Test
    void bindArgumentsThrowsPlayerOnlySignalForPlayerSenderFromConsole() {
        ParameterDefinition senderParameter = new ParameterDefinition(
                "player", PlayerStub.class, PlayerStub.class, true, false, false, "", false, 64);
        ExecutorDefinition executor = rootExecutor(List.of(senderParameter));
        CommandDefinition command = commandWith(executor);
        Selection selection = new Selection(executor, List.of(), command.name());
        ArgumentPreparer preparer = new ArgumentPreparer(stringResolvers(), bridgeWithPlayerType(PlayerStub.class));
        CommandActor actor = mock(CommandActor.class);
        when(actor.platformSender()).thenReturn("console");

        PreparedInvocation invocation = preparer.prepare(actor, command, command.name(), selection);

        assertThrows(
                ArgumentPreparer.PlayerOnlySignal.class,
                () -> preparer.bindArguments(actor, invocation));
    }

    private static Map<Class<?>, ArgumentResolver<?>> stringResolvers() {
        return Map.of(String.class, new ArgumentResolver<String>() {
            @Override
            public Class<String> type() {
                return String.class;
            }

            @Override
            public String resolve(io.github.hanielcota.commandframework.ArgumentResolutionContext context, String input) {
                return input;
            }
        });
    }

    private static PlatformBridge<Object> bridgeWithPlayerType(Class<?> playerType) {
        @SuppressWarnings("unchecked")
        PlatformBridge<Object> bridge = mock(PlatformBridge.class);
        if (playerType != null) {
            when(bridge.isPlayerSenderType(playerType)).thenReturn(true);
        }
        return bridge;
    }

    private static ExecutorDefinition rootExecutor(List<ParameterDefinition> parameters) {
        return new ExecutorDefinition(
                (instance, arguments) -> null,
                "",
                "",
                "",
                false,
                false,
                null,
                null,
                parameters);
    }

    private static CommandDefinition commandWith(ExecutorDefinition rootExecutor) {
        return new CommandDefinition(
                new Object(),
                "test",
                List.of(),
                "",
                rootExecutor,
                Map.of(),
                Set.of());
    }

    private static final class PlayerStub {
        private PlayerStub() {
        }
    }
}
