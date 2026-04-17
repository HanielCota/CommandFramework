package io.github.hanielcota.commandframework.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

final class RuntimeAccessFactory {

    private static final Map<String, Class<?>> PRIMITIVES = Map.ofEntries(
            Map.entry("boolean", boolean.class),
            Map.entry("byte", byte.class),
            Map.entry("char", char.class),
            Map.entry("short", short.class),
            Map.entry("int", int.class),
            Map.entry("long", long.class),
            Map.entry("float", float.class),
            Map.entry("double", double.class),
            Map.entry("void", void.class)
    );

    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, MethodHandles.Lookup> lookups = new ConcurrentHashMap<>();

    RuntimeAccessFactory(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    Class<?> resolveClass(String className) {
        Objects.requireNonNull(className, "className");
        Class<?> primitive = PRIMITIVES.get(className);
        if (primitive != null) {
            return primitive;
        }
        return this.classCache.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name, false, this.classLoader);
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Unable to load class " + name, exception);
            }
        });
    }

    Object instantiate(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            return this.privateLookup(type).unreflectConstructor(constructor).invoke();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("No accessible no-arg constructor in " + type.getName(), exception);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to instantiate " + type.getName(), exception);
        }
    }

    void injectField(Object instance, Field field, Object dependency) {
        try {
            this.privateLookup(field.getDeclaringClass()).unreflectSetter(field).invoke(instance, dependency);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to inject field " + field, exception);
        }
    }

    Field field(String declaringClassName, String fieldName) {
        Class<?> declaringClass = this.resolveClass(declaringClassName);
        try {
            return declaringClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Unable to resolve field "
                    + declaringClassName + "#" + fieldName, exception);
        }
    }

    CommandInvoker invoker(Method method) {
        try {
            MethodHandle spreader = this.privateLookup(method.getDeclaringClass())
                    .unreflect(method)
                    .asSpreader(Object[].class, method.getParameterCount());
            boolean returnsVoid = method.getReturnType() == Void.TYPE;
            return (instance, arguments) -> {
                if (returnsVoid) {
                    spreader.invoke(instance, arguments);
                    return null;
                }
                return spreader.invoke(instance, arguments);
            };
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to prepare invoker for " + method, exception);
        }
    }

    Method method(String declaringClassName, String methodName, List<Class<?>> parameterTypes) {
        Class<?> declaringClass = this.resolveClass(declaringClassName);
        try {
            return declaringClass.getDeclaredMethod(methodName, parameterTypes.toArray(Class[]::new));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to resolve method "
                    + declaringClassName + "#" + methodName + parameterTypes, exception);
        }
    }

    void verifyInjectable(Field field) {
        if (Modifier.isFinal(field.getModifiers())) {
            throw new IllegalStateException("@Inject field cannot be final: "
                    + field.getDeclaringClass().getName() + "#" + field.getName()
                    + ". Remove 'final' from the field modifier.");
        }
    }

    private MethodHandles.Lookup privateLookup(Class<?> type) {
        return this.lookups.computeIfAbsent(type, key -> {
            try {
                return MethodHandles.privateLookupIn(key, MethodHandles.lookup());
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to create method handle lookup for " + key.getName(), exception);
            }
        });
    }
}
