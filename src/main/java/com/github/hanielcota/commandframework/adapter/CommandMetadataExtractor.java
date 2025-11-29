package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.Cooldown;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.brigadier.CommandMetadata;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CommandMetadataExtractor {
    public CommandAnnotations extractAnnotations(CommandMetadata metadata) {
        var type = metadata.getType();
        var classPermission = type.getAnnotation(RequiredPermission.class);
        var classCooldown = type.getAnnotation(Cooldown.class);
        return new CommandAnnotations(classPermission, classCooldown);
    }

    public CommandMethods extractMethods(CommandMetadata metadata) {
        var methods = metadata.getType().getDeclaredMethods();
        var defaultMethod = findDefaultMethod(methods);
        var subCommands = extractSubCommands(methods);
        var subCommandPartsCache = buildSubCommandPartsCache(subCommands);
        return new CommandMethods(defaultMethod, subCommands, subCommandPartsCache);
    }

    private Method findDefaultMethod(Method[] methods) {
        for (var method : methods) {
            if (method.isAnnotationPresent(DefaultCommand.class)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private Map<String, Method> extractSubCommands(Method[] methods) {
        var subCommands = new HashMap<String, Method>();
        for (var method : methods) {
            if (method.isAnnotationPresent(SubCommand.class)) {
                var annotation = method.getAnnotation(SubCommand.class);
                var path = annotation.value().toLowerCase();
                subCommands.put(path, method);
                method.setAccessible(true);
            }
        }
        return subCommands;
    }

    private Map<String, String[]> buildSubCommandPartsCache(Map<String, Method> subCommands) {
        var cache = new HashMap<String, String[]>();
        for (var entry : subCommands.entrySet()) {
            var path = entry.getKey();
            var parts = path.split(" ");
            cache.put(path, parts);
        }
        return cache;
    }

    public record CommandAnnotations(RequiredPermission classPermission, Cooldown classCooldown) {}
    public record CommandMethods(Method defaultMethod, Map<String, Method> subCommands, Map<String, String[]> subCommandPartsCache) {}
}

