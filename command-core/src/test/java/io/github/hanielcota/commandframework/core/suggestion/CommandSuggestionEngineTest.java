package io.github.hanielcota.commandframework.core.suggestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.CommandRouteRegistry;
import io.github.hanielcota.commandframework.core.TestActor;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandSuggestionEngineTest {

    @Test
    void suggestsNestedChildLiteralsFromCurrentNode() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        registry.register(route("kit", List.of("global")));
        registry.register(route("kit", List.of("admin", "give")));
        CommandSuggestionEngine suggestions = new CommandSuggestionEngine(registry);

        List<String> values = suggestions.suggest(new TestActor(ActorKind.PLAYER), "kit", List.of("admin", "g"));

        assertEquals(List.of("give"), values);
    }

    @Test
    void normalizesChildSuggestionPrefix() {
        CommandRouteRegistry registry = new CommandRouteRegistry();
        registry.register(route("kit", List.of("give")));
        CommandSuggestionEngine suggestions = new CommandSuggestionEngine(registry);

        List<String> values = suggestions.suggest(new TestActor(ActorKind.PLAYER), "kit", List.of("G"));

        assertEquals(List.of("give"), values);
    }

    private CommandRoute route(String root, List<String> path) {
        return CommandRoute.builder(root, (context, parameters) -> CommandResult.success())
                .path(path)
                .build();
    }
}
