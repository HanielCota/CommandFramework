package io.github.hanielcota.commandframework.annotation.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hanielcota.commandframework.annotation.scan.AnnotatedCommandScanner;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PlatformCommandAdapterTest {

    @Test
    void registersPlatformRootOnlyOnceWhenAliasesChange() {
        CommandDispatcher dispatcher = CommandDispatcher.builder().build();
        RecordingAdapter adapter = new RecordingAdapter(dispatcher);

        adapter.registerRoutes(List.of(route("kit", Set.of("kits"), List.of("give"))));
        adapter.registerRoutes(List.of(route("kit", Set.of("loadout"), List.of("take"))));

        assertEquals(List.of("kit"), adapter.registeredLabels());
    }

    private CommandRoute route(String root, Set<String> aliases, List<String> path) {
        return CommandRoute.builder(root, (context, parameters) -> CommandResult.success())
                .aliases(aliases)
                .path(path)
                .build();
    }

    private static final class RecordingAdapter extends PlatformCommandAdapter {

        private final List<String> registeredLabels = new ArrayList<>();

        private RecordingAdapter(CommandDispatcher dispatcher) {
            super(dispatcher, new AnnotatedCommandScanner(ParameterResolverRegistry.withDefaults()));
        }

        List<String> registeredLabels() {
            return registeredLabels;
        }

        @Override
        protected void registerRoot(CommandRoot root) {
            registeredLabels.add(root.label());
        }

        @Override
        protected void unregisterRoot(CommandRoot root) {
        }
    }
}
