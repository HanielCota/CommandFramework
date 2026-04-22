package io.github.hanielcota.commandframework.core;

import io.github.hanielcota.commandframework.core.safety.ActorMessageDebouncer;
import java.time.Duration;
import java.util.Objects;

/**
 * Encapsulates message formatting and debounced delivery to actors.
 */
public final class CommandMessenger {

    private final CommandMessageProvider messages;
    private final ActorMessageDebouncer debouncer;

    public CommandMessenger(CommandMessageProvider messages, ActorMessageDebouncer debouncer) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.debouncer = Objects.requireNonNull(debouncer, "debouncer");
    }

    public void send(CommandActor actor, String message) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(actor.uniqueId(), "actor.uniqueId");
        Objects.requireNonNull(message, "message");
        if (debouncer.shouldSend(actor.uniqueId(), message)) {
            actor.sendMessage(message);
        }
    }

    public CommandResult unknownCommand(CommandActor actor, String label) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(label, "label");
        send(actor, messages.unknownCommand(label));
        return CommandResult.failure(CommandStatus.NOT_FOUND, label);
    }

    public CommandResult noPermission(CommandContext context) {
        Objects.requireNonNull(context, "context");
        String permission = context.route().permission();
        send(context.actor(), messages.noPermission(context, permission));
        return CommandResult.failure(CommandStatus.NO_PERMISSION, permission);
    }

    public CommandResult invalidSender(CommandContext context) {
        Objects.requireNonNull(context, "context");
        SenderRequirement expected = context.route().senderRequirement();
        send(context.actor(), messages.invalidSender(context, expected));
        return CommandResult.failure(CommandStatus.INVALID_SENDER, expected.name());
    }

    public CommandResult cooldown(CommandContext context, Duration remaining) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(remaining, "remaining");
        send(context.actor(), messages.cooldown(context, remaining));
        return CommandResult.cooldown(remaining);
    }

    public void notifyParseFailure(CommandContext context, String invalidValue, String expectedValue) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        send(context.actor(), messages.parseFailure(context, invalidValue, expectedValue));
    }

    public CommandResult invalidUsage(CommandContext context, String usage) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(usage, "usage");
        send(context.actor(), messages.invalidUsage(context, usage));
        return CommandResult.failure(CommandStatus.INVALID_USAGE, "");
    }

    public CommandResult rateLimited(CommandActor actor) {
        Objects.requireNonNull(actor, "actor");
        send(actor, messages.rateLimited(actor));
        return CommandResult.failure(CommandStatus.RATE_LIMITED);
    }

    public CommandResult invalidInput(CommandActor actor, String invalidValue, String expectedValue) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        send(actor, messages.invalidInput(actor, invalidValue, expectedValue));
        return CommandResult.failure(CommandStatus.INVALID_USAGE, invalidValue);
    }

    public CommandResult internalError(CommandContext context) {
        Objects.requireNonNull(context, "context");
        send(context.actor(), messages.internalError(context));
        return CommandResult.failure(CommandStatus.ERROR);
    }
}
