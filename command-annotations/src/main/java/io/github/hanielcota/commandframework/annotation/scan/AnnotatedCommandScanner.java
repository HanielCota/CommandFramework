package io.github.hanielcota.commandframework.annotation.scan;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class AnnotatedCommandScanner {

    private final MethodParameterBinder parameterBinder;
    private final RouteAnnotationReader annotationReader;
    private final ConcurrentHashMap<Class<?>, List<CommandMethodTemplate>> commandMethods = new ConcurrentHashMap<>();

    public AnnotatedCommandScanner(ParameterResolverRegistry resolvers) {
        Objects.requireNonNull(resolvers, "resolvers");
        this.parameterBinder = new MethodParameterBinder(resolvers);
        this.annotationReader = new RouteAnnotationReader();
    }

    public <T> List<CommandRoute> scan(T commandInstance) {
        T checkedCommandInstance = Objects.requireNonNull(commandInstance, "commandInstance");
        Class<?> commandType = checkedCommandInstance.getClass();
        List<CommandRoute> routes = createRoutes(
                checkedCommandInstance, commandMethods.computeIfAbsent(commandType, this::readCommandMethods));
        StartupRouteValidator.requireRoutes(commandType, routes);
        return routes;
    }

    public void clearCache() {
        commandMethods.clear();
    }

    private List<CommandMethodTemplate> readCommandMethods(Class<?> commandType) {
        Objects.requireNonNull(commandType, "commandType");
        Command command = commandType.getAnnotation(Command.class);
        if (command == null) {
            throw new IllegalArgumentException(
                    "Invalid command class '" + commandType.getName() + "': expected @Command");
        }
        return Arrays.stream(commandType.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName)
                        .thenComparing(method -> Arrays.toString(method.getParameterTypes())))
                .filter(annotationReader::isCommandMethod)
                .map(method -> new CommandMethodTemplate(method, annotationReader.read(commandType, command, method)))
                .toList();
    }

    private <T> List<CommandRoute> createRoutes(T commandInstance, List<CommandMethodTemplate> methods) {
        Objects.requireNonNull(commandInstance, "commandInstance");
        Objects.requireNonNull(methods, "methods");
        return methods.stream()
                .map(method -> createRoute(commandInstance, method))
                .toList();
    }

    private <T> CommandRoute createRoute(T commandInstance, CommandMethodTemplate template) {
        Objects.requireNonNull(commandInstance, "commandInstance");
        Objects.requireNonNull(template, "template");
        RouteAnnotationModel model = template.model();
        MethodCommandExecutor executor = MethodCommandExecutor.create(commandInstance, template.method());
        return CommandRoute.builder(model.root(), executor)
                .aliases(model.aliases())
                .path(model.path())
                .permission(model.permission())
                .senderRequirement(model.senderRequirement())
                .cooldown(model.cooldown())
                .description(model.description())
                .syntax(model.syntax())
                .async(model.async())
                .parameters(parameterBinder.bind(template.method()))
                .build();
    }

    private record CommandMethodTemplate(Method method, RouteAnnotationModel model) {

        private CommandMethodTemplate {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(model, "model");
        }
    }
}
