package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.TabCompletion;
import com.github.hanielcota.commandframework.collection.TabCompletions;
import com.github.hanielcota.commandframework.value.CompletionLimit;
import com.github.hanielcota.commandframework.value.ParameterIndex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gera sugestões de tab completion para comandos.
 * Suporta sugestões estáticas, dinâmicas via providers e sugestões padrão baseadas em tipos.
 */
public class TabCompleter {
    private static final Logger LOGGER = Logger.getLogger(TabCompleter.class.getSimpleName());

    /**
     * Gera completions baseado nos argumentos fornecidos.
     *
     * @param args                  Argumentos do comando
     * @param subCommandPartsCache  Cache dos parts dos subcomandos
     * @param defaultMethod         Método default do comando
     * @param subCommands           Mapa de subcomandos
     * @return Lista de sugestões de completion
     */
    public List<String> complete(String[] args, Map<String, String[]> subCommandPartsCache, Method defaultMethod, Map<String, Method> subCommands) {
        var completions = new TabCompletions();
        if (args == null || args.length == 0) {
            return completeEmptyArgs(subCommandPartsCache, completions);
        }
        if (args.length == 1) {
            return completeSingleArg(args[0], subCommandPartsCache, defaultMethod, completions);
        }
        return completeMultipleArgs(args, subCommands, defaultMethod, completions, subCommandPartsCache);
    }

    private List<String> completeEmptyArgs(Map<String, String[]> subCommandPartsCache, TabCompletions completions) {
        for (var parts : subCommandPartsCache.values()) {
            if (parts.length > 0) {
                completions.add(parts[0]);
            }
        }
        return completions.asList();
    }

    private List<String> completeSingleArg(String input, Map<String, String[]> subCommandPartsCache, Method defaultMethod, TabCompletions completions) {
        var lowerInput = input.toLowerCase();
        addSubCommandCompletions(lowerInput, subCommandPartsCache, completions);
        if (completions.isEmpty() && defaultMethod != null) {
            addMethodCompletions(defaultMethod, new ParameterIndex(0), lowerInput, completions);
        }
        return completions.asList();
    }

    private void addSubCommandCompletions(String input, Map<String, String[]> subCommandPartsCache, TabCompletions completions) {
        for (var parts : subCommandPartsCache.values()) {
            if (parts.length > 0 && parts[0].startsWith(input)) {
                completions.add(parts[0]);
            }
        }
    }

    private List<String> completeMultipleArgs(String[] args, Map<String, Method> subCommands, Method defaultMethod, TabCompletions completions, Map<String, String[]> subCommandPartsCache) {
        var finder = new SubCommandFinder(subCommandPartsCache);
        var match = finder.findSubCommand(args, subCommands);
        var targetMethod = resolveTargetMethod(match, subCommands, defaultMethod);
        if (targetMethod == null) {
            return completions.asList();
        }
        var paramIndex = calculateParamIndex(match, args, targetMethod);
        var input = args[args.length - 1].toLowerCase();
        addMethodCompletions(targetMethod, paramIndex, input, completions);
        return completions.asList();
    }

    private Method resolveTargetMethod(SubCommandFinder.SubCommandMatch match, Map<String, Method> subCommands, Method defaultMethod) {
        if (match != null) {
            var path = match.path().toLowerCase();
            return subCommands.get(path);
        }
        return defaultMethod;
    }

    private ParameterIndex calculateParamIndex(SubCommandFinder.SubCommandMatch match, String[] args, Method targetMethod) {
        if (match != null) {
            var remaining = match.remainingArgs();
            var index = remaining.length() - 1;
            return new ParameterIndex(index);
        }
        return new ParameterIndex(args.length - 1);
    }

    private void addMethodCompletions(Method method, ParameterIndex paramIndex, String input, TabCompletions completions) {
        var methodTabCompletion = method.getAnnotation(TabCompletion.class);
        if (methodTabCompletion != null) {
            if (tryMethodTabCompletion(methodTabCompletion, paramIndex, method, input, completions)) {
                return;
            }
        }
        tryParameterTabCompletion(method, paramIndex, input, completions);
    }

