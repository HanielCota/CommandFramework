package com.github.hanielcota.commandframework.processor;

import com.github.hanielcota.commandframework.adapter.BukkitCommandAdapter;
import com.github.hanielcota.commandframework.annotation.Command;
import com.github.hanielcota.commandframework.annotation.DefaultCommand;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.brigadier.CommandMetadata;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.dependency.DefaultDependencyResolver;
import com.github.hanielcota.commandframework.dependency.DependencyResolver;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import com.github.hanielcota.commandframework.registry.CommandDefinition;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CommandProcessor {

    private static final Logger LOGGER = Logger.getLogger(CommandProcessor.class.getSimpleName());

    @SuppressWarnings("unused") // Usado através do dependencyResolver
    Plugin plugin;
    @SuppressWarnings("unused") // Usado através do dependencyResolver
    ArgumentParserRegistry parserRegistry;
    @SuppressWarnings("unused") // Usado através do dependencyResolver
    CommandExecutor executor;
    @SuppressWarnings("unused") // Usado através do dependencyResolver
    CooldownService cooldownService;
    @SuppressWarnings("unused") // Usado através do dependencyResolver
    GlobalErrorHandler errorHandler;
    BukkitCommandAdapter adapter;
    DependencyResolver dependencyResolver;
    
    /**
     * Set de comandos registrados manualmente (com prioridade sobre scan automático).
     * Armazena o nome do comando em lowercase.
     */
    Set<String> manuallyRegisteredCommands = ConcurrentHashMap.newKeySet();
    
    /**
     * Set de todos os comandos registrados (para evitar duplicatas).
     * Armazena o nome do comando em lowercase.
     */
    Set<String> registeredCommands = ConcurrentHashMap.newKeySet();

    public CommandProcessor(Plugin plugin, ArgumentParserRegistry parserRegistry, CommandExecutor executor,
                           CooldownService cooldownService, GlobalErrorHandler errorHandler) {
        this.plugin = plugin;
        this.parserRegistry = parserRegistry;
        this.executor = executor;
        this.cooldownService = cooldownService;
        this.errorHandler = errorHandler;
        this.dependencyResolver = new DefaultDependencyResolver(plugin, parserRegistry, executor, cooldownService, errorHandler);
        this.adapter = new BukkitCommandAdapter(plugin, cooldownService, errorHandler, dependencyResolver);
    }

    public void processAndRegister(List<CommandDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }

        for (var definition : definitions) {
            var commandName = definition.getAnnotation().name().toLowerCase();
            
            // Verifica se o comando já foi registrado manualmente (tem prioridade)
            if (manuallyRegisteredCommands.contains(commandName)) {
                continue;
            }
            
            // Verifica se o comando já foi registrado pelo scan
            if (registeredCommands.contains(commandName)) {
                continue;
            }
            
            var metadata = toMetadata(definition);
            if (metadata == null) {
                LOGGER.warning("[CommandFramework] Falha ao criar metadata para: " + definition.getType().getName());
                continue;
            }

            adapter.register(metadata);
            registeredCommands.add(commandName);
        }
    }

    private CommandMetadata toMetadata(CommandDefinition definition) {
        if (definition == null) {
            return null;
        }
        var instance = createInstance(definition.getType());
        if (instance == null) {
            return null;
        }
        return CommandMetadata.builder()
            .commandAnnotation(definition.getAnnotation())
            .type(definition.getType())
            .instance(instance)
            .handlers(definition.getHandlers())
            .build();
    }

    private Object createInstance(Class<?> type) {
        if (type == null) {
            return null;
        }

        try {
            // Primeiro tenta encontrar um construtor sem parâmetros
            try {
                var noArgConstructor = type.getDeclaredConstructor();
                noArgConstructor.setAccessible(true);
                return noArgConstructor.newInstance();
            } catch (NoSuchMethodException e) {
                // Se não tem construtor sem argumentos, procura construtores com parâmetros
                var constructors = type.getDeclaredConstructors();
                if (constructors.length == 0) {
                    LOGGER.warning("[CommandFramework] Classe " + type.getName() + " não possui construtores");
                    return null;
                }

                // Usa o primeiro construtor encontrado
                var constructor = constructors[0];
                constructor.setAccessible(true);

                var parameters = constructor.getParameterCount();
                if (parameters == 0) {
                    return constructor.newInstance();
                }

                var args = new Object[parameters];
                for (int i = 0; i < parameters; i++) {
                    var paramType = constructor.getParameterTypes()[i];
                    args[i] = resolveDependency(paramType);
                }

                return constructor.newInstance(args);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CommandFramework] Erro ao criar instância de " + type.getName() + ": " + e.getMessage(), e);
            return null;
        }
    }

    private Object resolveDependency(Class<?> type) {
        return dependencyResolver.resolve(type);
    }
    
    /**
     * Obtém o resolvedor de dependências para permitir registro de dependências customizadas.
     * 
     * @return O resolvedor de dependências
     */
    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Registra um comando diretamente a partir de uma instância.
     * Comandos registrados manualmente têm prioridade sobre o scan automático.
     * Se o comando já estiver registrado, será substituído pela nova instância.
     * 
     * @param instance Instância do comando (deve ter a anotação @Command)
     */
    public void register(Object instance) {
        if (instance == null) {
            LOGGER.warning("[CommandFramework] Tentativa de registrar comando nulo");
            return;
        }

        var definition = toDefinition(instance);
        if (definition == null) {
            LOGGER.warning("[CommandFramework] Falha ao criar definição para: " + instance.getClass().getName());
            return;
        }

        var commandName = definition.getAnnotation().name().toLowerCase();
        
        // Marca como registrado manualmente (tem prioridade sobre scan automático)
        manuallyRegisteredCommands.add(commandName);
        
        var metadata = toMetadata(definition, instance);
        if (metadata == null) {
            LOGGER.warning("[CommandFramework] Falha ao criar metadata para: " + instance.getClass().getName());
            return;
        }
        adapter.register(metadata);
        registeredCommands.add(commandName);
    }

    /**
     * Registra um comando a partir de uma classe (cria a instância automaticamente).
     * 
     * @param commandClass Classe do comando (deve ter a anotação @Command)
     */
    public void register(Class<?> commandClass) {
        if (commandClass == null) {
            LOGGER.warning("[CommandFramework] Tentativa de registrar classe nula");
            return;
        }

        var instance = createInstance(commandClass);
        if (instance == null) {
            LOGGER.warning("[CommandFramework] Falha ao criar instância de: " + commandClass.getName());
            return;
        }

        register(instance);
    }

    private CommandDefinition toDefinition(Object instance) {
        if (instance == null) {
            return null;
        }
        var type = instance.getClass();
        var annotation = type.getAnnotation(Command.class);
        if (annotation == null) {
            LOGGER.warning("[CommandFramework] Classe " + type.getName() + " não possui anotação @Command");
            return null;
        }
        var methods = findHandlerMethods(type);
        return CommandDefinition.builder()
            .annotation(annotation)
            .type(type)
            .handlers(methods)
            .build();
    }

    private static List<Method> findHandlerMethods(Class<?> type) {
        if (type == null) {
            return List.of();
        }

        var methods = type.getDeclaredMethods();
        if (methods.length == 0) {
            return List.of();
        }

        return Arrays.stream(methods)
            .filter(method -> method.isAnnotationPresent(SubCommand.class) || method.isAnnotationPresent(DefaultCommand.class))
            .peek(method -> method.setAccessible(true))
            .collect(Collectors.toList());
    }

    private CommandMetadata toMetadata(CommandDefinition definition, Object instance) {
        if (definition == null || instance == null) {
            return null;
        }
        return CommandMetadata.builder()
            .commandAnnotation(definition.getAnnotation())
            .type(definition.getType())
            .instance(instance)
            .handlers(definition.getHandlers())
            .build();
    }
    
    /**
     * Verifica se um comando já está registrado.
     * 
     * @param commandName Nome do comando (case-insensitive)
     * @return true se o comando já está registrado
     */
    public boolean isRegistered(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return false;
        }
        return registeredCommands.contains(commandName.toLowerCase());
    }
    
    /**
     * Verifica se um comando foi registrado manualmente (com dependências).
     * 
     * @param commandName Nome do comando (case-insensitive)
     * @return true se o comando foi registrado manualmente
     */
    public boolean isManuallyRegistered(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return false;
        }
        return manuallyRegisteredCommands.contains(commandName.toLowerCase());
    }
}
