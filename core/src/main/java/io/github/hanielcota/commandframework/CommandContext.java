package io.github.hanielcota.commandframework;

import java.util.List;
import java.util.Objects;

/**
 * Immutable context passed through command middlewares.
 *
 * @param actor        the sender actor
 * @param label        the command label used to invoke the command
 * @param rawArguments the raw argument string
 * @param arguments    the tokenized arguments
 * @param commandPath  the resolved command path, such as {@code eco pay}
 */
public record CommandContext(
        CommandActor actor,
        String label,
        String rawArguments,
        List<String> arguments,
        String commandPath
) {

    /**
     * Creates a new command context.
     *
     * @param actor        the sender actor
     * @param label        the command label
     * @param rawArguments the raw argument string
     * @param arguments    the tokenized arguments
     * @param commandPath  the resolved command path
     */
    public CommandContext {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(rawArguments, "rawArguments");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        Objects.requireNonNull(commandPath, "commandPath");
    }
}
