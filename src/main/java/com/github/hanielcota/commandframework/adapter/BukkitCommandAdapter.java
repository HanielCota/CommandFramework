package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.Async;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.annotation.TabCompletion;
import com.github.hanielcota.commandframework.brigadier.CommandMetadata;
import com.github.hanielcota.commandframework.cooldown.CooldownKey;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.annotation.Cooldown;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.Collection;
import java.util.logging.Logger;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BukkitCommandAdapter {

    private static final Logger LOGGER = Logger.getLogger(BukkitCommandAdapter.class.getSimpleName());
    private static CommandMap CACHED_COMMAND_MAP;

    Plugin plugin;
    CooldownService cooldownService;
    GlobalErrorHandler errorHandler;

    public void register(CommandMetadata metadata) {
        if (metadata == null) {
            return;
        }

        var annotation = metadata.getCommandAnnotation();
        var name = annotation.name();
        var aliases = annotation.aliases();
        var description = annotation.description();

        var command = new FrameworkCommand(name, description, aliases, metadata);

        var commandMap = getCommandMap();
        if (commandMap == null) {
            LOGGER.severe("[CommandFramework] Não foi possível obter o CommandMap!");
            return;
        }

        // Remove comando existente se overrideVanilla
        if (annotation.overrideVanilla()) {
            var existing = commandMap.getCommand(name);
            if (existing != null) {
                existing.unregister(commandMap);
            }
        }

        commandMap.register(plugin.getName().toLowerCase(), command);
        LOGGER.info("[CommandFramework] Comando /" + name + " registrado com sucesso!");
    }

    private CommandMap getCommandMap() {
        if (CACHED_COMMAND_MAP != null) {
            return CACHED_COMMAND_MAP;
        }

        try {
            var server = Bukkit.getServer();
            var method = server.getClass().getMethod("getCommandMap");
            CACHED_COMMAND_MAP = (CommandMap) method.invoke(server);
            return CACHED_COMMAND_MAP;
        } catch (Exception e) {
            LOGGER.severe("[CommandFramework] Erro ao obter CommandMap: " + e.getMessage());
            return null;
        }
    }

    private class FrameworkCommand extends BukkitCommand {

        private final CommandMetadata metadata;
        private final Map<String, Method> subCommands = new HashMap<>();
        private final Map<String, String[]> subCommandPartsCache = new HashMap<>();
        private Method defaultMethod;
        private final RequiredPermission classPermission;
        private final Cooldown classCooldown;

        protected FrameworkCommand(String name, String description, String[] aliases, CommandMetadata metadata) {
            super(name);
            this.metadata = metadata;
            this.setDescription(description);
            this.setAliases(Arrays.asList(aliases));

            // Cachea permissão e cooldown da classe
            this.classPermission = metadata.getType().getAnnotation(RequiredPermission.class);
            this.classCooldown = metadata.getType().getAnnotation(Cooldown.class);
            
            if (this.classPermission != null) {
                this.setPermission(this.classPermission.value());
            }

            // Encontra o método default e subcomandos
            for (var method : metadata.getType().getDeclaredMethods()) {
                if (method.isAnnotationPresent(DefaultCommand.class)) {
                    this.defaultMethod = method;
                    method.setAccessible(true);
                }
                if (method.isAnnotationPresent(SubCommand.class)) {
                    var subAnnotation = method.getAnnotation(SubCommand.class);
                    var subPath = subAnnotation.value().toLowerCase();
                    subCommands.put(subPath, method);
                    // Cachea os parts do subcomando para evitar split repetido
                    subCommandPartsCache.put(subPath, subPath.split(" "));
                    method.setAccessible(true);
                }
            }
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (sender == null) {
                return false;
            }

            if (args == null) {
                args = new String[0];
            }

            // Verifica permissão da classe (usando cache)
            if (classPermission != null && !sender.hasPermission(classPermission.value())) {
                errorHandler.handleNoPermission(sender, classPermission.value())
                    .ifPresent(sender::sendMessage);
                return true;
            }

            // Se não há argumentos, executa o método default
            if (args.length == 0) {
                if (defaultMethod == null) {
                    sender.sendMessage(Component.text("Uso: /" + label + " <subcomando>"));
                    return true;
                }

                // Verifica permissão do método
                var methodPermission = defaultMethod.getAnnotation(RequiredPermission.class);
                if (methodPermission != null && !sender.hasPermission(methodPermission.value())) {
                    errorHandler.handleNoPermission(sender, methodPermission.value())
                        .ifPresent(sender::sendMessage);
                    return true;
                }

                executeMethod(sender, defaultMethod, args);
                return true;
            }

            // Tenta encontrar subcomando
            var subCommandPath = findSubCommandPath(args);
            if (subCommandPath != null) {
                var method = subCommands.get(subCommandPath.path.toLowerCase());
                if (method != null) {
                    // Verifica permissão do método
                    var methodPermission = method.getAnnotation(RequiredPermission.class);
                    if (methodPermission != null && !sender.hasPermission(methodPermission.value())) {
                        errorHandler.handleNoPermission(sender, methodPermission.value())
                            .ifPresent(sender::sendMessage);
                        return true;
                    }

                    executeMethod(sender, method, subCommandPath.remainingArgs);
                    return true;
                }
            }

            // Se nenhum subcomando, executa default com args
            if (defaultMethod == null) {
                errorHandler.handleSubCommandNotFound(sender, label)
                    .ifPresent(sender::sendMessage);
                return true;
            }

            // Verifica permissão do método
            var methodPermission = defaultMethod.getAnnotation(RequiredPermission.class);
            if (methodPermission != null && !sender.hasPermission(methodPermission.value())) {
                errorHandler.handleNoPermission(sender, methodPermission.value())
                    .ifPresent(sender::sendMessage);
                return true;
            }

            executeMethod(sender, defaultMethod, args);
            return true;
        }

        private SubCommandMatch findSubCommandPath(String[] args) {
            if (args.length == 0) {
                return null;
            }

            // Tenta encontrar subcomandos de múltiplos níveis primeiro
            int maxDepth = Math.min(args.length, 3);
            for (int depth = maxDepth; depth >= 1; depth--) {
                // Constrói path sem criar arrays intermediários
                StringBuilder pathBuilder = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    if (i > 0) pathBuilder.append(' ');
                    pathBuilder.append(args[i].toLowerCase());
                }
                String path = pathBuilder.toString();
                
                if (subCommands.containsKey(path)) {
                    // Só cria array remaining se necessário
                    String[] remaining = depth < args.length 
                        ? Arrays.copyOfRange(args, depth, args.length)
                        : new String[0];
                    return new SubCommandMatch(path, remaining);
                }
            }
            return null;
        }

        private void executeMethod(CommandSender sender, Method method, String[] args) {
            if (sender == null || method == null) {
                return;
            }

            // Verifica se o método deve ser executado assincronamente
            boolean isAsync = method.isAnnotationPresent(Async.class);
            
            if (isAsync) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> doExecuteMethod(sender, method, args));
                return;
            }

            doExecuteMethod(sender, method, args);
        }

        private void doExecuteMethod(CommandSender sender, Method method, String[] args) {
            if (sender == null || method == null) {
                return;
            }

            try {
                // Verifica cooldown (primeiro do método, depois da classe se não encontrar - usando cache)
                var cooldown = method.getAnnotation(Cooldown.class);
                if (cooldown == null) {
                    cooldown = classCooldown;
                }
                
                if (cooldown != null) {
                    var uuid = sender instanceof Player p ? p.getUniqueId() : 
                        UUID.fromString("00000000-0000-0000-0000-000000000000");
                    var key = new CooldownKey(uuid, getName(), method.getName());
                    
                    var remainingTime = cooldownService.getRemainingTime(key);
                    if (remainingTime.isPresent()) {
                        errorHandler.handleCooldown(sender, remainingTime.get());
                        return;
                    }
                    cooldownService.putOnCooldown(key, Duration.ofSeconds(cooldown.seconds()));
                }

                var parameters = method.getParameters();
                var invokeArgs = new Object[parameters.length];

                int argIndex = 0;
                for (int i = 0; i < parameters.length; i++) {
                    var param = parameters[i];
                    var type = param.getType();

                    if (type.equals(CommandSender.class)) {
                        invokeArgs[i] = sender;
                        continue;
                    }

                    if (type.equals(Player.class)) {
                        invokeArgs[i] = resolvePlayerParameter(sender, parameters, args, i, argIndex);
                        if (invokeArgs[i] == null && isFirstPlayerParameter(parameters, i)) {
                            sender.sendMessage(Component.text("Este comando só pode ser usado por jogadores."));
                            return;
                        }
                        if (!isFirstPlayerParameter(parameters, i) && argIndex < args.length) {
                            argIndex++;
                        }
                        continue;
                    }

                    if (type.equals(String[].class)) {
                        invokeArgs[i] = args;
                        continue;
                    }

                    if (type.equals(String.class)) {
                        invokeArgs[i] = argIndex < args.length ? args[argIndex++] : null;
                        continue;
                    }

                    if (type.equals(Integer.class) || type.equals(int.class)) {
                        invokeArgs[i] = parseInteger(args, argIndex, type.equals(int.class));
                        if (argIndex < args.length) argIndex++;
                        continue;
                    }

                    if (type.equals(Double.class) || type.equals(double.class)) {
                        invokeArgs[i] = parseDouble(args, argIndex, type.equals(double.class));
                        if (argIndex < args.length) argIndex++;
                        continue;
                    }

                    if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                        invokeArgs[i] = parseBoolean(args, argIndex, type.equals(boolean.class));
                        if (argIndex < args.length) argIndex++;
                        continue;
                    }

                    invokeArgs[i] = null;
                }

                var result = method.invoke(metadata.getInstance(), invokeArgs);

                // Trata o resultado
                if (result instanceof Component component) {
                    sender.sendMessage(component);
                    return;
                }

                if (result instanceof String string) {
                    sender.sendMessage(Component.text(string));
                }

            } catch (Exception e) {
                LOGGER.severe("[CommandFramework] Erro ao executar comando: " + e.getMessage());
                e.printStackTrace();
                errorHandler.handleInternalError(sender, e).ifPresent(sender::sendMessage);
            }
        }

        private boolean isFirstPlayerParameter(java.lang.reflect.Parameter[] parameters, int index) {
            if (index == 0) {
                return true;
            }
            return index == 1 && parameters[0].getType().equals(CommandSender.class);
        }

        private Player resolvePlayerParameter(CommandSender sender, java.lang.reflect.Parameter[] parameters, String[] args, int index, int argIndex) {
            // Primeiro parâmetro Player = sender
            if (isFirstPlayerParameter(parameters, index)) {
                if (sender instanceof Player p) {
                    return p;
                }
                return null;
            }

            // Parâmetro Player posterior = parse do nome
            if (argIndex >= args.length) {
                return null;
            }

            return Bukkit.getPlayer(args[argIndex]);
        }

        private Object parseInteger(String[] args, int argIndex, boolean isPrimitive) {
            if (argIndex >= args.length) {
                return isPrimitive ? 0 : null;
            }

            try {
                return Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                return isPrimitive ? 0 : null;
            }
        }

        private Object parseDouble(String[] args, int argIndex, boolean isPrimitive) {
            if (argIndex >= args.length) {
                return isPrimitive ? 0.0 : null;
            }

            try {
                return Double.parseDouble(args[argIndex]);
            } catch (NumberFormatException e) {
                return isPrimitive ? 0.0 : null;
            }
        }

        private Object parseBoolean(String[] args, int argIndex, boolean isPrimitive) {
            if (argIndex >= args.length) {
                return isPrimitive ? false : null;
            }

            return Boolean.parseBoolean(args[argIndex]);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            var completions = new ArrayList<String>();

            if (sender == null || args == null) {
                return completions;
            }

            // Se não há argumentos ou o primeiro argumento está vazio
            if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
                // Usa cache de parts para evitar split repetido
                for (var parts : subCommandPartsCache.values()) {
                    if (parts.length > 0) {
                        completions.add(parts[0]);
                    }
                }
                return completions;
            }

            // Se há apenas 1 argumento, sugere subcomandos ou parâmetros do default
            if (args.length == 1) {
                var input = args[0].toLowerCase();
                
                // Primeiro, tenta subcomandos (usa cache de parts)
                for (var parts : subCommandPartsCache.values()) {
                    if (parts.length > 0 && parts[0].startsWith(input)) {
                        completions.add(parts[0]);
                    }
                }
                
                // Se não encontrou subcomandos e há método default, sugere parâmetros do default
                if (completions.isEmpty() && defaultMethod != null) {
                    completions.addAll(getTabCompletionsForMethod(defaultMethod, 0, input));
                }
                
                return completions;
            }

            // Para 2+ argumentos, determina qual método será executado
            Method targetMethod = null;
            int paramIndex = 0;
            String input = args[args.length - 1].toLowerCase();

            // Tenta encontrar subcomando
            var subCommandPath = findSubCommandPath(args);
            if (subCommandPath != null) {
                targetMethod = subCommands.get(subCommandPath.path.toLowerCase());
                if (targetMethod != null) {
                    // Calcula qual parâmetro está sendo completado
                    paramIndex = subCommandPath.remainingArgs.length - 1;
                }
            }

            if (targetMethod == null && defaultMethod != null) {
                // Usa o método default
                targetMethod = defaultMethod;
                paramIndex = args.length - 1;
            }

            if (targetMethod == null) {
                return completions;
            }

            completions.addAll(getTabCompletionsForMethod(targetMethod, paramIndex, input));
            return completions;
        }

        private List<String> getTabCompletionsForMethod(Method method, int paramIndex, String input) {
            var completions = new ArrayList<String>();
            
            if (method == null) {
                return completions;
            }

            // Primeiro, verifica se o método tem @TabCompletion
            // Isso se aplica quando o primeiro argumento está sendo completado
            var methodTabCompletion = method.getAnnotation(TabCompletion.class);
            if (methodTabCompletion != null) {
                String[] staticSuggestions = methodTabCompletion.value();
                if (staticSuggestions.length > 0) {
                    // Se o método tem @TabCompletion, aplica ao primeiro argumento
                    // (paramIndex 0 ou quando não há parâmetros específicos)
                    if (paramIndex == 0 || method.getParameterCount() == 0) {
                        for (String suggestion : staticSuggestions) {
                            if (suggestion.toLowerCase().startsWith(input)) {
                                completions.add(suggestion);
                            }
                        }
                        if (!completions.isEmpty()) {
                            return completions;
                        }
                    }
                }
                
                // Tenta usar provider dinâmico do método
                var providerClass = methodTabCompletion.provider();
                if (providerClass != null && providerClass != TabCompletion.NoProvider.class) {
                    var providerCompletions = getProviderCompletions(providerClass, input);
                    if (!providerCompletions.isEmpty()) {
                        return providerCompletions;
                    }
                }
            }

            var parameters = method.getParameters();
            if (paramIndex < 0 || paramIndex >= parameters.length) {
                return completions;
            }

            var parameter = parameters[paramIndex];
            
            // Se o parâmetro é String[], usa as sugestões do método se disponíveis
            if (parameter.getType().equals(String[].class) && methodTabCompletion != null) {
                String[] staticSuggestions = methodTabCompletion.value();
                if (staticSuggestions.length > 0) {
                    for (String suggestion : staticSuggestions) {
                        if (suggestion.toLowerCase().startsWith(input)) {
                            completions.add(suggestion);
                        }
                    }
                    return completions;
                }
            }

            var tabCompletion = parameter.getAnnotation(TabCompletion.class);
            
            if (tabCompletion != null) {
                // Prioriza sugestões estáticas do parâmetro
                String[] staticSuggestions = tabCompletion.value();
                if (staticSuggestions.length > 0) {
                    for (String suggestion : staticSuggestions) {
                        if (suggestion.toLowerCase().startsWith(input)) {
                            completions.add(suggestion);
                        }
                    }
                    return completions;
                }

                // Se não há sugestões estáticas, tenta usar o provider
                var providerClass = tabCompletion.provider();
                if (providerClass != null && providerClass != TabCompletion.NoProvider.class) {
                    var providerCompletions = getProviderCompletions(providerClass, input);
                    if (!providerCompletions.isEmpty()) {
                        return providerCompletions;
                    }
                }
            }

            // Sugestões padrão baseadas no tipo
            var type = parameter.getType();
            if (type.equals(Player.class)) {
                // Sugere jogadores online (limita a 50 para performance)
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                int count = 0;
                for (Player player : players) {
                    if (count >= 50) break; // Limita sugestões para performance
                    var name = player.getName();
                    if (name.toLowerCase().startsWith(input)) {
                        completions.add(name);
                        count++;
                    }
                }
                return completions;
            }

            if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                // Sugere true/false para booleanos
                if ("true".startsWith(input)) completions.add("true");
                if ("false".startsWith(input)) completions.add("false");
            }

            return completions;
        }

        private List<String> getProviderCompletions(Class<?> providerClass, String input) {
            var completions = new ArrayList<String>();
            try {
                // Cache de providers poderia ser adicionado aqui se necessário
                var provider = providerClass.getDeclaredConstructor().newInstance();
                // Se o provider tem um método getSuggestions ou similar
                var methods = providerClass.getDeclaredMethods();
                for (var method : methods) {
                    var methodName = method.getName();
                    if (methodName.equals("getSuggestions") || methodName.equals("complete")) {
                        method.setAccessible(true);
                        var result = method.invoke(provider);
                        if (result instanceof List<?> list) {
                            int count = 0;
                            for (var item : list) {
                                if (count >= 50) break; // Limita sugestões para performance
                                var str = item.toString();
                                if (str.toLowerCase().startsWith(input)) {
                                    completions.add(str);
                                    count++;
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("[CommandFramework] Erro ao obter sugestões do provider: " + e.getMessage());
            }
            return completions;
        }

        private record SubCommandMatch(String path, String[] remainingArgs) {}
    }
}
