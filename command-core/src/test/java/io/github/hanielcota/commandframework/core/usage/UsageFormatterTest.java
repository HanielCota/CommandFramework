package io.github.hanielcota.commandframework.core.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hanielcota.commandframework.core.CommandExecutor;
import io.github.hanielcota.commandframework.core.CommandParameter;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

final class UsageFormatterTest {

    @Test
    void formatsRouteWithoutParameters() {
        UsageFormatter formatter = new UsageFormatter();
        CommandRoute route = CommandRoute.builder("kit", executor()).build();
        assertEquals("/kit", formatter.format(route));
    }

    @Test
    void formatsRouteWithParameters() {
        UsageFormatter formatter = new UsageFormatter();
        var registry = ParameterResolverRegistry.withDefaults();
        var resolver = registry.find(String.class).orElseThrow();
        CommandRoute route = CommandRoute.builder("kit", executor())
                .parameters(List.of(new CommandParameter<>("player", String.class, resolver, true)))
                .path(List.of("give"))
                .build();
        assertEquals("/kit give <player>", formatter.format(route));
    }

    private CommandExecutor executor() {
        return (context, parameters) -> null;
    }
}
