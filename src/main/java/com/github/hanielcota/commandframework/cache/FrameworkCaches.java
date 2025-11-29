package com.github.hanielcota.commandframework.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

public final class FrameworkCaches {

    private FrameworkCaches() {
    }

    public static Cache<Class<?>, List<Method>> commandMethods() {
        return Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(256)
            .build();
    }

    public static Cache<Class<?>, Object> handlerInstances() {
        return Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(256)
            .build();
    }
}


