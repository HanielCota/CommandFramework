package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandExecutor;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.ParsedParameter;
import io.github.hanielcota.commandframework.core.RouteConfigurationException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

record MethodCommandExecutor(MethodHandle handle, boolean returnsCommandResult) implements CommandExecutor {

    MethodCommandExecutor {
        Objects.requireNonNull(handle, "handle");
    }

    static <T> MethodCommandExecutor create(T commandInstance, Method method) {
        T checkedCommandInstance = Objects.requireNonNull(commandInstance, "commandInstance");
        Method checkedMethod = Objects.requireNonNull(method, "method");
        StartupRouteValidator.validateReturnType(checkedMethod);
        if (Modifier.isStatic(checkedMethod.getModifiers())) {
            throw new RouteConfigurationException("Invalid method '" + checkedMethod.getName() + "': expected non-static method");
        }
        try {
            if (!checkedMethod.trySetAccessible()) {
                throw new IllegalAccessException("Method is not accessible");
            }
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(checkedMethod.getDeclaringClass(), MethodHandles.lookup());
            MethodHandle handle = lookup.unreflect(checkedMethod).bindTo(checkedCommandInstance);
            return new MethodCommandExecutor(handle, checkedMethod.getReturnType() == CommandResult.class);
        } catch (IllegalAccessException exception) {
            throw new RouteConfigurationException(
                    "Invalid method '" + checkedMethod.getName() + "': expected accessible method",
                    exception
            );
        }
    }

    @Override
    public CommandResult execute(CommandContext context, List<ParsedParameter<?>> parameters) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(parameters, "parameters");
        try {
            var result = handle.invokeWithArguments(values(parameters));
            if (!returnsCommandResult || result == null) {
                return CommandResult.success();
            }
            return (CommandResult) result;
        } catch (Error error) {
            throw error;
        } catch (Throwable throwable) {
            throw new CommandInvocationException("Command method failed", throwable);
        }
    }

    private List<?> values(List<ParsedParameter<?>> parameters) {
        Objects.requireNonNull(parameters, "parameters");
        return parameters.stream()
                .map(ParsedParameter::value)
                .toList();
    }
}
