package io.github.hanielcota.commandframework.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.CommandRouteRegistry;
import io.github.hanielcota.commandframework.core.RouteConfigurationException;
import io.github.hanielcota.commandframework.core.RouteResolution;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class CommandRouteRegistryTest {

    @Test
    void resolvesAliasToCanonicalRoute() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        CommandRoute route = route("kit", Set.of("kits"), List.of("give"));
        registry.register(route);
        RouteResolution resolution = registry.resolve("kits", List.of("give", "Steve"));
        assertTrue(resolution.isFound());
        assertEquals("kit give", resolution.match().orElseThrow().route().canonicalPath());
        assertEquals(List.of("Steve"), resolution.match().orElseThrow().arguments());
    }

    @Test
    void rejectsDuplicateCanonicalRoute() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        registry.register(route("kit", Set.of(), List.of()));
        assertThrows(RouteConfigurationException.class, () -> registry.register(route("kit", Set.of(), List.of())));
    }

    @Test
    void keepsRoutesRegisteredWithEquivalentRootLabels() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        registry.register(route("Kit", Set.of(), List.of("give")));
        registry.register(route("kit", Set.of(), List.of("take")));

        assertEquals(1, registry.roots().size());
        assertTrue(registry.resolve("KIT", List.of("give")).isFound());
        assertTrue(registry.resolve("KIT", List.of("take")).isFound());
    }

    @Test
    void keepsAliasesDeclaredByLaterRoutes() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        registry.register(route("kit", Set.of("kits"), List.of("give")));
        registry.register(route("kit", Set.of("loadout"), List.of("take")));

        CommandRoot root = registry.root("kit").orElseThrow();

        assertEquals(Set.of("kits", "loadout"), root.aliases());
        assertTrue(registry.resolve("loadout", List.of("take")).isFound());
    }

    @Test
    void doesNotKeepAliasesFromRejectedDuplicateRoute() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        registry.register(route("kit", Set.of(), List.of("give")));

        assertThrows(
                RouteConfigurationException.class,
                () -> registry.register(route("kit", Set.of("loadout"), List.of("give")))
        );

        assertTrue(registry.root("loadout").isEmpty());
        assertTrue(registry.root("kit").orElseThrow().aliases().isEmpty());
    }

    private CommandRoute route(String root, Set<String> aliases, List<String> path) {
        return CommandRoute.builder(root, (context, parameters) -> CommandResult.success())
                .aliases(aliases)
                .path(path)
                .build();
    }
}
