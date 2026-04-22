package io.github.hanielcota.commandframework.core;

import java.util.Objects;

/**
 * Restricts which kind of {@link CommandActor} may execute a route.
 */
public enum SenderRequirement {
    ANY,
    PLAYER,
    CONSOLE;

    /**
     * Checks whether the given actor satisfies this requirement.
     *
     * @param actor the actor to test; never {@code null}
     */
    public boolean allows(CommandActor actor) {
        Objects.requireNonNull(actor, "actor");
        return switch (this) {
            case ANY -> true;
            case PLAYER -> actor.isPlayer();
            case CONSOLE -> actor.isConsole();
        };
    }
}
