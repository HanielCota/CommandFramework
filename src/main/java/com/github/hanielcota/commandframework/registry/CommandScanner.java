package com.github.hanielcota.commandframework.registry;

import com.github.hanielcota.commandframework.annotation.Command;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.cache.FrameworkCaches;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandScanner {

    public List<CommandDefinition> scan(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return List.of();
        }

        try (ScanResult scanResult = new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages(basePackage)
            .scan()) {

            var classes = scanResult.getClassesWithAnnotation(Command.class.getName());
            if (classes.isEmpty()) {
                return List.of();
            }

            return classes.stream()
                .map(info -> toDefinition(info.loadClass()))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        }
    }

    private Optional<CommandDefinition> toDefinition(Class<?> type) {
        if (type == null) {
            return Optional.empty();
        }

        var annotation = type.getAnnotation(Command.class);
        if (annotation == null) {
            return Optional.empty();
        }

        var methods = FrameworkCaches.commandMethods()
            .get(type, CommandScanner::findHandlerMethods);

        return Optional.of(
            CommandDefinition.builder()
                .annotation(annotation)
                .type(type)
                .handlers(methods)
                .build()
        );
    }

    private static List<Method> findHandlerMethods(Class<?> type) {
        if (type == null) {
            return List.of();
        }

        var methods = type.getDeclaredMethods();
        if (methods.length == 0) {
            return List.of();
        }

        return java.util.Arrays.stream(methods)
            .filter(method -> method.isAnnotationPresent(SubCommand.class) || method.isAnnotationPresent(DefaultCommand.class))
            .peek(method -> method.setAccessible(true))
            .collect(Collectors.toList());
    }
}
