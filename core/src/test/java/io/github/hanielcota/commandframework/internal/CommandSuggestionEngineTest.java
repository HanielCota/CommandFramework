package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.MessageKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommandSuggestionEngineTest {

    @Test
    void suggestEmptyForUnknownLabel() {
        CommandSuggestionEngine engine = engine(Map.of(), CommandSuggestionEngineTest::unusedResolver);

        List<String> suggestions = engine.suggest(mock(CommandActor.class), "missing", "");

        assertEquals(List.of(), suggestions);
    }

    @Test
    void suggestSwallowsExceptionsReturnsEmpty() {
        CommandDefinition command = singleArgRootCommand();
        CommandSuggestionEngine engine = engine(Map.of("crash", command), CommandSuggestionEngineTest::throwingResolver);

        List<String> suggestions = engine.suggest(mock(CommandActor.class), "crash", "a");

        assertEquals(List.of(), suggestions);
    }

    @Test
    void emitDidYouMeanSendsMessageWithinThreshold() {
        CommandDefinition command = commandWithSubcommand("punish", "ban");
        MessageService messages = mock(MessageService.class);
        CommandSuggestionEngine engine = new CommandSuggestionEngine(
                Map.of("punish", command),
                new CommandTokenizer(),
                messages,
                mock(FrameworkLogger.class),
                CommandSuggestionEngineTest::allowAll,
                CommandSuggestionEngineTest::unusedResolver
        );
        CommandActor actor = mock(CommandActor.class);
        TokenizedInput tokenizedInput = new TokenizedInput(List.of("bam"), false);

        engine.emitDidYouMean(actor, command, tokenizedInput);

        verify(messages).send(actor, MessageKey.UNKNOWN_SUBCOMMAND, Map.of(
                "typed", "bam",
                "command", "punish",
                "suggestion", "ban"));
    }

    private static CommandSuggestionEngine engine(
            Map<String, CommandDefinition> commandsByLabel,
            Function<Class<?>, ArgumentResolver<Object>> resolverLookup
    ) {
        return new CommandSuggestionEngine(
                commandsByLabel,
                new CommandTokenizer(),
                mock(MessageService.class),
                mock(FrameworkLogger.class),
                CommandSuggestionEngineTest::allowAll,
                resolverLookup
        );
    }

    private static boolean allowAll(CommandActor actor, ExecutorDefinition executor) {
        java.util.Objects.requireNonNull(actor);
        java.util.Objects.requireNonNull(executor);
        return true;
    }

    private static ArgumentResolver<Object> unusedResolver(Class<?> type) {
        throw new AssertionError("resolver lookup should not be invoked: " + type);
    }

    private static ArgumentResolver<Object> throwingResolver(Class<?> type) {
        java.util.Objects.requireNonNull(type);
        throw new IllegalStateException("boom");
    }

    private static CommandDefinition singleArgRootCommand() {
        ParameterDefinition argument = new ParameterDefinition(
                "value", String.class, String.class, false, false, false, "", false, 64);
        ExecutorDefinition root = new ExecutorDefinition(
                (instance, arguments) -> null,
                "",
                "",
                "",
                false,
                false,
                null,
                null,
                List.of(argument));
        return new CommandDefinition(
                new Object(),
                "crash",
                List.of(),
                "",
                root,
                Map.of(),
                Set.of());
    }

    private static CommandDefinition commandWithSubcommand(String name, String subcommand) {
        ExecutorDefinition executor = new ExecutorDefinition(
                (instance, arguments) -> null,
                subcommand,
                "",
                "",
                false,
                false,
                null,
                null,
                List.of());
        return new CommandDefinition(
                new Object(),
                name,
                List.of(),
                "",
                null,
                Map.of(subcommand, executor),
                Set.of());
    }
}
