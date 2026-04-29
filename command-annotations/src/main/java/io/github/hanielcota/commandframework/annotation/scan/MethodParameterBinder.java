package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.annotation.DefaultValue;
import io.github.hanielcota.commandframework.annotation.Greedy;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandParameter;
import io.github.hanielcota.commandframework.core.ParameterResolver;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import io.github.hanielcota.commandframework.core.argument.DefaultValueResolver;
import io.github.hanielcota.commandframework.core.argument.GreedyStringParameterResolver;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

record MethodParameterBinder(ParameterResolverRegistry resolvers) {

    MethodParameterBinder {
        Objects.requireNonNull(resolvers, "resolvers");
    }

    List<CommandParameter<?>> bind(Method method) {
        Objects.requireNonNull(method, "method");
        Method checkedMethod = method;
        List<CommandParameter<?>> parameters = new ArrayList<>();
        Parameter[] methodParameters = checkedMethod.getParameters();
        for (int index = 0; index < methodParameters.length; index++) {
            parameters.add(bindOne(checkedMethod, methodParameters[index], index, methodParameters.length));
        }
        return List.copyOf(parameters);
    }

    private <T> CommandParameter<T> bindOne(Method method, Parameter parameter, int index, int count) {
        Class<T> type = parameterType(parameter);
        ParameterResolver<T> resolver = resolver(method, parameter, type);
        validateRawArgumentsPosition(method, type, index, count);
        validateGreedyPosition(method, parameter, index, count);
        return new CommandParameter<>(parameter.getName(), type, resolver, visible(type));
    }

    private <T> ParameterResolver<T> resolver(Method method, Parameter parameter, Class<T> type) {
        ParameterResolver<T> base = resolvers.find(type).orElseThrow(() -> new IllegalArgumentException(
                "Invalid parameter type '" + type.getName() + "' in method '" + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                        + "': expected registered ParameterResolver or CommandActor as first parameter"
        ));
        if (parameter.isAnnotationPresent(Greedy.class) && type == String.class) {
            @SuppressWarnings("unchecked")
            ParameterResolver<T> greedy = (ParameterResolver<T>) new GreedyStringParameterResolver();
            base = greedy;
        }
        DefaultValue defaultValue = parameter.getAnnotation(DefaultValue.class);
        if (defaultValue != null) {
            base = new DefaultValueResolver<>(base, defaultValue.value());
        }
        return base;
    }

    private <T> void validateRawArgumentsPosition(Method method, Class<T> type, int index, int count) {
        if (type != String[].class || index == count - 1) {
            return;
        }
        throw new IllegalArgumentException(
                "Invalid parameter 'String[]' in method '" + method.getName() + "': expected final parameter"
        );
    }

    private void validateGreedyPosition(Method method, Parameter parameter, int index, int count) {
        if (!parameter.isAnnotationPresent(Greedy.class)) {
            return;
        }
        if (index == count - 1) {
            return;
        }
        throw new IllegalArgumentException(
                "Invalid parameter '@Greedy' in method '" + method.getName() + "': expected final parameter"
        );
    }

    private boolean visible(Class<?> type) {
        return !CommandActor.class.isAssignableFrom(type) && type != String[].class;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> parameterType(Parameter parameter) {
        return (Class<T>) parameter.getType();
    }
}
