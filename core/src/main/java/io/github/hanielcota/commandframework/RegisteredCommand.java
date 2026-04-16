package io.github.hanielcota.commandframework;

import java.util.List;
import java.util.Objects;

/**
 * Describes a top-level command registered in the framework.
 *
 * @param name        the primary command name
 * @param aliases     the secondary aliases
 * @param description the command description
 */
public record RegisteredCommand(String name, List<String> aliases, String description) {

    /**
     * Creates a new command descriptor.
     *
     * @param name        the primary command name
     * @param aliases     the aliases
     * @param description the description
     */
    public RegisteredCommand {
        Objects.requireNonNull(name, "name");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        Objects.requireNonNull(description, "description");
    }
}
