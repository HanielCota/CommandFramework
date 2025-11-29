package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.Cooldown;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.brigadier.CommandMetadata;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Extrai metadados de comandos a partir de anotações e métodos.
 */
public class CommandMetadataExtractor {
    /**
     * Extrai anotações da classe do comando.
     *
     * @param metadata Metadados do comando
     * @return Anotações extraídas (permissão e cooldown da classe)
     */
    public CommandAnnotations extractAnnotations(CommandMetadata metadata) {
        var type = metadata.getType();
        var classPermission = type.getAnnotation(RequiredPermission.class);
        var classCooldown = type.getAnnotation(Cooldown.class);
        return new CommandAnnotations(classPermission, classCooldown);
    }

    /**
     * Extrai métodos do comando (default e subcomandos).
     *
     * @param metadata Metadados do comando
     * @return Métodos extraídos com cache de parts dos subcomandos
     */
    public CommandMethods extractMethods(CommandMetadata metadata) {
        var methods = metadata.getType().getDeclaredMethods();
        var defaultMethod = findDefaultMethod(methods);
        var subCommands = extractSubCommands(methods);
        var subCommandPartsCache = buildSubCommandPartsCache(subCommands);
        return new CommandMethods(defaultMethod, subCommands, subCommandPartsCache);
    }

    /**
     * Encontra o método marcado com @DefaultCommand.
     *
     * @param methods Array de métodos para buscar
     * @return Método default ou null se não encontrado
     */
    private Method findDefaultMethod(Method[] methods) {
        for (var method : methods) {
            if (method.isAnnotationPresent(DefaultCommand.class)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    /**
     * Extrai todos os métodos marcados com @SubCommand.
     *
     * @param methods Array de métodos para buscar
     * @return Mapa de paths de subcomandos para métodos
     */
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

    /**
     * Constrói cache dos parts dos subcomandos para otimização.
     *
     * @param subCommands Mapa de subcomandos
     * @return Cache de parts dos subcomandos
     */
    private Map<String, String[]> buildSubCommandPartsCache(Map<String, Method> subCommands) {
        var cache = new HashMap<String, String[]>();
        for (var entry : subCommands.entrySet()) {
            var path = entry.getKey();
            var parts = path.split(" ");
            cache.put(path, parts);
        }
        return cache;
    }

    /**
     * Anotações extraídas da classe do comando.
     *
     * @param classPermission Permissão requerida pela classe
     * @param classCooldown   Cooldown da classe
     */
    public record CommandAnnotations(RequiredPermission classPermission, Cooldown classCooldown) {}
    
    /**
     * Métodos extraídos do comando.
     *
     * @param defaultMethod        Método marcado com @DefaultCommand
     * @param subCommands          Mapa de paths de subcomandos para métodos
     * @param subCommandPartsCache Cache dos parts dos subcomandos
     */
    public record CommandMethods(Method defaultMethod, Map<String, Method> subCommands, Map<String, String[]> subCommandPartsCache) {}
}

