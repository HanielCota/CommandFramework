package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.core.SenderRequirement;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

record RouteAnnotationModel(
        String root,
        Set<String> aliases,
        List<String> path,
        String permission,
        SenderRequirement senderRequirement,
        Duration cooldown,
        String description,
        String syntax,
        boolean async
) {

    RouteAnnotationModel {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(aliases, "aliases");
        aliases = Set.copyOf(aliases);
        Objects.requireNonNull(path, "path");
        path = List.copyOf(path);
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(senderRequirement, "senderRequirement");
        Objects.requireNonNull(cooldown, "cooldown");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(syntax, "syntax");
    }
}
