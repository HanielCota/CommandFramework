package io.github.hanielcota.commandframework.core.usage;

import io.github.hanielcota.commandframework.core.CommandParameter;
import io.github.hanielcota.commandframework.core.CommandRoute;
import java.util.List;
import java.util.Objects;

public final class UsageFormatter {

    public String format(CommandRoute route) {
        Objects.requireNonNull(route, "route");
        List<String> parameters = route.parameters().stream()
                .filter(CommandParameter::visibleInUsage)
                .filter(CommandParameter::consumesInput)
                .map(parameter -> "<" + parameter.name() + ">")
                .toList();

        if (parameters.isEmpty()) {
            return "/" + route.canonicalPath();
        }
        return "/" + route.canonicalPath() + " " + String.join(" ", parameters);
    }
}
