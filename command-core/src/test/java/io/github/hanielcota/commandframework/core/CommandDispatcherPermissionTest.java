package io.github.hanielcota.commandframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandDispatcherPermissionTest {

    @Test
    void rejectsActorWithoutPermissionBeforeExecution() {
        CommandDispatcher dispatcher = CommandDispatcher.builder().build();
        dispatcher.register(route());
        TestActor actor = new TestActor(ActorKind.PLAYER);
        CommandResult result = dispatcher.dispatch(actor, "kit", new String[]{"give", "Steve"});
        assertEquals(CommandStatus.NO_PERMISSION, result.status());
        assertEquals(1, actor.messages().size());
    }

    @Test
    void dispatchesListArguments() {
        CommandDispatcher dispatcher = CommandDispatcher.builder().build();
        dispatcher.register(route());
        TestActor actor = new TestActor(ActorKind.PLAYER);
        actor.grant("kit.give");

        CommandResult result = dispatcher.dispatch(actor, "kit", List.of("give"));

        assertEquals(CommandStatus.SUCCESS, result.status());
        assertEquals(List.of("executed"), actor.messages());
    }

    private CommandRoute route() {
        return CommandRoute.builder("kit", (context, parameters) -> {
                    context.actor().sendMessage("executed");
                    return CommandResult.success();
                })
                .path(List.of("give"))
                .permission("kit.give")
                .build();
    }
}
