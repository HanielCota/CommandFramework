package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.RouteConfigurationException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class StartupRouteValidator {

    private StartupRouteValidator() {
    }

    static void requireRoutes(Class<?> commandType, List<CommandRoute> routes) {
        Objects.requireNonNull(commandType, "commandType");
        Objects.requireNonNull(routes, "routes");
        if (routes.isEmpty()) {
            throw new RouteConfigurationException("Invalid command class '" + commandType.getName() + "': expected route method");
        }
        validateUniquePaths(commandType, routes);
    }

    static void validateReturnType(Method method) {
        Objects.requireNonNull(method, "method");
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE || returnType == CommandResult.class) {
            return;
        }
        throw new RouteConfigurationException("Invalid return type '" + returnType.getName() + "': expected void or CommandResult");
    }

    private static void validateUniquePaths(Class<?> commandType, List<CommandRoute> routes) {
        Set<String> paths = new HashSet<>();
        for (CommandRoute route : routes) {
            if (paths.add(route.canonicalPath())) {
                continue;
            }
            throw new RouteConfigurationException("Invalid command class '" + commandType.getName() + "': expected unique routes");
        }
    }
}
