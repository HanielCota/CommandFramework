package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.MessageProvider;

/**
 * Default {@link MessageProvider} returning built-in MiniMessage templates for every
 * {@link MessageKey} in the framework.
 *
 * <p>This provider is registered automatically at framework construction and is superseded by any
 * explicit provider installed via the builder. Returned templates always include the full set of
 * placeholders documented by each key so callers can rely on deterministic substitution.
 *
 * <p><b>Thread-safety:</b> stateless and safe for concurrent use.
 */
public final class DefaultMessageProvider implements MessageProvider {

    /** Singleton instance — the provider is stateless. */
    public static final DefaultMessageProvider INSTANCE = new DefaultMessageProvider();

    @Override
    public String message(MessageKey key) {
        return switch (key) {
            case PLAYER_ONLY -> "<red>Only players can use this command.";
            case NO_PERMISSION -> "<red>You do not have permission to use this command.";
            case INVALID_ARGUMENT -> "<red>Invalid value for {name}: {input}";
            case MISSING_ARGUMENT -> "<red>Missing argument: {name}";
            case TOO_MANY_ARGUMENTS -> "<red>Too many arguments. Unexpected input: {input}";
            case COOLDOWN_ACTIVE -> "<red>You must wait {remaining} before using this command again.";
            case COMMAND_ERROR -> "<red>An unexpected error occurred while running this command.";
            case CONFIRM_PROMPT -> "<yellow>Click <click:run_command:'/{command}'><hover:show_text:'<gray>Run /{command}'><green>[Confirm]</green></hover></click> within <white>{seconds}s</white>.";
            case CONFIRM_NOTHING_PENDING -> "<red>You have nothing pending to confirm.";
            case HELP_HEADER -> "<yellow>Available commands for /{command}:";
            case HELP_ENTRY -> "<gray>/{usage}</gray> <dark_gray>-</dark_gray> <white>{description}";
            case UNKNOWN_SUBCOMMAND -> "<red>Unknown subcommand <yellow>{typed}</yellow>. Did you mean <click:suggest_command:'/{command} {suggestion}'><green>{suggestion}</green></click>?";
        };
    }
}
