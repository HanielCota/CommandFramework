package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.CommandResult;
import io.github.hanielcota.commandframework.MessageKey;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Renders dispatcher outcomes to the invoking actor by translating each {@link CommandResult}
 * variant into the appropriate {@link MessageService} call and by printing the per-command help
 * listing when no executor matches.
 *
 * <p>The permission gate is supplied as a {@link BiPredicate} so the emitter shares the exact
 * visibility rule used elsewhere in the pipeline without holding a back-reference to
 * {@link CommandDispatcher}.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use. All mutable state is request-scoped.
 */
final class CommandResultEmitter {

    private final MessageService messages;
    private final BiPredicate<CommandActor, ExecutorDefinition> permissionGate;

    CommandResultEmitter(
            MessageService messages,
            BiPredicate<CommandActor, ExecutorDefinition> permissionGate
    ) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.permissionGate = Objects.requireNonNull(permissionGate, "permissionGate");
    }

    CommandResult emit(CommandActor actor, CommandResult result) {
        switch (result) {
            case CommandResult.Failure(MessageKey key, Map<String, String> placeholders) ->
                this.messages.send(actor, key, placeholders);
            case CommandResult.InvalidArgs(String argumentName, String input) ->
                this.messages.send(actor, MessageKey.INVALID_ARGUMENT, Map.of(
                        "name", argumentName,
                        "input", input
                ));
            case CommandResult.NoPermission ignored ->
                this.messages.send(actor, MessageKey.NO_PERMISSION);
            case CommandResult.PlayerOnly ignored ->
                this.messages.send(actor, MessageKey.PLAYER_ONLY);
            case CommandResult.CooldownActive(Duration remaining) ->
                this.messages.send(actor, MessageKey.COOLDOWN_ACTIVE, Map.of(
                        "remaining", this.messages.formatDuration(remaining)
                ));
            case CommandResult.PendingConfirmation(String commandName, Duration expiresIn) ->
                this.messages.send(actor, MessageKey.CONFIRM_PROMPT, Map.of(
                        "command", commandName,
                        "seconds", String.valueOf(Math.max(1L, expiresIn.toSeconds()))
                ));
            default -> { /* Success / Handled / HelpShown / RateLimited - nothing to emit */ }
        }
        return result;
    }

    void sendHelp(CommandActor actor, CommandDefinition command, String label) {
        List<Component> lines = new ArrayList<>();
        lines.add(this.messages.render(MessageKey.HELP_HEADER, Map.of("command", label)));

        if (command.rootExecutor() != null && this.permissionGate.test(actor, command.rootExecutor())) {
            lines.add(this.messages.render(MessageKey.HELP_ENTRY, Map.of(
                    "usage", label,
                    "description", command.rootExecutor().description()
            )));
        }

        for (Map.Entry<String, ExecutorDefinition> entry : command.executorsBySubcommand().entrySet()) {
            if (!this.permissionGate.test(actor, entry.getValue())) {
                continue;
            }
            lines.add(this.messages.render(MessageKey.HELP_ENTRY, Map.of(
                    "usage", label + " " + entry.getKey(),
                    "description", entry.getValue().description()
            )));
        }

        actor.sendMessage(this.messages.renderLines(lines.toArray(Component[]::new)));
    }
}
