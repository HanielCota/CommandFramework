package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.value.ArgumentIndex;
import com.github.hanielcota.commandframework.value.RemainingArguments;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Invoca métodos de comandos, resolvendo parâmetros automaticamente.
 * Suporta tipos como CommandSender, Player, String, Integer, Double, Boolean, etc.
 */
public class MethodInvoker {
    /**
     * Invoca um método de comando, resolvendo parâmetros e tratando o resultado.
     *
     * @param sender   Quem executou o comando
     * @param method   Método a ser invocado
     * @param instance Instância onde invocar o método
     * @param args     Argumentos do comando
     */
    public void invoke(CommandSender sender, Method method, Object instance, String[] args) {
        var parameters = method.getParameters();
        var invokeArgs = buildInvokeArgs(sender, parameters, args);
        var result = executeMethod(method, instance, invokeArgs);
        handleResult(sender, result);
    }

    private Object[] buildInvokeArgs(CommandSender sender, Parameter[] parameters, String[] args) {
        var invokeArgs = new Object[parameters.length];
        var argIndex = new ArgumentIndex(0);
        for (int i = 0; i < parameters.length; i++) {
            var param = parameters[i];
            var resolved = resolveParameter(sender, param, args, argIndex, parameters, i);
            invokeArgs[i] = resolved.value;
            argIndex = resolved.newIndex;
        }
        return invokeArgs;
    }

    private ParameterResolution resolveParameter(CommandSender sender, Parameter param, String[] args, ArgumentIndex argIndex, Parameter[] allParams, int paramIndex) {
        var type = param.getType();
        if (type.equals(CommandSender.class)) {
            return new ParameterResolution(sender, argIndex);
        }
        if (type.equals(Player.class)) {
            return resolvePlayerParameter(sender, args, argIndex, allParams, paramIndex);
        }
        if (type.equals(String[].class)) {
            return new ParameterResolution(args, argIndex);
        }
        if (type.equals(String.class)) {
            return resolveStringParameter(args, argIndex);
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return resolveIntegerParameter(args, argIndex, type.equals(int.class));
        }
        if (type.equals(Double.class) || type.equals(double.class)) {
            return resolveDoubleParameter(args, argIndex, type.equals(double.class));
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return resolveBooleanParameter(args, argIndex, type.equals(boolean.class));
        }
        return new ParameterResolution(null, argIndex);
    }

    private ParameterResolution resolvePlayerParameter(CommandSender sender, String[] args, ArgumentIndex argIndex, Parameter[] allParams, int paramIndex) {
        if (isFirstPlayerParameter(allParams, paramIndex)) {
            if (sender instanceof Player player) {
                return new ParameterResolution(player, argIndex);
            }
            return new ParameterResolution(null, argIndex);
        }
        if (!argIndex.isWithinBounds(args.length)) {
            return new ParameterResolution(null, argIndex);
        }
        var player = org.bukkit.Bukkit.getPlayer(args[argIndex.value()]);
        return new ParameterResolution(player, argIndex.increment());
    }

    private boolean isFirstPlayerParameter(Parameter[] parameters, int index) {
        if (index == 0) {
            return true;
        }
        return index == 1 && parameters[0].getType().equals(CommandSender.class);
    }

    private ParameterResolution resolveStringParameter(String[] args, ArgumentIndex argIndex) {
        if (!argIndex.isWithinBounds(args.length)) {
            return new ParameterResolution(null, argIndex);
        }
        var value = args[argIndex.value()];
        return new ParameterResolution(value, argIndex.increment());
    }

    private ParameterResolution resolveIntegerParameter(String[] args, ArgumentIndex argIndex, boolean isPrimitive) {
        if (!argIndex.isWithinBounds(args.length)) {
            return new ParameterResolution(isPrimitive ? 0 : null, argIndex);
        }
        try {
            var value = Integer.parseInt(args[argIndex.value()]);
            return new ParameterResolution(value, argIndex.increment());
        } catch (NumberFormatException e) {
            return new ParameterResolution(isPrimitive ? 0 : null, argIndex);
        }
    }

    private ParameterResolution resolveDoubleParameter(String[] args, ArgumentIndex argIndex, boolean isPrimitive) {
        if (!argIndex.isWithinBounds(args.length)) {
            return new ParameterResolution(isPrimitive ? 0.0 : null, argIndex);
        }
        try {
            var value = Double.parseDouble(args[argIndex.value()]);
            return new ParameterResolution(value, argIndex.increment());
        } catch (NumberFormatException e) {
            return new ParameterResolution(isPrimitive ? 0.0 : null, argIndex);
        }
    }

    private ParameterResolution resolveBooleanParameter(String[] args, ArgumentIndex argIndex, boolean isPrimitive) {
        if (!argIndex.isWithinBounds(args.length)) {
            return new ParameterResolution(isPrimitive ? false : null, argIndex);
        }
        var value = Boolean.parseBoolean(args[argIndex.value()]);
        return new ParameterResolution(value, argIndex.increment());
    }

    private Object executeMethod(Method method, Object instance, Object[] args) {
        try {
            method.setAccessible(true);
            return method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar método", e);
        }
    }

    private void handleResult(CommandSender sender, Object result) {
        if (result instanceof Component component) {
            sender.sendMessage(component);
            return;
        }
        if (result instanceof String string) {
            sender.sendMessage(Component.text(string));
        }
    }

    private record ParameterResolution(Object value, ArgumentIndex newIndex) {}
}

