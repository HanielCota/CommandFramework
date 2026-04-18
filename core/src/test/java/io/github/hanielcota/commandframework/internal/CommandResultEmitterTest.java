package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.MessageKey;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandResultEmitterTest {

    @Test
    void emitSendsNoPermissionMessage() {
        MessageService messages = mock(MessageService.class);
        CommandResultEmitter emitter = new CommandResultEmitter(messages, (actor, executor) -> true);
        CommandActor actor = mock(CommandActor.class);

        emitter.emit(actor, new CommandResult.NoPermission("perm.foo"));

        verify(messages).send(actor, MessageKey.NO_PERMISSION);
    }

    @Test
    void emitSuppressesSuccessResult() {
        MessageService messages = mock(MessageService.class);
        CommandResultEmitter emitter = new CommandResultEmitter(messages, (actor, executor) -> true);
        CommandActor actor = mock(CommandActor.class);

        emitter.emit(actor, CommandResult.success());

        verify(messages, never()).send(any(), any(MessageKey.class));
        verify(messages, never()).send(any(), any(MessageKey.class), any());
    }

    @Test
    void emitSendsCooldownActiveWithFormattedRemaining() {
        MessageService messages = mock(MessageService.class);
        Duration remaining = Duration.ofSeconds(12);
        when(messages.formatDuration(remaining)).thenReturn("12s");
        CommandResultEmitter emitter = new CommandResultEmitter(messages, (actor, executor) -> true);
        CommandActor actor = mock(CommandActor.class);

        emitter.emit(actor, new CommandResult.CooldownActive(remaining));

        verify(messages).send(actor, MessageKey.COOLDOWN_ACTIVE, Map.of("remaining", "12s"));
    }

    @Test
    void emitSendsFailureWithKeyAndPlaceholders() {
        MessageService messages = mock(MessageService.class);
        CommandResultEmitter emitter = new CommandResultEmitter(messages, (actor, executor) -> true);
        CommandActor actor = mock(CommandActor.class);

        emitter.emit(actor, new CommandResult.Failure(MessageKey.COMMAND_ERROR, Map.of("foo", "bar")));

        verify(messages).send(actor, MessageKey.COMMAND_ERROR, Map.of("foo", "bar"));
    }

    @Test
    void emitSendsInvalidArgsWithNameAndInput() {
        MessageService messages = mock(MessageService.class);
        CommandResultEmitter emitter = new CommandResultEmitter(messages, (actor, executor) -> true);
        CommandActor actor = mock(CommandActor.class);

        emitter.emit(actor, new CommandResult.InvalidArgs("age", "abc"));

        verify(messages).send(actor, MessageKey.INVALID_ARGUMENT, Map.of("name", "age", "input", "abc"));
    }

    @Test
    void emitClampsPendingConfirmationSecondsToOne() {
        MessageService messages = mock(MessageService.class);
        CommandResultEmitter emitter = new CommandResultEmitter(messages, (actor, executor) -> true);
        CommandActor actor = mock(CommandActor.class);

        emitter.emit(actor, new CommandResult.PendingConfirmation("myconfirm", Duration.ofMillis(200)));

        verify(messages).send(actor, MessageKey.CONFIRM_PROMPT, Map.of("command", "myconfirm", "seconds", "1"));
    }

    @Test
    void sendHelpListsRootAndAllowedSubcommandsOnly() {
        MessageService messages = mock(MessageService.class);
        ExecutorDefinition rootExecutor = executor("", "root description");
        ExecutorDefinition allowedSubcommand = executor("allowed", "allowed description");
        ExecutorDefinition deniedSubcommand = executor("denied", "denied description");
        CommandDefinition command = new CommandDefinition(
                new Object(),
                "test",
                List.of(),
                "",
                rootExecutor,
                Map.of("allowed", allowedSubcommand, "denied", deniedSubcommand),
                Set.of());
        Component header = Component.text("header");
        Component rootEntry = Component.text("root");
        Component allowedEntry = Component.text("allowed");
        Component aggregated = Component.text("aggregated");
        when(messages.render(MessageKey.HELP_HEADER, Map.of("command", "test"))).thenReturn(header);
        when(messages.render(MessageKey.HELP_ENTRY, Map.of(
                "usage", "test",
                "description", "root description"))).thenReturn(rootEntry);
        when(messages.render(MessageKey.HELP_ENTRY, Map.of(
                "usage", "test allowed",
                "description", "allowed description"))).thenReturn(allowedEntry);
        when(messages.renderLines(any(Component[].class))).thenReturn(aggregated);
        CommandResultEmitter emitter = new CommandResultEmitter(
                messages,
                (actor, executor) -> !Objects.equals(executor, deniedSubcommand));
        CommandActor actor = mock(CommandActor.class);

        emitter.sendHelp(actor, command, "test");

        verify(messages, times(1)).render(MessageKey.HELP_HEADER, Map.of("command", "test"));
        verify(messages).render(MessageKey.HELP_ENTRY, Map.of(
                "usage", "test",
                "description", "root description"));
        verify(messages).render(MessageKey.HELP_ENTRY, Map.of(
                "usage", "test allowed",
                "description", "allowed description"));
        verify(messages, never()).render(MessageKey.HELP_ENTRY, Map.of(
                "usage", "test denied",
                "description", "denied description"));
        verify(actor, times(1)).sendMessage(aggregated);
    }

    private static ExecutorDefinition executor(String subcommand, String description) {
        return new ExecutorDefinition(
                (instance, arguments) -> null,
                subcommand,
                description,
                "",
                false,
                false,
                null,
                null,
                List.of());
    }
}
