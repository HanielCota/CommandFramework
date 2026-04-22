package io.github.hanielcota.commandframework.core.cooldown;

import java.util.Objects;

public record CooldownKey(String actorId, String routePath) {

    public CooldownKey {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(routePath, "routePath");
    }
}
