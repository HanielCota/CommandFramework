package io.github.hanielcota.commandframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hanielcota.commandframework.core.RouteConfigurationException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandNodeTest {

    @Test
    void createsChildNode() {
        CommandNode root = new CommandNode("root");
        CommandNode child = root.childOrCreate("child");
        assertEquals("child", child.literal());
        assertTrue(root.child("child").isPresent());
    }

    @Test
    void suggestsChildLiterals() {
        CommandNode root = new CommandNode("root");
        root.childOrCreate("abc");
        root.childOrCreate("abd");
        root.childOrCreate("xyz");
        assertEquals(List.of("abc", "abd"), root.childLiteralsStartingWith("ab"));
    }

    @Test
    void preventsDuplicateRoute() {
        CommandNode node = new CommandNode("node");
        CommandRoute route = CommandRoute.builder("cmd", (ctx, params) -> null).build();
        node.setRoute(route);
        assertThrows(RouteConfigurationException.class, () -> node.setRoute(route));
    }

    @Test
    void preventsDuplicateDefaultRoute() {
        CommandNode node = new CommandNode("node");
        CommandRoute route = CommandRoute.builder("cmd", (ctx, params) -> null).build();
        node.setDefaultRoute(route);
        assertThrows(RouteConfigurationException.class, () -> node.setDefaultRoute(route));
    }
}
