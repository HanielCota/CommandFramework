package io.github.hanielcota.commandframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandRouteTest {

    @Test
    void buildsMinimalRoute() {
        CommandRoute route = CommandRoute.builder("test", (ctx, params) -> null).build();
        assertEquals("test", route.root());
        assertTrue(route.path().isEmpty());
    }

    @Test
    void rejectsNegativeCooldown() {
        assertThrows(IllegalArgumentException.class, () ->
                CommandRoute.builder("test", (ctx, params) -> null)
                        .cooldown(Duration.ofSeconds(-1)));
    }

    @Test
    void canonicalPathWithPath() {
        CommandRoute route = CommandRoute.builder("test", (ctx, params) -> null)
                .path(List.of("sub", "cmd"))
                .build();
        assertEquals("test sub cmd", route.canonicalPath());
    }

    @Test
    void canonicalPathWithoutPath() {
        CommandRoute route = CommandRoute.builder("test", (ctx, params) -> null).build();
        assertEquals("test", route.canonicalPath());
    }
}
