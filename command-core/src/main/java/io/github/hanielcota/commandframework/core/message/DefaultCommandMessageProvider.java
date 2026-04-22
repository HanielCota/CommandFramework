package io.github.hanielcota.commandframework.core.message;

import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandMessageProvider;
import io.github.hanielcota.commandframework.core.SenderRequirement;
import java.time.Duration;
import java.util.Objects;

public final class DefaultCommandMessageProvider implements CommandMessageProvider {

    @Override
    public String unknownCommand(String label) {
        Objects.requireNonNull(label, "label");
        return "Unknown command: %s.".formatted(label);
    }

    @Override
    public String noPermission(CommandContext context, String permission) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(permission, "permission");
        return "No permission: %s.".formatted(permission);
    }

    @Override
    public String invalidSender(CommandContext context, SenderRequirement expected) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(expected, "expected");
        return "Invalid sender. Expected: %s.".formatted(expected.name().toLowerCase());
    }

    @Override
    public String cooldown(CommandContext context, Duration remaining) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(remaining, "remaining");
        return "Wait %ds to use this command again.".formatted(Math.max(1, remaining.toSeconds()));
    }

    @Override
    public String parseFailure(CommandContext context, String invalidValue, String expectedValue) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        return "Invalid argument '%s'. Expected: %s.".formatted(invalidValue, expectedValue);
    }

    @Override
    public String invalidUsage(CommandContext context, String usage) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(usage, "usage");
        return "Usage: %s".formatted(usage);
    }

    @Override
    public String rateLimited(CommandActor actor) {
        Objects.requireNonNull(actor, "actor");
        return "You are sending commands too fast.";
    }

    @Override
    public String invalidInput(CommandActor actor, String invalidValue, String expectedValue) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        return "Invalid input '%s'. Expected: %s.".formatted(invalidValue, expectedValue);
    }

    @Override
    public String internalError(CommandContext context) {
        Objects.requireNonNull(context, "context");
        return "An internal error occurred while executing the command.";
    }
}
