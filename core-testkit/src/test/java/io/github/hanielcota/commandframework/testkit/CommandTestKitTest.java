package io.github.hanielcota.commandframework.testkit;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class CommandTestKitTest {

    @Test
    void dispatchSuccessfulRoot() {
        HelloCommand command = new HelloCommand();
        CommandTestKit kit = CommandTestKit.create(command);

        kit.dispatch("hello", "").assertSuccess();
        org.junit.jupiter.api.Assertions.assertEquals(1, command.calls.get());
    }

    @Test
    void dispatchCooldownAfterFirstInvocation() {
        CooldownCommand command = new CooldownCommand();
        CommandTestKit kit = CommandTestKit.create(command);
        TestSender player = kit.player("Alice");

        kit.dispatch(player, "cool", "").assertSuccess();
        kit.dispatch(player, "cool", "").assertCooldownActive();
    }

    @Test
    void dispatchNoPermissionWithoutGrant() {
        ProtectedCommand command = new ProtectedCommand();
        CommandTestKit kit = CommandTestKit.create(command);
        TestSender player = kit.player("Bob");

        kit.dispatch(player, "secret", "").assertNoPermission("testkit.secret");

        player.grant("testkit.secret");
        kit.dispatch(player, "secret", "").assertSuccess();
    }

    @Command(name = "hello")
    private static final class HelloCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void run(@Sender TestSender sender) {
            this.calls.incrementAndGet();
            sender.sendMessage(Component.text("hi"));
        }
    }

    @Command(name = "cool")
    private static final class CooldownCommand {
        @Execute
        @Cooldown(value = 30, unit = TimeUnit.SECONDS)
        public void run(@Sender TestSender sender) {
            sender.sendMessage(Component.text("done"));
        }
    }

    @Command(name = "secret")
    @Permission("testkit.secret")
    private static final class ProtectedCommand {
        @Execute
        public void run(@Sender TestSender sender) {
            sender.sendMessage(Component.text("ok"));
        }
    }
}
