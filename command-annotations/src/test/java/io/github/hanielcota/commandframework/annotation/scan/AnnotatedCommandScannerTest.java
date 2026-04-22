package io.github.hanielcota.commandframework.annotation.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Default;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandStatus;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AnnotatedCommandScannerTest {

    @Test
    void scansAndExecutesAnnotatedCommand() {
        AnnotatedCommandScanner scanner = new AnnotatedCommandScanner(ParameterResolverRegistry.withDefaults());
        CommandDispatcher dispatcher = CommandDispatcher.builder().build();
        scanner.scan(new KitCommand()).forEach(dispatcher::register);
        TestActor actor = new TestActor();
        CommandResult result = dispatcher.dispatch(actor, "kit", new String[]{"give", "Steve"});
        assertEquals(CommandStatus.SUCCESS, result.status());
        assertEquals(List.of("Kit enviado para Steve"), actor.messages());
    }

    @Test
    void rejectsUnsupportedParameterAtStartup() {
        AnnotatedCommandScanner scanner = new AnnotatedCommandScanner(ParameterResolverRegistry.withDefaults());
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(new InvalidCommand()));
    }

    @Command("kit")
    private static final class KitCommand {

        @Subcommand("give")
        void give(CommandActor actor, String target) {
            actor.sendMessage("Kit enviado para " + target);
        }
    }

    @Command("bad")
    private static final class InvalidCommand {

        @Default
        void run(Thread unsupported) {
        }
    }

    private static final class TestActor implements CommandActor {

        private final List<String> messages = new ArrayList<>();

        List<String> messages() {
            return messages;
        }

        @Override
        public String uniqueId() {
            return "annotation-test";
        }

        @Override
        public String name() {
            return "AnnotationTest";
        }

        @Override
        public ActorKind kind() {
            return ActorKind.PLAYER;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }
    }
}
