package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.Async;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.annotation.TabCompletion;
import com.github.hanielcota.commandframework.brigadier.CommandMetadata;
import com.github.hanielcota.commandframework.cooldown.CooldownKey;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.dependency.DependencyResolver;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.annotation.Cooldown;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class BukkitCommandAdapter {

    private static final Logger LOGGER = Logger.getLogger(BukkitCommandAdapter.class.getSimpleName());
    private static CommandMap CACHED_COMMAND_MAP;
    
    // Cache de instâncias de providers de tab completion para evitar reflexão repetida
    private static final Map<Class<?>, Object> PROVIDER_CACHE = new ConcurrentHashMap<>();

    Plugin plugin;
    CooldownService cooldownService;
    GlobalErrorHandler errorHandler;
    DependencyResolver dependencyResolver;

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
        private final Map<Method, MethodMetadata> methodMetadataCache = new HashMap<>();
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
                    cacheMethodMetadata(method);
                }
                if (method.isAnnotationPresent(SubCommand.class)) {
                    var subAnnotation = method.getAnnotation(SubCommand.class);
                    var subPath = subAnnotation.value().toLowerCase();
                    subCommands.put(subPath, method);
                    // Cachea os parts do subcomando para evitar split repetido
                    subCommandPartsCache.put(subPath, subPath.split(" "));
                    method.setAccessible(true);
                    cacheMethodMetadata(method);
                }
            }
        }
        
        /**
         * Cacheia as annotations do método para evitar reflexão repetida durante execução.
         */
        private void cacheMethodMetadata(Method method) {
            var cooldown = method.getAnnotation(Cooldown.class);
            var permission = method.getAnnotation(RequiredPermission.class);
            var isAsync = method.isAnnotationPresent(Async.class);
            methodMetadataCache.put(method, new MethodMetadata(cooldown, permission, isAsync));
        }
        
        /**
         * Record para armazenar metadata cacheada de um método.
         */
        private record MethodMetadata(Cooldown cooldown, RequiredPermission permission, boolean isAsync) {}

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

                // Verifica permissão do método (usando cache)
                if (!hasMethodPermission(sender, defaultMethod)) {
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
                    // Verifica permissão do método (usando cache)
                    if (!hasMethodPermission(sender, method)) {
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

            // Verifica permissão do método (usando cache)
            if (!hasMethodPermission(sender, defaultMethod)) {
                return true;
            }

            executeMethod(sender, defaultMethod, args);
            return true;
        }
        
        /**
         * Verifica se o sender tem permissão para executar o método usando cache.
         * @return true se tem permissão, false se não tem (já envia mensagem de erro)
         */
        private boolean hasMethodPermission(CommandSender sender, Method method) {
            var metadata = methodMetadataCache.get(method);
            if (metadata == null || metadata.permission() == null) {
                return true;
            }
            var permission = metadata.permission().value();
            if (sender.hasPermission(permission)) {
                return true;
            }
            errorHandler.handleNoPermission(sender, permission)
                .ifPresent(sender::sendMessage);
            return false;
        }

        private SubCommandMatch findSubCommandPath(String[] args) {
            if (args.length == 0) {
                return null;
            }

            // Calcula a profundidade máxima baseada nos subcomandos registrados
            int maxSubCommandDepth = calculateMaxSubCommandDepth();
            int maxDepth = Math.min(args.length, maxSubCommandDepth);
            
            // Tenta encontrar subcomandos de múltiplos níveis primeiro
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
        
        /**
         * Calcula a profundidade máxima dos subcomandos registrados.
         * @return A profundidade máxima (número de níveis)
         */
        private int calculateMaxSubCommandDepth() {
            int maxDepth = 0;
            for (var parts : subCommandPartsCache.values()) {
                maxDepth = Math.max(maxDepth, parts.length);
            }
            // Retorna pelo menos 1 para garantir que sempre tenta encontrar subcomandos
            return Math.max(maxDepth, 1);
        }

        private void executeMethod(CommandSender sender, Method method, String[] args) {
            if (sender == null || method == null) {
                return;
            }

            // Usa cache para verificar se o método deve ser executado assincronamente
            var metadata = methodMetadataCache.get(method);
            boolean isAsync = metadata != null && metadata.isAsync();
            
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
                // Verifica cooldown usando cache (primeiro do método, depois da classe)
                var methodMeta = methodMetadataCache.get(method);
                var cooldown = methodMeta != null ? methodMeta.cooldown() : null;
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
                        boolean isFirstPlayer = isFirstPlayerParameter(parameters, i);
                        invokeArgs[i] = resolvePlayerParameter(sender, parameters, args, i, argIndex);
                        if (invokeArgs[i] == null && isFirstPlayer) {
                            sender.sendMessage(Component.text("Este comando só pode ser usado por jogadores."));
                            return;
                        }
                        // Se não é o primeiro parâmetro Player, sempre incrementa argIndex se usou um argumento
                        if (!isFirstPlayer && argIndex < args.length) {
                            argIndex++;
                        }
                        continue;
                    }

                    if (type.equals(OfflinePlayer.class)) {
                        if (argIndex >= args.length) {
                            invokeArgs[i] = null;
                            continue;
                        }
                        String targetName = args[argIndex++];
                        OfflinePlayer resolved = resolveOfflinePlayer(targetName);
                        if (resolved == null) {
                            errorHandler.handleTargetOffline(sender, targetName)
                                .ifPresent(sender::sendMessage);
                            return;
                        }
                        invokeArgs[i] = resolved;
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

                if (result instanceof Component component) {
                    sender.sendMessage(component);
                    return;
                }
                if (result instanceof String string) {
                    sender.sendMessage(Component.text(string));
                }

            } catch (Exception e) {
                // Verifica se é uma exceção relacionada a jogador inválido
                if (isPlayerNotFoundException(e)) {
                    // Tenta extrair o nome do jogador da mensagem de erro ou dos argumentos
                    String playerName = extractPlayerNameFromException(e, args);
                    if (playerName != null && !playerName.isEmpty()) {
                        errorHandler.handleTargetOffline(sender, playerName)
                            .ifPresent(sender::sendMessage);
                        return;
                    }
                }
                
                LOGGER.severe("[CommandFramework] Erro ao executar comando: " + e.getMessage());
                e.printStackTrace();
                errorHandler.handleInternalError(sender, e).ifPresent(sender::sendMessage);
            }
        }

        private boolean isFirstPlayerParameter(java.lang.reflect.Parameter[] parameters, int index) {
            if (index == 0) {
                return true;
            }
            if (index != 1) {
                return false;
            }
            return parameters[0].getType().equals(CommandSender.class);
        }

        private Player resolvePlayerParameter(CommandSender sender, java.lang.reflect.Parameter[] parameters, String[] args, int index, int argIndex) {
            if (!isFirstPlayerParameter(parameters, index)) {
                if (argIndex >= args.length) {
                    return null;
                }
                return Bukkit.getPlayer(args[argIndex]);
            }
            
            if (sender instanceof Player p) {
                return p;
            }
            return null;
        }

        /**
         * Resolve um OfflinePlayer de forma segura, sem fazer chamadas HTTP desnecessárias.
         * Primeiro verifica se o jogador está online, depois verifica no cache do servidor.
         * Só retorna null se o jogador nunca jogou no servidor.
         */
        private OfflinePlayer resolveOfflinePlayer(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }

            // Primeiro tenta encontrar um jogador online (mais rápido)
            Player onlinePlayer = Bukkit.getPlayer(name);
            if (onlinePlayer != null) {
                return onlinePlayer;
            }

            // Verifica se o nome é válido (evita chamadas HTTP para nomes inválidos)
            // Nomes de jogadores do Minecraft: 3-16 caracteres, alfanumérico + underscore
            if (!isValidMinecraftUsername(name)) {
                return null;
            }

            // Usa getOfflinePlayerIfCached para evitar chamadas HTTP
            // Este método retorna null se o jogador nunca esteve no servidor
            OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
            if (cached != null) {
                return cached;
            }

            // Se não está no cache, o jogador nunca jogou neste servidor
            // Não chamamos getOfflinePlayer() para evitar chamadas HTTP
            return null;
        }

        /**
         * Valida se um nome é um username válido do Minecraft.
         * Usernames devem ter 3-16 caracteres e conter apenas letras, números e underscores.
         */
        private boolean isValidMinecraftUsername(String name) {
            if (name == null || name.length() < 3 || name.length() > 16) {
                return false;
            }
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    return false;
                }
            }
            return true;
        }

        private Object parseInteger(String[] args, int argIndex, boolean isPrimitive) {
            var defaultValue = isPrimitive ? 0 : null;
            if (argIndex >= args.length) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private Object parseDouble(String[] args, int argIndex, boolean isPrimitive) {
            var defaultValue = isPrimitive ? 0.0 : null;
            if (argIndex >= args.length) {
                return defaultValue;
            }
            try {
                return Double.parseDouble(args[argIndex]);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private Object parseBoolean(String[] args, int argIndex, boolean isPrimitive) {
            if (argIndex >= args.length) {
                return isPrimitive ? false : null;
            }
            return Boolean.parseBoolean(args[argIndex]);
        }

        private boolean isPlayerNotFoundException(Throwable e) {
            if (e == null) {
                return false;
            }
            
            String exceptionName = e.getClass().getName();
            if (exceptionName.contains("MinecraftClientHttpException") ||
                exceptionName.contains("authlib") ||
                exceptionName.contains("GameProfile")) {
                return true;
            }
            
            String message = e.getMessage();
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                if (lowerMessage.contains("couldn't find") && 
                    (lowerMessage.contains("profile") || lowerMessage.contains("player"))) {
                    return true;
                }
            }
            
            Throwable cause = e.getCause();
            if (cause == null || cause == e) {
                return false;
            }
            return isPlayerNotFoundException(cause);
        }

        private String extractPlayerNameFromException(Throwable e, String[] args) {
            if (e == null || args == null || args.length == 0) {
                return null;
            }
            
            String message = e.getMessage();
            if (message == null) {
                return args.length > 0 && !args[0].isEmpty() ? args[0] : null;
            }
            
            int nameIndex = message.indexOf("name: ");
            if (nameIndex < 0) {
                return args.length > 0 && !args[0].isEmpty() ? args[0] : null;
            }
            
            String namePart = message.substring(nameIndex + 6).trim();
            int endIndex = namePart.indexOf('\n');
            if (endIndex > 0) {
                namePart = namePart.substring(0, endIndex);
            }
            endIndex = namePart.indexOf('\r');
            if (endIndex > 0) {
                namePart = namePart.substring(0, endIndex);
            }
            if (!namePart.isEmpty()) {
                return namePart.trim();
            }
            
            return args.length > 0 && !args[0].isEmpty() ? args[0] : null;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            var completions = new ArrayList<String>();

            if (sender == null || args == null) {
                return completions;
            }

            if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
                for (var parts : subCommandPartsCache.values()) {
                    if (parts.length > 0) {
                        completions.add(parts[0]);
                    }
                }
                if (defaultMethod != null) {
                    var methodCompletions = getTabCompletionsForMethod(defaultMethod, 0, "");
                    completions.addAll(methodCompletions);
                }
                return completions;
            }

            if (args.length == 1) {
                var input = args[0].toLowerCase();
                
                for (var parts : subCommandPartsCache.values()) {
                    if (parts.length > 0 && parts[0].startsWith(input)) {
                        completions.add(parts[0]);
                    }
                }
                
                if (defaultMethod != null) {
                    var methodCompletions = getTabCompletionsForMethod(defaultMethod, 0, input);
                    completions.addAll(methodCompletions);
                }
                
                return completions;
            }

            Method targetMethod = null;
            int paramIndex = 0;
            String input = args[args.length - 1].toLowerCase();

            var subCommandPath = findSubCommandPath(args);
            if (subCommandPath != null) {
                targetMethod = subCommands.get(subCommandPath.path.toLowerCase());
                if (targetMethod != null) {
                    paramIndex = subCommandPath.remainingArgs.length - 1;
                }
            }

            if (targetMethod == null && defaultMethod != null) {
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

            var parameters = method.getParameters();
            boolean hasStringArrayParam = parameters.length > 0 && 
                parameters[parameters.length - 1].getType().equals(String[].class);
            
            int commandArgCount = 0;
            for (var param : parameters) {
                var type = param.getType();
                if (!type.equals(CommandSender.class) && 
                    !type.equals(Player.class) && 
                    !type.equals(String[].class)) {
                    commandArgCount++;
                }
            }
            
            if (hasStringArrayParam) {
                return getStringArrayCompletions(method, paramIndex, input, completions, commandArgCount);
            }

            var methodTabCompletion = method.getAnnotation(TabCompletion.class);
            if (methodTabCompletion != null) {
                var result = tryMethodTabCompletion(methodTabCompletion, paramIndex, method, input, completions);
                if (!result.isEmpty()) {
                    return result;
                }
            }

            if (paramIndex < 0 || paramIndex >= parameters.length) {
                return completions;
            }

            var parameter = parameters[paramIndex];
            return getParameterCompletions(parameter, methodTabCompletion, input, completions);
        }
        
        private List<String> getStringArrayCompletions(Method method, int paramIndex, String input, ArrayList<String> completions, int commandArgCount) {
            int argIndex = paramIndex;
            
            var methodTabCompletion = method.getAnnotation(TabCompletion.class);
            if (methodTabCompletion != null) {
                String[] staticSuggestions = methodTabCompletion.value();
                if (staticSuggestions.length > 0 && argIndex == 0) {
                    addSuggestionsWithPlayersSupport(staticSuggestions, input, completions);
                    return completions;
                }
                
                if (staticSuggestions.length > 0 && argIndex == 1) {
                    if (hasPlayersSuggestion(staticSuggestions)) {
                        addPlayerCompletions(input, completions);
                        return completions;
                    }
                }
                
                var providerClass = methodTabCompletion.provider();
                if (providerClass != null && providerClass != TabCompletion.NoProvider.class) {
                    var providerCompletions = getProviderCompletions(providerClass, input);
                    if (!providerCompletions.isEmpty()) {
                        return providerCompletions;
                    }
                }
            }
            
            if (argIndex == 1 && completions.isEmpty()) {
                addPlayerCompletions(input, completions);
                return completions;
            }
            
            return completions;
        }
        
        private List<String> tryMethodTabCompletion(TabCompletion methodTabCompletion, int paramIndex, Method method, String input, ArrayList<String> completions) {
            String[] staticSuggestions = methodTabCompletion.value();
            if (staticSuggestions.length > 0 && (paramIndex == 0 || method.getParameterCount() == 0)) {
                addSuggestionsWithPlayersSupport(staticSuggestions, input, completions);
                return completions;
            }
            
            var providerClass = methodTabCompletion.provider();
            if (providerClass != null && providerClass != TabCompletion.NoProvider.class) {
                var providerCompletions = getProviderCompletions(providerClass, input);
                if (!providerCompletions.isEmpty()) {
                    return providerCompletions;
                }
            }
            return new ArrayList<>();
        }
        
        private List<String> getParameterCompletions(java.lang.reflect.Parameter parameter, TabCompletion methodTabCompletion, String input, ArrayList<String> completions) {
            if (parameter.getType().equals(String[].class) && methodTabCompletion != null) {
                String[] staticSuggestions = methodTabCompletion.value();
                if (staticSuggestions.length > 0) {
                    addSuggestionsWithPlayersSupport(staticSuggestions, input, completions);
                    return completions;
                }
            }

            var tabCompletion = parameter.getAnnotation(TabCompletion.class);
            if (tabCompletion != null) {
                String[] staticSuggestions = tabCompletion.value();
                if (staticSuggestions.length > 0) {
                    addSuggestionsWithPlayersSupport(staticSuggestions, input, completions);
                    return completions;
                }

                var providerClass = tabCompletion.provider();
                if (providerClass != null && providerClass != TabCompletion.NoProvider.class) {
                    var providerCompletions = getProviderCompletions(providerClass, input);
                    if (!providerCompletions.isEmpty()) {
                        return providerCompletions;
                    }
                }
            }

            var type = parameter.getType();
            if (type.equals(Player.class)) {
                addPlayerCompletions(input, completions);
                return completions;
            }

            if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                if ("true".startsWith(input)) completions.add("true");
                if ("false".startsWith(input)) completions.add("false");
            }

            return completions;
        }
        
        private void addSuggestionsWithPlayersSupport(String[] suggestions, String input, ArrayList<String> completions) {
            for (String suggestion : suggestions) {
                if (suggestion.equals("@players")) {
                    addPlayerCompletions(input, completions);
                    continue;
                }
                if (input.isEmpty() || suggestion.toLowerCase().startsWith(input)) {
                    completions.add(suggestion);
                }
            }
        }
        
        private boolean hasPlayersSuggestion(String[] suggestions) {
            for (String suggestion : suggestions) {
                if (suggestion.equals("@players")) {
                    return true;
                }
            }
            return false;
        }
        
        private void addPlayerCompletions(String input, ArrayList<String> completions) {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            int count = 0;
            for (Player player : players) {
                if (count >= 50) break;
                var name = player.getName();
                if (input.isEmpty() || name.toLowerCase().startsWith(input)) {
                    completions.add(name);
                    count++;
                }
            }
        }

        private List<String> getProviderCompletions(Class<?> providerClass, String input) {
            var completions = new ArrayList<String>();
            try {
                // Usa cache para evitar criação de nova instância a cada tab completion
                var provider = getOrCreateProvider(providerClass);
                if (provider == null) {
                    return completions;
                }
                
                // Verifica se é um SuggestionProvider do Brigadier
                if (provider instanceof SuggestionProvider<?> suggestionProvider) {
                    return getBrigadierProviderCompletions(suggestionProvider, input);
                }
                
                // Fallback para providers com método customizado
                var methods = providerClass.getDeclaredMethods();
                for (var method : methods) {
                    var methodName = method.getName();
                    if (!methodName.equals("getSuggestions") && !methodName.equals("complete")) {
                        continue;
                    }
                    
                    var paramCount = method.getParameterCount();
                    method.setAccessible(true);
                    
                    // Tenta invocar com diferentes assinaturas
                    Object result = null;
                    if (paramCount == 0) {
                        result = method.invoke(provider);
                    } else if (paramCount == 2) {
                        // Pode ser SuggestionProvider do Brigadier
                        var context = createMockCommandContext();
                        var builder = new SuggestionsBuilder(input, input.length());
                        result = method.invoke(provider, context, builder);
                    }
                    
                    if (result instanceof List<?> list) {
                        int count = 0;
                        for (var item : list) {
                            if (count >= 50) break;
                            var str = item.toString();
                            if (str.toLowerCase().startsWith(input)) {
                                completions.add(str);
                                count++;
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
        
        /**
         * Obtém completions de um SuggestionProvider do Brigadier.
         */
        private List<String> getBrigadierProviderCompletions(SuggestionProvider<?> provider, String input) {
            var completions = new ArrayList<String>();
            try {
                // Cria um CommandContext mínimo usando reflexão
                var context = createMockCommandContext();
                if (context == null) {
                    LOGGER.warning("[CommandFramework] Não foi possível criar CommandContext para provider");
                    return completions;
                }
                
                var builder = new SuggestionsBuilder(input, input.length());
                
                // Invoca o provider usando reflexão para garantir tipos corretos
                var method = provider.getClass().getMethod("getSuggestions", 
                    CommandContext.class, SuggestionsBuilder.class);
                method.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                var future = (java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>) 
                    method.invoke(provider, context, builder);
                
                // Aguarda o resultado
                var suggestions = future.get();
                
                // Extrai as sugestões
                int count = 0;
                for (var suggestion : suggestions.getList()) {
                    if (count >= 50) break;
                    var text = suggestion.getText();
                    if (text.toLowerCase().startsWith(input)) {
                        completions.add(text);
                        count++;
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("[CommandFramework] Erro ao obter sugestões do Brigadier provider: " + e.getMessage());
            }
            return completions;
        }
        
        /**
         * Cria um CommandContext mock para uso com SuggestionProvider.
         * Usa reflexão para criar uma instância mínima.
         */
        @SuppressWarnings("unchecked")
        private CommandContext<CommandSender> createMockCommandContext() {
            try {
                // Tenta criar um CommandContext usando o construtor via reflexão
                var constructors = CommandContext.class.getDeclaredConstructors();
                if (constructors.length == 0) {
                    return null;
                }
                
                var constructor = constructors[0];
                constructor.setAccessible(true);
                
                var paramTypes = constructor.getParameterTypes();
                var params = new Object[paramTypes.length];
                
                // Preenche os parâmetros com valores padrão
                for (int i = 0; i < paramTypes.length; i++) {
                    var type = paramTypes[i];
                    if (type == CommandSender.class || CommandSender.class.isAssignableFrom(type)) {
                        params[i] = Bukkit.getConsoleSender();
                    } else if (type == String.class) {
                        params[i] = "";
                    } else if (type == java.util.Map.class) {
                        params[i] = Collections.emptyMap();
                    } else if (type == java.util.List.class) {
                        params[i] = Collections.emptyList();
                    } else if (type == boolean.class || type == Boolean.class) {
                        params[i] = false;
                    } else {
                        params[i] = null;
                    }
                }
                
                return (CommandContext<CommandSender>) constructor.newInstance(params);
            } catch (Exception e) {
                // Se falhar, retorna null e o provider pode não funcionar perfeitamente
                // mas pelo menos não quebra
                LOGGER.warning("[CommandFramework] Não foi possível criar CommandContext mock: " + e.getMessage());
                return null;
            }
        }
        
        /**
         * Obtém ou cria uma instância do provider usando cache estático.
         * Evita reflexão repetida para criação de instâncias.
         * Suporta injeção de dependências para providers com construtores parametrizados.
         */
        private Object getOrCreateProvider(Class<?> providerClass) {
            return PROVIDER_CACHE.computeIfAbsent(providerClass, clazz -> {
                try {
                    // Primeiro tenta construtor sem argumentos (compatibilidade retroativa)
                    try {
                        var noArgConstructor = clazz.getDeclaredConstructor();
                        noArgConstructor.setAccessible(true);
                        return noArgConstructor.newInstance();
                    } catch (NoSuchMethodException e) {
                        // Se não tem construtor sem argumentos, tenta encontrar um construtor
                        var constructors = clazz.getDeclaredConstructors();
                        if (constructors.length == 0) {
                            LOGGER.warning("[CommandFramework] Provider " + clazz.getName() + " não possui construtores");
                            return null;
                        }
                        
                        // Usa o primeiro construtor encontrado
                        var constructor = constructors[0];
                        constructor.setAccessible(true);
                        
                        var parameters = constructor.getParameterCount();
                        if (parameters == 0) {
                            return constructor.newInstance();
                        }
                        
                        // Resolve dependências usando o DependencyResolver
                        var args = new Object[parameters];
                        for (int i = 0; i < parameters; i++) {
                            var paramType = constructor.getParameterTypes()[i];
                            args[i] = dependencyResolver.resolve(paramType);
                        }
                        
                        return constructor.newInstance(args);
                    }
                } catch (Exception e) {
                    LOGGER.warning("[CommandFramework] Erro ao criar provider: " + clazz.getName() + " - " + e.getMessage());
                    return null;
                }
            });
        }

        private record SubCommandMatch(String path, String[] remainingArgs) {}
    }
}

