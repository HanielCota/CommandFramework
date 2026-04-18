package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.MessageKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
}