    private boolean tryMethodTabCompletion(TabCompletion annotation, ParameterIndex paramIndex, Method method, String input, TabCompletions completions) {
        var staticSuggestions = annotation.value();
        if (staticSuggestions.length > 0) {
            if (paramIndex.value() == 0 || method.getParameterCount() == 0) {
                addStaticSuggestions(staticSuggestions, input, completions);
                if (!completions.isEmpty()) {
                    return true;
                }
            }
        }
        return tryProviderCompletions(annotation.provider(), input, completions);
    }

    private void addStaticSuggestions(String[] suggestions, String input, TabCompletions completions) {
        for (var suggestion : suggestions) {
            completions.addIfStartsWith(suggestion, input);
        }
    }

    private boolean tryProviderCompletions(Class<?> providerClass, String input, TabCompletions completions) {
        if (providerClass == null || providerClass == TabCompletion.NoProvider.class) {
            return false;
        }
        var providerCompletions = getProviderCompletions(providerClass, input);
        if (providerCompletions.isEmpty()) {
            return false;
        }
        providerCompletions.forEach(completions::add);
        return true;
    }

    private void tryParameterTabCompletion(Method method, ParameterIndex paramIndex, String input, TabCompletions completions) {
        var parameters = method.getParameters();
        if (!paramIndex.isWithinBounds(parameters.length)) {
            return;
        }
        var parameter = parameters[paramIndex.value()];
        var tabCompletion = parameter.getAnnotation(TabCompletion.class);
        if (tabCompletion != null) {
            if (tryParameterStaticSuggestions(tabCompletion, input, completions)) {
                return;
            }
            tryProviderCompletions(tabCompletion.provider(), input, completions);
            return;
        }
        addDefaultCompletions(parameter, input, completions);
    }

    private boolean tryParameterStaticSuggestions(TabCompletion tabCompletion, String input, TabCompletions completions) {
        var staticSuggestions = tabCompletion.value();
        if (staticSuggestions.length == 0) {
            return false;
        }
        addStaticSuggestions(staticSuggestions, input, completions);
        return !completions.isEmpty();
    }

    private void addDefaultCompletions(Parameter parameter, String input, TabCompletions completions) {
        var type = parameter.getType();
        if (type.equals(Player.class)) {
            addPlayerCompletions(input, completions);
            return;
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            addBooleanCompletions(input, completions);
        }
    }

    private void addPlayerCompletions(String input, TabCompletions completions) {
        var players = Bukkit.getOnlinePlayers();
        var limit = CompletionLimit.DEFAULT;
        var count = 0;
        for (var player : players) {
            if (limit.isReached(count)) {
                break;
            }
            var name = player.getName();
            if (name.toLowerCase().startsWith(input)) {
                completions.add(name);
                count++;
            }
        }
    }

    private void addBooleanCompletions(String input, TabCompletions completions) {
        if ("true".startsWith(input)) {
            completions.add("true");
        }
        if ("false".startsWith(input)) {
            completions.add("false");
        }
    }

    private List<String> getProviderCompletions(Class<?> providerClass, String input) {
        var completions = new TabCompletions();
        try {
            var provider = providerClass.getDeclaredConstructor().newInstance();
            var methods = providerClass.getDeclaredMethods();
            for (var method : methods) {
                if (isProviderMethod(method)) {
                    method.setAccessible(true);
                    var result = method.invoke(provider);
                    if (result instanceof List<?> list) {
                        addProviderCompletions(list, input, completions);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warning("[CommandFramework] Erro ao obter sugestões do provider: " + e.getMessage());
        }
        return completions.asList();
    }

    private boolean isProviderMethod(Method method) {
        var methodName = method.getName();
        return methodName.equals("getSuggestions") || methodName.equals("complete");
    }

    private void addProviderCompletions(List<?> list, String input, TabCompletions completions) {
        var limit = CompletionLimit.DEFAULT;
        var count = 0;
        for (var item : list) {
            if (limit.isReached(count)) {
                break;
            }
            var str = item.toString();
            if (str.toLowerCase().startsWith(input)) {
                completions.add(str);
                count++;
            }
        }
    }
}

