package com.github.hanielcota.commandframework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

public final class FrameworkCaches {

    private static final Cache<Class<?>, List<Method>> COMMAND_METHODS = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(256)
        .build();

    private static final Cache<Class<?>, Object> HANDLER_INSTANCES = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .maximumSize(256)
        .build();

    private FrameworkCaches() {
    }

    public static Cache<Class<?>, List<Method>> commandMethods() {
        return COMMAND_METHODS;
    }

    public static Cache<Class<?>, Object> handlerInstances() {
        return HANDLER_INSTANCES;
    }
}
