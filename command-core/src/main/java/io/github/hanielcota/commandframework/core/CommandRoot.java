package io.github.hanielcota.commandframework.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a registered root command together with its aliases and the
 * root {@link CommandNode} of the literal tree.
 */
public record CommandRoot(String label, Set<String> aliases, CommandNode node) {

    public CommandRoot(String label, Set<String> aliases, CommandNode node) {
        this.label = Objects.requireNonNull(label, "label");
        this.aliases = Collections.unmodifiableSet(new LinkedHashSet<>(aliases));
        this.node = Objects.requireNonNull(node, "node");
    }
}
