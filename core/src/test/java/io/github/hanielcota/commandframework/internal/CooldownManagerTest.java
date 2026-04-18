package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CooldownManagerTest {

    private static final String COMMAND_PATH = "test";
    private static final String OTHER_COMMAND_PATH = "other";

    @Test
    void firstInvocationIsAllowed() {
        CooldownManager manager = new CooldownManager();
        CommandActor actor = actor(UUID.randomUUID());
        CooldownDefinition cooldown = new CooldownDefinition(Duration.ofSeconds(5), "");

        CooldownManager.CooldownResult result = manager.checkAndConsume(COMMAND_PATH, actor, cooldown);

        assertTrue(result.allowed());
        assertEquals(Duration.ZERO, result.remaining());
    }

    @Test
    void secondInvocationWithinWindowIsBlocked() {
        CooldownManager manager = new CooldownManager();
        CommandActor actor = actor(UUID.randomUUID());
        CooldownDefinition cooldown = new CooldownDefinition(Duration.ofSeconds(5), "");

        manager.checkAndConsume(COMMAND_PATH, actor, cooldown);
        CooldownManager.CooldownResult second = manager.checkAndConsume(COMMAND_PATH, actor, cooldown);

        assertFalse(second.allowed());
        assertTrue(second.remaining().toNanos() > 0L);
    }

    @Test
    void invocationAfterWindowIsAllowed() throws InterruptedException {
        // TODO(F-02): inject Clock to replace this sleep-based timing assertion.
        CooldownManager manager = new CooldownManager();
        CommandActor actor = actor(UUID.randomUUID());
        CooldownDefinition cooldown = new CooldownDefinition(Duration.ofMillis(50), "");

        manager.checkAndConsume(COMMAND_PATH, actor, cooldown);
        Thread.sleep(60L);
        CooldownManager.CooldownResult after = manager.checkAndConsume(COMMAND_PATH, actor, cooldown);

        assertTrue(after.allowed());
        assertEquals(Duration.ZERO, after.remaining());
    }

    @Test
    void differentPlayersHaveIndependentWindows() {
        CooldownManager manager = new CooldownManager();
        CommandActor first = actor(UUID.randomUUID());
        CommandActor second = actor(UUID.randomUUID());
        CooldownDefinition cooldown = new CooldownDefinition(Duration.ofSeconds(5), "");

        manager.checkAndConsume(COMMAND_PATH, first, cooldown);
        CooldownManager.CooldownResult secondActorResult = manager.checkAndConsume(COMMAND_PATH, second, cooldown);

        assertTrue(secondActorResult.allowed());
    }

    @Test
    void differentCommandPathsHaveIndependentWindows() {
        CooldownManager manager = new CooldownManager();
        CommandActor actor = actor(UUID.randomUUID());
        CooldownDefinition cooldown = new CooldownDefinition(Duration.ofSeconds(5), "");

        manager.checkAndConsume(COMMAND_PATH, actor, cooldown);
        CooldownManager.CooldownResult otherPath = manager.checkAndConsume(OTHER_COMMAND_PATH, actor, cooldown);

        assertTrue(otherPath.allowed());
    }

    @Test
    void statusDoesNotConsume() throws InterruptedException {
        // TODO(F-02): inject Clock to replace this sleep-based timing assertion.
        CooldownManager manager = new CooldownManager();
        CommandActor actor = actor(UUID.randomUUID());
        CooldownDefinition cooldown = new CooldownDefinition(Duration.ofMillis(50), "");

        manager.checkAndConsume(COMMAND_PATH, actor, cooldown);
        CooldownManager.CooldownResult statusBlocked = manager.status(COMMAND_PATH, actor, cooldown);
        assertFalse(statusBlocked.allowed());

        Thread.sleep(60L);
        CooldownManager.CooldownResult consumed = manager.checkAndConsume(COMMAND_PATH, actor, cooldown);
        assertTrue(consumed.allowed());
    }

    private static CommandActor actor(UUID id) {
        CommandActor actor = mock(CommandActor.class);
        when(actor.uniqueId()).thenReturn(id);
        return actor;
    }
}
