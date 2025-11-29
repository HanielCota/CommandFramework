package com.github.hanielcota.commandframework.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Utilitários para operações de reflexão.
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    /**
     * Encontra um método em uma classe pelo nome e tipos de parâmetros.
     *
     * @param clazz          Classe onde buscar o método
     * @param name           Nome do método
     * @param parameterTypes Tipos dos parâmetros do método
     * @return Optional contendo o método encontrado, ou vazio se não encontrado
     */
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

    /**
     * Invoca um método em uma instância com os argumentos fornecidos.
     *
     * @param instance Instância onde invocar o método
     * @param method   Método a ser invocado
     * @param args     Argumentos para passar ao método
     * @return Optional contendo o resultado da invocação, ou vazio em caso de erro
     */
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

