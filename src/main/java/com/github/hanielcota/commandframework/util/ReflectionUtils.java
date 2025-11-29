package com.github.hanielcota.commandframework.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        if (clazz == null) {
            return Optional.empty();
        }

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        try {
            var method = clazz.getMethod(name, parameterTypes);
            return Optional.of(method);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    public static Optional<Object> invokeMethod(Object instance, Method method, Object... args) {
        if (instance == null) {
            return Optional.empty();
        }

        if (method == null) {
            return Optional.empty();
        }

        try {
            method.setAccessible(true);
            var result = method.invoke(instance, args);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

