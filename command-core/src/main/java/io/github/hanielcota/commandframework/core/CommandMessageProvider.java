package io.github.hanielcota.commandframework.core;

import java.time.Duration;

/**
 * Provides human-readable messages for every dispatch outcome.
 *
 * <p>Implementations can customize color codes, prefixes, or translate
 * messages. All methods receive contextual data so the message can be as
 * specific as needed.</p>
 */
public interface CommandMessageProvider {

    String unknownCommand(String label);

    String noPermission(CommandContext context, String permission);

    String invalidSender(CommandContext context, SenderRequirement expected);

    String cooldown(CommandContext context, Duration remaining);

    String parseFailure(CommandContext context, String invalidValue, String expectedValue);

    String invalidUsage(CommandContext context, String usage);

    String rateLimited(CommandActor actor);

    String invalidInput(CommandActor actor, String invalidValue, String expectedValue);

    String internalError(CommandContext context);
}
