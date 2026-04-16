package io.github.hanielcota.commandframework;

import java.util.List;
import java.util.Objects;

/**
 * Immutable context provided to argument resolvers.
 *
 * @param actor             the sender actor
 * @param label             the command label
 * @param commandPath       the resolved command path
 * @param previousArguments already resolved argument values in declaration order
 */
public record ArgumentResolutionContext(
        CommandActor actor,
        String label,
        String commandPath,
        List<Object> previousArguments
) {

    /**
     * Creates a new argument resolution context.
     *
     * @param actor             the sender actor
     * @param label             the command label
     * @param commandPath       the command path
     * @param previousArguments already resolved argument values
     */
    public ArgumentResolutionContext {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(commandPath, "commandPath");
        previousArguments = List.copyOf(Objects.requireNonNull(previousArguments, "previousArguments"));
    }
}
