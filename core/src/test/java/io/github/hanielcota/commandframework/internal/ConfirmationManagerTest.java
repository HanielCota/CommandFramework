package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmationManagerTest {

    private static final String COMMAND_NAME = "confirm";

    @Test
    void putThenConsumeReturnsInvocation() {
        ConfirmationManager manager = new ConfirmationManager();
        CommandActor actor = actor(UUID.randomUUID());
        ConfirmDefinition confirm = new ConfirmDefinition(Duration.ofSeconds(5), COMMAND_NAME);
        PreparedInvocation invocation = invocationWithConfirm(confirm);

        manager.put(actor, invocation, confirm);
        PreparedInvocation consumed = manager.consume(actor, COMMAND_NAME);

        assertSame(invocation, consumed);
    }

    @Test
    void consumeWithoutPendingReturnsNull() {
        ConfirmationManager manager = new ConfirmationManager();
        CommandActor actor = actor(UUID.randomUUID());

        PreparedInvocation consumed = manager.consume(actor, COMMAND_NAME);

        assertNull(consumed);
    }

    @Test
    void consumeExpiredReturnsNull() throws InterruptedException {
        // TODO(F-02): inject Clock to replace this sleep-based timing assertion.
        ConfirmationManager manager = new ConfirmationManager();
        CommandActor actor = actor(UUID.randomUUID());
        ConfirmDefinition confirm = new ConfirmDefinition(Duration.ofMillis(50), COMMAND_NAME);
        PreparedInvocation invocation = invocationWithConfirm(confirm);

        manager.put(actor, invocation, confirm);
        Thread.sleep(60L);
        PreparedInvocation consumed = manager.consume(actor, COMMAND_NAME);

        assertNull(consumed);
    }

    @Test
    void differentActorsDoNotShareConfirmation() {
        ConfirmationManager manager = new ConfirmationManager();
        CommandActor alice = actor(UUID.randomUUID());
        CommandActor bob = actor(UUID.randomUUID());
        ConfirmDefinition confirm = new ConfirmDefinition(Duration.ofSeconds(5), COMMAND_NAME);
        PreparedInvocation invocation = invocationWithConfirm(confirm);

        manager.put(alice, invocation, confirm);
        PreparedInvocation consumed = manager.consume(bob, COMMAND_NAME);

        assertNull(consumed);
    }

    private static CommandActor actor(UUID id) {
        CommandActor actor = mock(CommandActor.class);
        when(actor.uniqueId()).thenReturn(id);
        return actor;
    }

    private static PreparedInvocation invocationWithConfirm(ConfirmDefinition confirm) {
        ExecutorDefinition executor = new ExecutorDefinition(
                (instance, arguments) -> null,
                "",
                "",
                "",
                false,
                false,
                null,
                confirm,
                List.of());
        CommandDefinition command = new CommandDefinition(
                new Object(),
                "test",
                List.of(),
                "",
                executor,
                Map.of(),
                Set.of());
        return new PreparedInvocation(command, executor, command.name(), command.name(), List.of());
    }
}
